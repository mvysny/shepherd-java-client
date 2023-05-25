package com.github.mvysny.shepherd.api

import com.offbytwo.jenkins.JenkinsServer
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Interacts with the actual shepherd system.
 * @property etcShepherdPath the root shepherd path, `/etc/shepherd`
 */
public class LinuxShepherdClient(
    private val kubernetesClient: SimpleKubernetesClient = SimpleKubernetesClient(),
    private val etcShepherdPath: Path = Path("/etc/shepherd"),
    private val jenkins: SimpleJenkinsClient = SimpleJenkinsClient()
) : ShepherdClient {
    /**
     * Project config JSONs are stored here. Defaults to `/etc/shepherd/projects`.
     */
    private val projectConfigFolder = ProjectConfigFolder(etcShepherdPath / "projects")

    override fun getAllProjects(): List<ProjectId> = projectConfigFolder.getAllProjects()

    override fun getProjectInfo(id: ProjectId): Project = projectConfigFolder.getProjectInfo(id)

    override fun createProject(project: Project) {
        // 1. Create project JSON file
        projectConfigFolder.requireProjectDoesntExist(project.id)
        projectConfigFolder.writeProjectJson(project)

        // TODO
        // 2. Create Kubernetes config file at /etc/shepherd/k8s/PROJECT_ID.yaml

        // 3. Create Jenkins job
        jenkins.createJob(project)

        // 4. Run Jenkins job
        jenkins.build(project.id)
    }

    private val ProjectId.kubernetesNamespace: String get() = "shepherd-${id}"

    override fun getRunLogs(id: ProjectId): String {
        // main deployment is always called "deployment"
        val podNames = kubernetesClient.getPods(id.kubernetesNamespace)
        val podName = podNames.firstOrNull { it.startsWith("deployment-") }
        requireNotNull(podName) { "No deployment pod for ${id.id}: $podNames" }
        return kubernetesClient.getLogs(podName, id.kubernetesNamespace)
    }
}
