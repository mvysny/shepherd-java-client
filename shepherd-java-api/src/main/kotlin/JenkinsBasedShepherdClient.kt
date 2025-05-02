package com.github.mvysny.shepherd.api

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
        return containerSystem.getRunLogs(id)
    }

    override fun getRunMetrics(id: ProjectId): ResourcesUsage {
        checkProjectExists(id)
        return containerSystem.getRunMetrics(id)
    }

    override fun getBuildLog(id: ProjectId, buildNumber: Int): String = jenkins.getBuildConsoleText(id, buildNumber)

    override fun updateProject(project: Project) {
        val oldProject = fs.configFolder.projects.getProjectInfo(project.id)
        require(oldProject.gitRepo.url == project.gitRepo.url) { "gitRepo is not allowed to be changed: new ${project.gitRepo.url} old ${project.gitRepo.url}" }

        checkMemoryUsage(project)

        // 1. Overwrite the project JSON file
        fs.configFolder.projects.writeProjectJson(project)

        // 2. Overwrite Kubernetes config file at /etc/shepherd/k8s/PROJECT_ID.yaml
        val needsRestart = containerSystem.updateProjectConfig(project)

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
            containerSystem.restartProject(project.id)
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
 * Runtime container system which is capable of running project docker containers.
 * There are two implementations: Kubernetes and pure Docker.
 */
public interface RuntimeContainerSystem {
    /**
     * Creates the project in the underlying runtime env: prepares all necessary files
     * and resources. Note that the project will be compiled by Jenkins first, and started
     * later on.
     */
    public fun createProject(project: Project)

    /**
     * Kills all project's running containers and deletes all files & resources
     * used by the project. Does nothing if the project containers aren't running and
     * there are no resources to clean up.
     *
     * The project exists, but it may not be running (e.g. if no Jenkins build passed yet).
     */
    public fun deleteProject(id: ProjectId)

    /**
     * Updates runtime system files and objects to the new project configuration.
     *
     * The project exists, but it may not be running (e.g. if no Jenkins build passed yet).
     * @return true if the project needs to be restarted (via [restartProject]).
     */
    public fun updateProjectConfig(project: Project): Boolean

    /**
     * Checks whether the main project container is running or not.
     *
     * The project exists, but it may not be running (e.g. if no Jenkins build passed yet).
     * @return true if the main project container is running, false if not.
     */
    public fun isProjectRunning(id: ProjectId): Boolean

    /**
     * Restarts project [id]. Only called if the project runtime container is running at the moment.
     */
    public fun restartProject(id: ProjectId)

    /**
     * Retrieves the run logs of the main app pod (=the app itself). There may be additional pods (e.g. PostgreSQL)
     * but their logs are not returned.
     *
     * The project exists, but it may not be running (e.g. if no Jenkins build passed yet).
     * If it's not running, returns an empty string.
     */
    public fun getRunLogs(id: ProjectId): String

    /**
     * Returns the current CPU/memory usage of the main app pod.
     *
     * The project exists, but it may not be running (e.g. if no Jenkins build passed yet).
     * If it's not running, return [ResourcesUsage.zero].
     */
    public fun getRunMetrics(id: ProjectId): ResourcesUsage
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
