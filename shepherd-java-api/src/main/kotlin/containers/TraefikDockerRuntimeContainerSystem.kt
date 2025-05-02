package com.github.mvysny.shepherd.api.containers

import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.api.ProjectId

/**
 * Interacts with the [Shepherd-Traefik](https://github.com/mvysny/shepherd-traefik) system running via Docker+Traefik.
 */
public class TraefikDockerRuntimeContainerSystem : RuntimeContainerSystem {
    private val ProjectId.dockerNetworkName: String get() = "${id}.shepherd"
    private val ProjectId.dockerContainerName: String get() = "shepherd_${id}"

    override fun createProject(project: Project) {
        // Creates a new Docker network for given project and connect the network
        // to the Traefik container.
        DockerClient.networkCreate(project.id.dockerNetworkName)
        DockerClient.networkConnect(project.id.dockerNetworkName, getTraefikContainerId())
        // The project containers will be connected to the network later on, when Jenkins
        // finishes its first build of the project.
    }

    private fun getTraefikContainerId(): String {
        val containers = DockerClient.ps()
        val traefik = containers.firstOrNull { it.endsWith("_traefik_1") }
        checkNotNull(traefik) { "Traefik Docker Container is not running" }
        return traefik
    }

    override fun deleteProject(id: ProjectId) {
        // @todo kill the database as well
        if (DockerClient.psA().contains(id.dockerContainerName)) {
            DockerClient.kill(id.dockerContainerName)
        }
        if (DockerClient.doesNetworkExist(id.dockerNetworkName)) {
            DockerClient.networkDisconnect(id.dockerNetworkName, getTraefikContainerId())
            DockerClient.networkRm(id.dockerNetworkName)
        }
    }

    override fun updateProjectConfig(project: Project): Boolean = true

    override fun isProjectRunning(id: ProjectId): Boolean =
        DockerClient.isRunning(id.dockerContainerName)

    override fun restartProject(id: ProjectId) {
        // no need to kill the associated databases; only kill & restart the main container.
        DockerClient.kill(id.dockerContainerName)
        // todo: here the project should be started via the `docker` command
        // however, the same command is present in Shepherd-Traefik `shepherd-build` command,
        // so we should create one script which does the same thing.
        TODO("Not yet implemented")
    }

    override fun getRunLogs(id: ProjectId): String =
        DockerClient.logs(id.dockerContainerName)

    override fun getRunMetrics(id: ProjectId): ResourcesUsage =
        if (!DockerClient.isRunning(id.dockerContainerName)) ResourcesUsage.zero else DockerClient.containerStats(id.dockerContainerName)
}
