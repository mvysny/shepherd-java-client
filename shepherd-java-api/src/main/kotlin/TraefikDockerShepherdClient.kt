package com.github.mvysny.shepherd.api

/**
 * Interacts with the actual [Shepherd-Traefik](https://github.com/mvysny/shepherd-traefik) system running via Docker+Traefik.
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
        TODO("Not yet implemented")
    }

    override fun getRunLogs(id: ProjectId): String =
        docker.getRunLogs(id)

    override fun getRunMetrics(id: ProjectId): ResourcesUsage =
        docker.getRunMetrics(id)

    override fun close() {
    }
}
