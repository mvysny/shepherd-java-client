package com.github.mvysny.shepherd.api

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

    override final fun getAllProjectIDs(): List<ProjectId> = projectConfigFolder.getAllProjects()

    override final fun getAllProjects(ownerEmail: String?): List<ProjectView> {
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

    override final fun getProjectInfo(id: ProjectId): Project = projectConfigFolder.getProjectInfo(id)
    override final fun existsProject(id: ProjectId): Boolean = projectConfigFolder.existsProject(id)

    override final fun getConfig(): Config = fs.configFolder.loadConfig()

    internal val jenkins: SimpleJenkinsClient = getConfig().let { config ->
        SimpleJenkinsClient(
            jenkinsUrl = config.jenkins.url,
            jenkinsUsername = config.jenkins.username,
            jenkinsPassword = config.jenkins.password,
            shepherdHome = config.shepherdHome
        )
    }

    override final fun getLastBuilds(id: ProjectId): List<Build> {
        projectConfigFolder.requireProjectExists(id)
        val lastBuilds = try {
            jenkins.getLastBuilds(id)
        } catch (e: FileNotFoundException) {
            throw NoSuchProjectException(id, e)
        }
        return lastBuilds.map { it.toBuild() }
    }

    override final fun createProject(project: Project) {
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
     * Creates the project in the underlying runtime env
     */
    protected abstract fun doCreateProject(project: Project)

    override final fun deleteProject(id: ProjectId) {
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

    override final fun getBuildLog(id: ProjectId, buildNumber: Int): String = jenkins.getBuildConsoleText(id, buildNumber)
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
