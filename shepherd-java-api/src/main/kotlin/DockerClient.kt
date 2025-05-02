package com.github.mvysny.shepherd.api

/**
 * Very simple Docker client, uses the `docker` binary.
 */
internal object DockerClient {
    private val ProjectId.dockerNetworkName: String get() = "${id}.shepherd"
    private val ProjectId.dockerContainerName: String get() = "shepherd_${id}"

    /**
     * Creates a new Docker network for given project [pid] and connect the network
     * to the Traefik container.
     */
    fun createNetworkAndConnect(pid: ProjectId) {
        networkCreate(pid.dockerNetworkName)
        networkConnect(pid.dockerNetworkName, getTraefikContainerId())
    }

    fun networkCreate(networkName: String) {
        exec("docker", "network", "create", networkName)
    }

    fun networkConnect(networkName: String, runningContainerName: String) {
        exec("docker", "network", "connect", networkName, runningContainerName)
    }

    /**
     * Returns names of Docker running containers: `docker ps`.
     */
    fun ps(): Set<String> {
        val containers = exec("docker", "ps", "--format", "{{.Names}}").lines()
        return containers.filter { it.isNotEmpty() }.toSet()
    }

    /**
     * Returns names of all Docker containers, both running and stopped: `docker ps -a`.
     */
    fun psA(): Set<String> {
        val containers = exec("docker", "ps", "-a", "--format", "{{.Names}}").lines()
        return containers.filter { it.isNotEmpty() }.toSet()
    }

    /**
     * Return the names of networks currently created in Docker: `docker network ls`
     */
    fun networkLs(): Set<String> {
        val output = exec("docker", "network", "ls", "--format", "{{.Name}}").lines()
        return output.filter { it.isNotEmpty() }.toSet()
    }

    /**
     * Checks whether network with given name is created in Docker.
     */
    fun doesNetworkExist(networkName: String): Boolean {
        val networks = networkLs()
        return networks.contains(networkName)
    }

    /**
     * Deletes Docker network for project [pid]. The container must not be running, and Traefik must still be connected to this network.
     * Does nothing if the network doesn't exist.
     */
    fun disconnectNetworkAndDelete(pid: ProjectId) {
        if (doesNetworkExist(pid.dockerNetworkName)) {
            exec("docker", "network", "disconnect", pid.dockerNetworkName, getTraefikContainerId())
            exec("docker", "network", "rm", pid.dockerNetworkName)
        }
    }

    fun deleteIfExists(pid: ProjectId) {
        if (isRunning(pid.dockerNetworkName)) {
            kill(pid.dockerContainerName)
        }
        disconnectNetworkAndDelete(pid)
    }

    /**
     * The main process inside the container will receive SIGTERM, and after a grace period, SIGKILL.
     * The grace period is 10 seconds for Linux containers. Does nothing if the container is already stopped.
     * Fails if no such container exists.
     */
    fun containerStop(containerName: String) {
        exec("docker", "container", "stop", containerName)
    }

    /**
     * Removes stopped container. Fails if no such container exists.
     */
    fun containerRm(containerName: String) {
        exec("docker", "container", "rm", containerName)
    }

    /**
     * The main process inside the container will receive SIGTERM, and after a grace period, SIGKILL.
     * The grace period is 10 seconds for Linux containers. Then, the container is removed.
     */
    fun kill(containerName: String) {
        containerStop(containerName)
        containerRm(containerName)
    }

    /**
     * Checks if a container [containerName] is running.
     */
    fun isRunning(containerName: String): Boolean = ps().contains(containerName)

    /**
     * Returns the stdout of given container. Fails if the container doesn't exist.
     */
    fun logs(containerName: String): String = exec("docker", "logs", containerName)

    /**
     * Returns the runtime metrics of given docker container. Works with stopped containers as well. Fails if the container doesn't exist.
     */
    fun containerStats(containerName: String): ResourcesUsage {
        val stats = exec("docker", "container", "stats", "--no-stream", "--format", "{{.CPUPerc}} {{.MemUsage}}", containerName)
        // returns: 0.16% 128.1MiB / 256MiB
        val statsSplit = stats.splitByWhitespaces()
        val cpuUsage = statsSplit[0].trimEnd('%').toFloat()
        require(statsSplit[1].endsWith("MiB")) { "Unexpected: $stats: the memory value doesn't end with MiB" }
        val memoryUsage = statsSplit[1].removeSuffix("MiB").toInt()
        return ResourcesUsage(memoryMb = memoryUsage, cpu = cpuUsage)
    }

    private fun getTraefikContainerId(): String {
        val containers = ps()
        val traefik = containers.firstOrNull { it.endsWith("_traefik_1") }
        checkNotNull(traefik) { "Traefik Docker Container is not running" }
        return traefik.splitByWhitespaces()[0]
    }
}
