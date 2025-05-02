package com.github.mvysny.shepherd.api

/**
 * Very simple Docker client, uses the `docker` binary. Used by Shepherd-Traefik.
 */
public class SimpleDockerClient() {
    private val ProjectId.dockerNetworkName: String get() = "${id}.shepherd"
    private val ProjectId.dockerContainerName: String get() = "shepherd_${id}"

    /**
     * Creates a new Docker network for given project [pid] and connect the network
     * to the Traefik container.
     */
    public fun createNetworkAndConnect(pid: ProjectId) {
        exec("docker", "network", "create", pid.dockerNetworkName)
        exec("docker", "network", "connect", pid.dockerNetworkName, getTraefikContainerId())
    }

    /**
     * Deletes Docker network for project [pid]. The container must not be running, and Traefik must still be connected to this network.
     */
    public fun disconnectNetworkAndDelete(pid: ProjectId) {
        exec("docker", "network", "disconnect", pid.dockerNetworkName, getTraefikContainerId())
        exec("docker", "network", "rm", pid.dockerNetworkName)
    }

    public fun deleteIfExists(pid: ProjectId) {
        if (isRunning(pid)) {
            kill(pid)
        }
        disconnectNetworkAndDelete(pid)
    }

    /**
     * The main process inside the container will receive SIGTERM, and after a grace period, SIGKILL.
     * The grace period is 10 seconds for Linux containers. Then, the container is removed.
     */
    public fun kill(pid: ProjectId) {
        exec("docker", "container", "stop", pid.dockerContainerName)
        exec("docker", "container", "rm", pid.dockerContainerName)
    }

    /**
     * Checks if a container for project [pid] is running.
     */
    public fun isRunning(pid: ProjectId): Boolean {
        val containers = exec("docker", "ps").lines().drop(1)
        return containers.any { container -> container.endsWith(pid.dockerContainerName) }
    }

    /**
     * Returns the run log of project [pid].
     */
    public fun getRunLogs(pid: ProjectId): String =
        if (!isRunning(pid)) "" else exec("docker", "logs", pid.dockerContainerName)

    /**
     * Returns the runtime metrics of docker container for project [pid].
     */
    public fun getRunMetrics(pid: ProjectId): ResourcesUsage {
        if (!isRunning(pid)) {
            return ResourcesUsage.zero
        }
        val stats = exec("docker", "container", "stats", "--no-stream", "--format", "{{.CPUPerc}} {{.MemUsage}}", pid.dockerContainerName)
        // returns: 0.16% 128.1MiB / 256MiB
        val statsSplit = stats.splitByWhitespaces()
        val cpuUsage = statsSplit[0].trimEnd('%').toFloat()
        require(statsSplit[1].endsWith("MiB")) { "Unexpected: $stats: the memory value doesn't end with MiB" }
        val memoryUsage = statsSplit[1].removeSuffix("MiB").toInt()
        return ResourcesUsage(memoryMb = memoryUsage, cpu = cpuUsage)
    }

    private fun getTraefikContainerId(): String {
        val containers = exec("docker", "ps").lines().drop(1)
        val traefik = containers.firstOrNull { it.endsWith("_traefik_1") }
        checkNotNull(traefik) { "Traefik Docker Container is not running" }
        return traefik.splitByWhitespaces()[0]
    }
}
