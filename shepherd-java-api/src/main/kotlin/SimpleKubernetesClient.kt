package com.github.mvysny.shepherd.api

import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists

/**
 * A very simple Kubernetes client, retrieves stuff by running the kubectl binary.
 * @property kubectl the kubectl binary
 * @property yamlConfigFolder where the kubernetes yaml config files for projects are stored. Shepherd expects
 * this to be `/etc/shepherd/k8s`.
 */
public class SimpleKubernetesClient(
    private val kubectl: Array<String> = arrayOf("microk8s", "kubectl"),
    private val yamlConfigFolder: Path = Path("/etc/shepherd/k8s")
) {
    /**
     * Runs `kubectl get pods` and returns all names.
     */
    private fun getPods(namespace: String): List<String> {
        val stdout = exec(*kubectl, "get", "pods", "--namespace", namespace)
        return stdout.lines()
            .drop(1)
            .map { it.split(' ').first() }
    }

    private val ProjectId.kubernetesNamespace: String get() = "shepherd-${id}"

    /**
     * Returns the logs of given pod.
     */
    private fun getLogs(podName: String, namespace: String): String =
        exec(*kubectl, "logs", podName, "--namespace", namespace)

    public fun getRunLogs(id: ProjectId): String {
        // main deployment is always called "deployment"
        val podNames = getPods(id.kubernetesNamespace)
        val podName = podNames.firstOrNull { it.startsWith("deployment-") }
        requireNotNull(podName) { "No deployment pod for ${id.id}: $podNames" }

        return getLogs(podName, id.kubernetesNamespace)
    }

    private fun getConfigYamlFile(id: ProjectId): Path = yamlConfigFolder / "${id.id}.yaml"

    public fun deleteIfExists(id: ProjectId) {
        val f = getConfigYamlFile(id)
        if (f.exists()) {
            exec(*kubectl, "delete", "-f", f.toString())
        } else {
            log.warn("$f doesn't exist, not deleting project objects from Kubernetes")
        }
    }

    public companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(SimpleKubernetesClient::class.java)
    }
}
