package com.github.mvysny.shepherd.api

/**
 * A very simple Kubernetes client, retrieves stuff by running the kubectl binary.
 * @property kubectl the kubectl binary
 */
public class SimpleKubernetesClient(private val kubectl: Array<String> = arrayOf("microk8s", "kubectl")) {
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
}
