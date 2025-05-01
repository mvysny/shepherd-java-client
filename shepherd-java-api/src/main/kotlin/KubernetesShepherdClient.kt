package com.github.mvysny.shepherd.api

import org.slf4j.LoggerFactory

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

    override fun updateProject(project: Project) {
        val oldProject = fs.configFolder.projects.getProjectInfo(project.id)
        require(oldProject.gitRepo.url == project.gitRepo.url) { "gitRepo is not allowed to be changed: new ${project.gitRepo.url} old ${project.gitRepo.url}" }

        checkMemoryUsage(project)

        // 1. Overwrite the project JSON file
        fs.configFolder.projects.writeProjectJson(project)

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
            exec("/opt/shepherd/shepherd-apply", project.id.id, mainPodDockerImage)
        } else {
            // probably just a project description or project owner changed, do nothing
            log.info("${project.id.id}: no kubernetes-level/jenkins-level changes detected, not reloading the project")
        }
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

    public companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(KubernetesShepherdClient::class.java)
    }
}
