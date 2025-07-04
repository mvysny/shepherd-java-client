package com.github.mvysny.shepherd.api.containers

import com.github.mvysny.shepherd.api.ResourcesUsage
import com.github.mvysny.shepherd.api.exec
import com.github.mvysny.shepherd.api.splitByWhitespaces

/**
 * Very simple Docker client, uses the `docker` binary.
 */
internal object DockerClient {
    /**
     * Create Docker network with given [networkName]: `docker network create`. Fails if such
     * network already exists.
     */
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
     * Disconnects given [networkName] from given [containerName]. Fails if either network or container doesn't exist.
     * Fails if given network isn't connected to given container. Works both with running and stopped containers.
     */
    fun networkDisconnect(networkName: String, containerName: String) {
        exec("docker", "network", "disconnect", networkName, containerName)
    }

    /**
     * Removes given Docker [networkName]. Fails if no such network exists, or if the network is still connected
     * to any container, running or stopped.
     */
    fun networkRm(networkName: String) {
        exec("docker", "network", "rm", networkName)
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
     * Removes stopped container. Fails if no such container exists. Shouldn't be used on running containers: call [containerStop] to stop the container gracefully before calling this.
     */
    fun containerRm(containerName: String) {
        exec("docker", "container", "rm", containerName)
    }

    /**
     * The main process inside the container will receive SIGTERM, and after a grace period, SIGKILL.
     * The grace period is 10 seconds for Linux containers. Then, the container is removed.
     * If the container exists but is stopped, it is merely removed.
     *
     * When this function finishes successfully, the container no longer exists.
     *
     * Fails if no such container exists.
     */
    fun kill(containerName: String) {
        containerStop(containerName)
        containerRm(containerName)
    }

    /**
     * The main process inside the container will receive SIGTERM, and after a grace period, SIGKILL.
     * The grace period is 10 seconds for Linux containers. Then, the container is removed.
     * If the container exists but is stopped, it is merely removed.
     *
     * When this function finishes successfully, the container no longer exists.
     *
     * Does nothing if no such container exists.
     */
    fun killIfExists(containerName: String) {
        if (containerExists(containerName)) {
            kill(containerName)
        }
    }

    /**
     * Checks if a container [containerName] is running. Returns false if the container is stopped or doesn't exist.
     */
    fun isRunning(containerName: String): Boolean = ps().contains(containerName)

    /**
     * Checks if a container [containerName] exists. Returns true when the container runs or is stopped; false if it doesn't exist.
     */
    fun containerExists(containerName: String): Boolean = psA().contains(containerName)

    /**
     * Returns the stdout of given container. Works both with running and stopped containers. Fails if the container doesn't exist.
     */
    fun logs(containerName: String): String = exec("docker", "logs", containerName)

    /**
     * Returns the runtime metrics of given docker container. Works both with running and stopped containers. Fails if the container doesn't exist.
     */
    fun containerStats(containerName: String): ResourcesUsage {
        val stats = exec("docker", "container", "stats", "--no-stream", "--format", "{{.CPUPerc}} {{.MemUsage}}", containerName)
        // returns: 0.16% 128.1MiB / 256MiB
        return parseContainerStats(stats)
    }

    /**
     * @param stats e.g. `"0.16% 128.1MiB / 256MiB"` or `"0.20% 259MiB / 7.549GiB"`
     */
    internal fun parseContainerStats(stats: String): ResourcesUsage {
        val statsSplit = stats.splitByWhitespaces()
        val cpuUsage = statsSplit[0].trimEnd('%').toFloat()
        val memUsage = statsSplit[1]
        require(memUsage.endsWith("MiB") || memUsage.endsWith("GiB")) { "Unexpected: $stats: the memory value doesn't end with MiB or GiB" }
        var memoryUsage = statsSplit[1].dropLast(3).toFloat()
        if (memUsage.endsWith("GiB")) {
            memoryUsage = memoryUsage * 1000
        }
        return ResourcesUsage(memoryMb = memoryUsage.toInt(), cpu = cpuUsage)
    }
}
