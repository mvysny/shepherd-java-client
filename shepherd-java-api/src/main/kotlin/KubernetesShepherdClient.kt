package com.github.mvysny.shepherd.api

import org.slf4j.LoggerFactory

/**
 * Interacts with the actual shepherd system.
 * @property projectConfigFolder Project config JSONs are stored here.
 */
public class KubernetesShepherdClient @JvmOverloads constructor(
    private val kubernetes: SimpleKubernetesClient = SimpleKubernetesClient(),
    private val projectConfigFolder: ProjectConfigFolder = ProjectConfigFolder.defaultLinux(),
) : ShepherdClient {
    private val jenkins: SimpleJenkinsClient = SimpleJenkinsClient(jenkinsUrl = getConfig().jenkins.url, jenkinsUsername = getConfig().jenkins.username, jenkinsPassword = getConfig().jenkins.password)

    override fun getAllProjectIDs(): List<ProjectId> = projectConfigFolder.getAllProjects()

    override fun getAllProjects(ownerEmail: String?): List<ProjectView> {
        var projects = getAllProjectIDs()
            .map { getProjectInfo(it) }
        if (ownerEmail != null) {
            projects = projects.filter { it.owner.email == ownerEmail }
        }
        val jobs: Map<ProjectId, JenkinsJob> = jenkins.getJobsOverview().associateBy { ProjectId(it.name) }
        return projects.map { project ->
            val job = jobs[project.id]
            val lastBuild = job?.lastBuild?.toBuild()
            ProjectView(project, lastBuild)
        }
    }

    override fun getProjectInfo(id: ProjectId): Project = projectConfigFolder.getProjectInfo(id)

    override fun createProject(project: Project) {
        // check prerequisites
        projectConfigFolder.requireProjectDoesntExist(project.id)
        checkMemoryUsage(project)

        // 1. Create project JSON file
        projectConfigFolder.writeProjectJson(project)

        // 2. Create Kubernetes config file at /etc/shepherd/k8s/PROJECT_ID.yaml
        kubernetes.writeConfigYamlFile(project)

        // 3. Create Jenkins job
        jenkins.createJob(project)

        // 4. Run the build immediately
        jenkins.build(project.id)
    }

    override fun updateProject(project: Project) {
        val oldProject = projectConfigFolder.getProjectInfo(project.id)
        require(oldProject.gitRepo == project.gitRepo) { "gitRepo is not allowed to be changed: new ${project.gitRepo} old ${project.gitRepo}" }

        checkMemoryUsage(project)

        // 1. Overwrite the project JSON file
        projectConfigFolder.writeProjectJson(project)

        // 2. Overwrite Kubernetes config file at /etc/shepherd/k8s/PROJECT_ID.yaml
        val kubernetesConfigYamlChanged = kubernetes.writeConfigYamlFile(project)

        // 3. Update Jenkins job
        jenkins.updateJob(project)

        // 4. Detect what kind of update is needed.
        val mainPodDockerImage = kubernetes.getCurrentDockerImage(project.id)
        val isMainPodRunning = mainPodDockerImage != null
        if (!isMainPodRunning) {
            log.info("${project.id.id}: isn't running yet, there is probably no Jenkins job which completed successfully yet")
            jenkins.build(project.id)
        } else if (SimpleJenkinsClient.needsProjectRebuild(project, oldProject)) {
            log.info("${project.id.id}: needs full rebuild on Jenkins")
            jenkins.build(project.id)
        } else if (kubernetesConfigYamlChanged) {
            log.info("${project.id.id}: performing quick kubernetes apply")
            exec("/opt/shepherd/shepherd-apply", project.id.id, mainPodDockerImage!!)
        } else {
            log.info("${project.id.id}: no kubernetes-level/jenkins-level changes detected, not reloading the project")
        }
    }

    override fun deleteProject(id: ProjectId) {
        jenkins.deleteJobIfExists(id)
        kubernetes.deleteIfExists(id)
        projectConfigFolder.deleteIfExists(id)
    }

    override fun getRunLogs(id: ProjectId): String = kubernetes.getRunLogs(id)
    override fun getRunMetrics(id: ProjectId): ResourcesUsage = kubernetes.getMetrics(id)
    override fun getLastBuilds(id: ProjectId): List<Build> {
        val lastBuilds = jenkins.getLastBuilds(id)
        return lastBuilds.map { it.toBuild() }
    }

    override fun getBuildLog(id: ProjectId, buildNumber: Int): String = jenkins.getBuildConsoleText(id, buildNumber)
    override fun getConfig(): Config = Config.load()

    override fun close() {}

    public companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(KubernetesShepherdClient::class.java)
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
