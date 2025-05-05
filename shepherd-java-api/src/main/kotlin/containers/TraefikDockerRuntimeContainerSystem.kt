package com.github.mvysny.shepherd.api.containers

import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.api.ResourcesUsage
import com.github.mvysny.shepherd.api.exec

/**
 * Interacts with the [Shepherd-Traefik](https://github.com/mvysny/shepherd-traefik) system running via Docker+Traefik.
 * @property hostDNS the main/default DNS domain where Shepherd is running, e.g. `v-herd.eu`.
 */
public class TraefikDockerRuntimeContainerSystem(
    public val hostDNS: String
) : RuntimeContainerSystem {
    private val ProjectId.dockerNetworkName: String get() = "${id}.shepherd"
    private val ProjectId.dockerContainerName: String get() = "shepherd_${id}"
    private val ProjectId.dockerImageName: String get() = "shepherd/${id}"

    override fun createProject(project: Project) {
        require(project.additionalServices.isEmpty()) { "Additional services not yet supported" }
        require(project.publication.additionalDomains.isEmpty()) { "Additional domains not yet supported" }

        // Creates a new Docker network for given project and connect the network
        // to the Traefik container. This way, the projects are separate from each other
        // and can't reach nor attack each other - they can only see the Traefik container.
        DockerClient.networkCreate(project.id.dockerNetworkName)
        DockerClient.networkConnect(project.id.dockerNetworkName, getTraefikContainerId())
        // The project containers will be connected to the network later on, when Jenkins
        // finishes its first build of the project.
    }

    private fun getTraefikContainerId(): String {
        val containers = DockerClient.ps()
        check(containers.contains("int_traefik")) { "Traefik Docker Container is not running" }
        return "int_traefik"
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

    override fun updateProjectConfig(newProject: Project, oldProject: Project): Boolean {
        // all project configuration is passed in via the docker command line, so there's no
        // file to update.
        // however, return true if the command line is changed => that means that we need to restart the
        // main project container with new settings.
        return calculateDockerRunCommand(oldProject) != calculateDockerRunCommand(
            newProject
        )
    }

    override fun isProjectRunning(id: ProjectId): Boolean =
        DockerClient.isRunning(id.dockerContainerName)

    internal fun calculateDockerRunCommand(project: Project): List<String> {
        // run the container in the background, with stdout (terminal) attached, with given name, and having the docker daemon keeping the container up.
        val cmdline = mutableListOf("docker", "run", "-d", "-t", "--name", project.id.dockerContainerName, "--restart", "always")
        cmdline.addAll(listOf("--network", project.id.dockerNetworkName)) // every project has its own network.
        // configure runtime resources
        cmdline.addAll(listOf("-m", "${project.runtime.resources.memoryMb}m", "--cpus", project.runtime.resources.cpu.toString()))
        // add Traefik labels so that the routing works automatically.
        cmdline.addAll(listOf("--label", "traefik.http.routers.shepherd_${project.id.id}.entrypoints=http"))
        cmdline.addAll(listOf("--label", "traefik.http.routers.shepherd_${project.id.id}.rule=Host(`${project.id.id}.${hostDNS}`)"))
        // which image to run. Jenkins is configured to build to `dockerImageName`.
        cmdline.add("${project.id.dockerImageName}:latest")
        return cmdline
    }

    override fun restartProject(project: Project) {
        // no need to kill the associated databases; only kill & restart the main container.
        // TODO if the project no longer uses a database, kill the database docker container.
        if (DockerClient.containerExists(project.id.dockerContainerName)) {
            DockerClient.kill(project.id.dockerContainerName)
        }

        // start the project via the `docker` command.
        // @todo if the project now uses a database, start the database as well.
        //
        // Because `updateProjectConfig()` also needs to know the script parameters, it's probably better
        // to have shepherd-java-api handle everything. However, if Jenkins needs to call
        // shepherd-java-api, it needs to have stuff on classpath and java installed. Turns out,
        // Java is installed along with Jenkins (since Jenkins runs on Java), and jq is not -
        // and we would need jq to parse shepherd json config file. Therefore,
        // it's easier to use shepherd-cli to restart the project.

        // start the main project container.
        val cmdline = calculateDockerRunCommand(project)
        exec(*cmdline.toTypedArray())
    }

    override fun getRunLogs(id: ProjectId): String =
        DockerClient.logs(id.dockerContainerName)

    override fun getRunMetrics(id: ProjectId): ResourcesUsage =
        if (!DockerClient.isRunning(id.dockerContainerName)) ResourcesUsage.zero else DockerClient.containerStats(id.dockerContainerName)

    override fun getMainDomainDeployURL(id: ProjectId): String = "https://${id.id}.${hostDNS}"
}
