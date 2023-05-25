package com.github.mvysny.shepherd.api

/**
 * A very simple Kubernetes client, retrieves stuff by running the kubectl binary.
 * @property kubectl the kubectl binary
 */
public class SimpleKubernetesClient(private val kubectl: String = "mkctl") {
    /**
     * Runs `kubectl get pods` and returns all names.
     */
    public fun getPods(namespace: String): List<String> {
        val stdout = exec(kubectl, "get", "pods", "--namespace", namespace)
        return stdout.lines()
            .drop(1)
            .map { it.split(' ').first() }
    }

    /**
     * Returns the logs of given pod.
     */
    public fun getLogs(podName: String, namespace: String): String =
        exec(kubectl, "logs", podName, "--namespace", namespace)
}
