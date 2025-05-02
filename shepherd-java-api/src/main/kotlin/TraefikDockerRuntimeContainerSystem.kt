package com.github.mvysny.shepherd.api

/**
 * Interacts with the [Shepherd-Traefik](https://github.com/mvysny/shepherd-traefik) system running via Docker+Traefik.
 */
public class TraefikDockerRuntimeContainerSystem : RuntimeContainerSystem {
    private val ProjectId.dockerNetworkName: String get() = "${id}.shepherd"
    private val ProjectId.dockerContainerName: String get() = "shepherd_${id}"

    override fun createProject(project: Project) {
        DockerClient.createNetworkAndConnect(project.id)
    }

    override fun deleteProject(id: ProjectId) {
        DockerClient.deleteIfExists(id)
    }

    override fun updateProjectConfig(project: Project): Boolean = true

    override fun isProjectRunning(id: ProjectId): Boolean =
        DockerClient.isRunning(id)

    override fun restartProject(id: ProjectId) {
        // no need to kill the associated databases; only kill & restart the main container.
        DockerClient.kill(id.dockerNetworkName)
        // todo: here the project should be started via the `docker` command
        // however, the same command is present in Shepherd-Traefik `shepherd-build` command,
        // so we should create one script which does the same thing.
        TODO("Not yet implemented")
    }

    override fun getRunLogs(id: ProjectId): String =
        DockerClient.getRunLogs(id)

    override fun getRunMetrics(id: ProjectId): ResourcesUsage =
        DockerClient.getRunMetrics(id)
}
