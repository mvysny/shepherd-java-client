package com.github.mvysny.shepherd.api.containers

import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.api.ResourcesUsage

/**
 * Interacts with the [Shepherd-Traefik](https://github.com/mvysny/shepherd-traefik) system running via Docker+Traefik.
 */
public class TraefikDockerRuntimeContainerSystem : RuntimeContainerSystem {
    private val ProjectId.dockerNetworkName: String get() = "${id}.shepherd"
    private val ProjectId.dockerContainerName: String get() = "shepherd_${id}"

    override fun createProject(project: Project) {
        require(project.additionalServices.isEmpty()) { "Additional services not yet supported" }
        require(project.publication.additionalDomains.isEmpty()) { "Additional domains not yet supported" }

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

    override fun updateProjectConfig(project: Project): Boolean {
        // @todo consider all input parameters that go into docker run command-line; return true if any of those changes.
        // simple fallback for now: always return true, so that the project is always restarted.
        return true
    }

    override fun isProjectRunning(id: ProjectId): Boolean =
        DockerClient.isRunning(id.dockerContainerName)

    override fun restartProject(id: ProjectId) {
        // no need to kill the associated databases; only kill & restart the main container.
        DockerClient.kill(id.dockerContainerName)
        // todo: here the project should be started via the `docker` command
        // however, the same command is present in Shepherd-Traefik `shepherd-build` command,
        // so we should create one script which does the same thing.

        // since `updateProjectConfig()` also needs to know the script parameters, it's probably better
        // to have shepherd-java-api handle everything. However, if Jenkins needs to call
        // shepherd-java-api, it needs to have stuff on classpath and java installed - is that viable?
        TODO("Not yet implemented")
    }

    override fun getRunLogs(id: ProjectId): String =
        DockerClient.logs(id.dockerContainerName)

    override fun getRunMetrics(id: ProjectId): ResourcesUsage =
        if (!DockerClient.isRunning(id.dockerContainerName)) ResourcesUsage.zero else DockerClient.containerStats(id.dockerContainerName)
}
