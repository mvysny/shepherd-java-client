package com.github.mvysny.shepherd.api

import com.github.mvysny.shepherd.api.containers.RuntimeContainerSystem
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException

/**
 * An implementation of [ShepherdClient] which uses Jenkins to build projects,
 * and uses [containerSystem] to control running project containers.
 */
public class JenkinsBasedShepherdClient(
    private val fs: LocalFS,
    private val containerSystem: RuntimeContainerSystem
) : ShepherdClient {
    private val projectConfigFolder get() = fs.configFolder.projects

    private fun checkProjectExists(id: ProjectId) {
        projectConfigFolder.requireProjectExists(id)
    }

    override fun getAllProjectIDs(): List<ProjectId> = projectConfigFolder.getAllProjects()

    override fun getAllProjects(ownerEmail: String?): List<ProjectView> {
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

    override fun getProjectInfo(id: ProjectId): Project = projectConfigFolder.getProjectInfo(id)
    override fun existsProject(id: ProjectId): Boolean = projectConfigFolder.existsProject(id)

    override fun getConfig(): Config = fs.configFolder.loadConfig()
    override fun restartContainers(id: ProjectId) {
        containerSystem.restartProject(projectConfigFolder.getProjectInfo(id))
    }

    override fun getMainDomainDeployURL(id: ProjectId): String = containerSystem.getMainDomainDeployURL(id)

    private val jenkins: SimpleJenkinsClient = getConfig().let { config ->
        SimpleJenkinsClient(
            jenkinsUrl = config.jenkins.url,
            jenkinsUsername = config.jenkins.username,
            jenkinsPassword = config.jenkins.password,
            shepherdHome = config.shepherdHome
        )
    }

    override fun getLastBuilds(id: ProjectId): List<Build> {
        projectConfigFolder.requireProjectExists(id)
        val lastBuilds = try {
            jenkins.getLastBuilds(id)
        } catch (e: FileNotFoundException) {
            throw NoSuchProjectException(id, e)
        }
        return lastBuilds.map { it.toBuild() }
    }

    override fun createProject(project: Project) {
        // check prerequisites
        require(!project.id.isReserved) { "${project.id} is reserved" }
        projectConfigFolder.requireProjectDoesntExist(project.id)
        checkMemoryUsage(project)

        // 1. Create project JSON file
        projectConfigFolder.writeProjectJson(project)

        // 2. Create config files for the underlying runtime system (Kubernetes, Docker)
        containerSystem.createProject(project)

        // 3. Create Jenkins job
        jenkins.createJob(project)

        // 4. Run the build immediately
        jenkins.build(project.id)
    }

    override fun deleteProject(id: ProjectId) {
        jenkins.deleteJobIfExists(id)
        fs.cacheFolder.deleteCacheIfExists(id)
        containerSystem.deleteProject(id)
        projectConfigFolder.deleteIfExists(id)
    }

    override fun getRunLogs(id: ProjectId): String {
        checkProjectExists(id)
        if (containerSystem.isProjectRunning(id)) {
            return containerSystem.getRunLogs(id)
        } else {
            return ""
        }
    }

    override fun getRunMetrics(id: ProjectId): ResourcesUsage {
        checkProjectExists(id)
        if (containerSystem.isProjectRunning(id)) {
            return containerSystem.getRunMetrics(id)
        } else {
            return ResourcesUsage.zero
        }
    }

    override fun getBuildLog(id: ProjectId, buildNumber: Int): String = jenkins.getBuildConsoleText(id, buildNumber)

    override fun updateProject(project: Project) {
        val oldProject = fs.configFolder.projects.getProjectInfo(project.id)
        require(oldProject.gitRepo.url == project.gitRepo.url) { "gitRepo is not allowed to be changed: new ${project.gitRepo.url} old ${project.gitRepo.url}" }

        checkMemoryUsage(project)

        // 1. Overwrite the project JSON file
        fs.configFolder.projects.writeProjectJson(project)

        // 2. Overwrite Kubernetes config file at /etc/shepherd/k8s/PROJECT_ID.yaml
        val needsRestart = containerSystem.updateProjectConfig(project, oldProject)

        // 3. Update Jenkins job
        jenkins.updateJob(project)

        // 4. Detect what kind of update is needed.
        val isMainPodRunning = containerSystem.isProjectRunning(project.id)
        if (!isMainPodRunning) {
            log.info("${project.id.id}: isn't running yet, there is probably no Jenkins job which completed successfully yet")
            jenkins.build(project.id)
        } else if (SimpleJenkinsClient.needsProjectRebuild(project, oldProject)) {
            log.info("${project.id.id}: needs full rebuild on Jenkins")
            jenkins.build(project.id)
        } else if (needsRestart) {
            log.info("${project.id.id}: performing quick kubernetes apply")
            containerSystem.restartProject(project)
        } else {
            // probably just a project description or project owner changed, do nothing
            log.info("${project.id.id}: no kubernetes-level/jenkins-level changes detected, not reloading the project")
        }
    }

    override fun close() {
    }

    private companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(JenkinsBasedShepherdClient::class.java)
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
