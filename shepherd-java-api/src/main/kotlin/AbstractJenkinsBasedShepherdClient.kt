package com.github.mvysny.shepherd.api

import org.slf4j.LoggerFactory
import java.io.FileNotFoundException

/**
 * A skeletal implementation of [ShepherdClient] which uses Jenkins to build projects.
 * Overriding classes use different runtime environments to run projects: Kubernetes or Docker.
 */
public abstract class AbstractJenkinsBasedShepherdClient(
    protected val fs: LocalFS
) : ShepherdClient {

    private val projectConfigFolder get() = fs.configFolder.projects

    protected fun checkProjectExists(id: ProjectId) {
        projectConfigFolder.requireProjectExists(id)
    }

    final override fun getAllProjectIDs(): List<ProjectId> = projectConfigFolder.getAllProjects()

    final override fun getAllProjects(ownerEmail: String?): List<ProjectView> {
        var projects = getAllProjectIDs()
            .map { getProjectInfo(it) }
        if (ownerEmail != null) {
            projects = projects.filter { it.canEdit(ownerEmail) }
        }
        val jobs: Map<ProjectId, JenkinsJob> = jenkins.getJobsOverview().associateBy { ProjectId(it.name) }
        return projects.map { project ->
            val job = jobs[project.id]
            val lastBuild = job?.lastBuild?.toBuild()
            ProjectView(project, lastBuild)
        }
    }

    final override fun getProjectInfo(id: ProjectId): Project = projectConfigFolder.getProjectInfo(id)
    final override fun existsProject(id: ProjectId): Boolean = projectConfigFolder.existsProject(id)

    final override fun getConfig(): Config = fs.configFolder.loadConfig()

    internal val jenkins: SimpleJenkinsClient = getConfig().let { config ->
        SimpleJenkinsClient(
            jenkinsUrl = config.jenkins.url,
            jenkinsUsername = config.jenkins.username,
            jenkinsPassword = config.jenkins.password,
            shepherdHome = config.shepherdHome
        )
    }

    final override fun getLastBuilds(id: ProjectId): List<Build> {
        projectConfigFolder.requireProjectExists(id)
        val lastBuilds = try {
            jenkins.getLastBuilds(id)
        } catch (e: FileNotFoundException) {
            throw NoSuchProjectException(id, e)
        }
        return lastBuilds.map { it.toBuild() }
    }

    final override fun createProject(project: Project) {
        // check prerequisites
        projectConfigFolder.requireProjectDoesntExist(project.id)
        checkMemoryUsage(project)

        // 1. Create project JSON file
        projectConfigFolder.writeProjectJson(project)

        // 2. Create config files for the underlying runtime system (Kubernetes, Docker)
        doCreateProject(project)

        // 3. Create Jenkins job
        jenkins.createJob(project)

        // 4. Run the build immediately
        jenkins.build(project.id)
    }

    /**
     * Creates the project in the underlying runtime env: prepares all necessary files
     * and resources.
     */
    protected abstract fun doCreateProject(project: Project)

    final override fun deleteProject(id: ProjectId) {
        jenkins.deleteJobIfExists(id)
        fs.cacheFolder.deleteCacheIfExists(id)
        doDeleteProject(id)
        projectConfigFolder.deleteIfExists(id)
    }

    /**
     * Kills all project running containers and deletes all files & resources
     * used by the project. Does nothing if the project doesn't exist.
     */
    protected abstract fun doDeleteProject(id: ProjectId)

    final override fun getBuildLog(id: ProjectId, buildNumber: Int): String = jenkins.getBuildConsoleText(id, buildNumber)

    final override fun updateProject(project: Project) {
        val oldProject = fs.configFolder.projects.getProjectInfo(project.id)
        require(oldProject.gitRepo.url == project.gitRepo.url) { "gitRepo is not allowed to be changed: new ${project.gitRepo.url} old ${project.gitRepo.url}" }

        checkMemoryUsage(project)

        // 1. Overwrite the project JSON file
        fs.configFolder.projects.writeProjectJson(project)

        // 2. Overwrite Kubernetes config file at /etc/shepherd/k8s/PROJECT_ID.yaml
        val needsRestart = doUpdateProjectConfig(project)

        // 3. Update Jenkins job
        jenkins.updateJob(project)

        // 4. Detect what kind of update is needed.
        val isMainPodRunning = isProjectRunning(project.id)
        if (!isMainPodRunning) {
            log.info("${project.id.id}: isn't running yet, there is probably no Jenkins job which completed successfully yet")
            jenkins.build(project.id)
        } else if (SimpleJenkinsClient.needsProjectRebuild(project, oldProject)) {
            log.info("${project.id.id}: needs full rebuild on Jenkins")
            jenkins.build(project.id)
        } else if (needsRestart) {
            log.info("${project.id.id}: performing quick kubernetes apply")
            restartProject(project.id)
        } else {
            // probably just a project description or project owner changed, do nothing
            log.info("${project.id.id}: no kubernetes-level/jenkins-level changes detected, not reloading the project")
        }
    }

    /**
     * Updates runtime system files and objects to the new project configuration.
     * @return true if the project needs to be restarted (via [restartProject]).
     */
    protected abstract fun doUpdateProjectConfig(project: Project): Boolean

    /**
     * @return true if the main project container is running, false if not.
     */
    protected abstract fun isProjectRunning(id: ProjectId): Boolean

    /**
     * Restarts project [id].
     */
    protected abstract fun restartProject(id: ProjectId)

    private companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(AbstractJenkinsBasedShepherdClient::class.java)
    }
}

