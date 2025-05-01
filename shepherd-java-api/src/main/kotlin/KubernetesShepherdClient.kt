package com.github.mvysny.shepherd.api

/**
 * Interacts with the actual [Shepherd](https://github.com/mvysny/shepherd) system running via Kubernetes.
 */
public class KubernetesShepherdClient @JvmOverloads constructor(
    fs: LocalFS,
    private val kubernetes: SimpleKubernetesClient = SimpleKubernetesClient(defaultDNS = fs.configFolder.loadConfig().hostDNS, yamlConfigFolder = fs.configFolder.kubernetesYamlFiles),
) : AbstractJenkinsBasedShepherdClient(fs) {

    override fun doCreateProject(project: Project) {
        // Create Kubernetes config file at /etc/shepherd/k8s/PROJECT_ID.yaml
        kubernetes.writeConfigYamlFile(project)
    }

    override fun doUpdateProjectConfig(project: Project): Boolean =
        kubernetes.writeConfigYamlFile(project)

    override fun isProjectRunning(id: ProjectId): Boolean {
        val mainPodDockerImage = kubernetes.getCurrentDockerImage(id)
        return mainPodDockerImage != null
    }

    override fun restartProject(id: ProjectId) {
        val mainPodDockerImage = kubernetes.getCurrentDockerImage(id)
        check(mainPodDockerImage != null) { "Project $id isn't running" }
        exec("/opt/shepherd/shepherd-apply", id.id, mainPodDockerImage)
    }

    override fun doDeleteProject(id: ProjectId) {
        kubernetes.deleteIfExists(id)
    }

    override fun getRunLogs(id: ProjectId): String {
        checkProjectExists(id)
        return kubernetes.getRunLogs(id)
    }
    override fun getRunMetrics(id: ProjectId): ResourcesUsage {
        checkProjectExists(id)
        return kubernetes.getMetrics(id)
    }

    override fun close() {}
}
