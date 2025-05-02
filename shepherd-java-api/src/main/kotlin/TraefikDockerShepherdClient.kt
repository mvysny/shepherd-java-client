package com.github.mvysny.shepherd.api

/**
 * Interacts with the [Shepherd-Traefik](https://github.com/mvysny/shepherd-traefik) system running via Docker+Traefik.
 */
public class TraefikDockerShepherdClient(fs: LocalFS) : AbstractJenkinsBasedShepherdClient(fs) {
    private val docker = SimpleDockerClient()
    override fun doCreateProject(project: Project) {
        docker.createNetworkAndConnect(project.id)
    }

    override fun doDeleteProject(id: ProjectId) {
        docker.deleteIfExists(id)
    }

    override fun doUpdateProjectConfig(project: Project): Boolean = true

    override fun isProjectRunning(id: ProjectId): Boolean =
        docker.isRunning(id)

    override fun restartProject(id: ProjectId) {
        docker.kill(id)
        // todo: here the project should be started via the `docker` command
        // however, the same command is present in Shepherd-Traefik `shepherd-build` command,
        // so we should create one script which does the same thing.
    }

    override fun getRunLogs(id: ProjectId): String =
        docker.getRunLogs(id)

    override fun getRunMetrics(id: ProjectId): ResourcesUsage =
        docker.getRunMetrics(id)

    override fun close() {
    }
}

/**
 * Interacts with the [Shepherd-Traefik](https://github.com/mvysny/shepherd-traefik) system running via Docker+Traefik.
 */
public class TraefikDockerRuntimeContainerSystem : RuntimeContainerSystem {
    private val docker = SimpleDockerClient()
    override fun createProject(project: Project) {
        docker.createNetworkAndConnect(project.id)
    }

    override fun deleteProject(id: ProjectId) {
        docker.deleteIfExists(id)
    }

    override fun updateProjectConfig(project: Project): Boolean = true

    override fun isProjectRunning(id: ProjectId): Boolean =
        docker.isRunning(id)

    override fun restartProject(id: ProjectId) {
        docker.kill(id)
        // todo: here the project should be started via the `docker` command
        // however, the same command is present in Shepherd-Traefik `shepherd-build` command,
        // so we should create one script which does the same thing.
        TODO("Not yet implemented")
    }

    override fun getRunLogs(id: ProjectId): String =
        docker.getRunLogs(id)

    override fun getRunMetrics(id: ProjectId): ResourcesUsage =
        docker.getRunMetrics(id)
}