/**
 * We must take care not to bring the Shepherd VM down by OOMs or excessive swapping.
 *
 * If a project creation/update would create a situation where [ProjectMemoryStats.totalQuota]
 * would overlap memory available to shepherd, then such a change will be rejected with an informative exception.
 */
internal fun ShepherdClient.checkMemoryUsage(updatedOrCreatedProject: Project) {
    val config = getConfig()
    // 1. check the max runtime+build memory+cpu usage
    require(updatedOrCreatedProject.runtime.resources.memoryMb <= config.maxProjectRuntimeResources.memoryMb) {
        "A project can ask for max ${config.maxProjectRuntimeResources.memoryMb} Mb of runtime memory but it asked for ${updatedOrCreatedProject.runtime.resources.memoryMb} Mb"
    }
    require(updatedOrCreatedProject.runtime.resources.cpu <= config.maxProjectRuntimeResources.cpu) {
        "A project can ask for max ${config.maxProjectRuntimeResources.cpu} runtime CPUs but it asked for ${updatedOrCreatedProject.runtime.resources.cpu} CPUs"
    }
    require(updatedOrCreatedProject.build.resources.memoryMb <= config.maxProjectBuildResources.memoryMb) {
        "A project can ask for max ${config.maxProjectBuildResources.memoryMb} Mb of build memory but it asked for ${updatedOrCreatedProject.build.resources.memoryMb} Mb"
    }
    require(updatedOrCreatedProject.build.resources.cpu <= config.maxProjectBuildResources.cpu) {
        "A project can ask for max ${config.maxProjectBuildResources.cpu} runtime CPUs but it asked for ${updatedOrCreatedProject.build.resources.cpu} CPUs"
    }

    // 2. Check memory quota
    val projectMap: MutableMap<ProjectId, Project> = getAllProjectIDs().associateWith { getProjectInfo(it) } .toMutableMap()
    projectMap[updatedOrCreatedProject.id] = updatedOrCreatedProject
    val stats = ProjectMemoryStats.calculateQuota(config, projectMap.values.toList())
    require(stats.totalQuota.usageMb <= stats.totalQuota.limitMb) {
        "Can not add project ${updatedOrCreatedProject.id.id}: there is no available memory to run it+build it"
    }
}
