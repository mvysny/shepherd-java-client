package com.github.mvysny.shepherd.api.containers

import com.github.mvysny.shepherd.api.ClientFeatures
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.api.ResourcesUsage
import com.github.mvysny.shepherd.api.ServiceType
import com.github.mvysny.shepherd.api.exec
import java.util.EnumSet

/**
 * Interacts with the [Shepherd-Traefik](https://github.com/mvysny/shepherd-traefik) system running via Docker+Traefik.
 * @property hostDNS the main/default DNS domain where Shepherd is running, e.g. `v-herd.eu`.
 */
public class TraefikDockerRuntimeContainerSystem(
    public val hostDNS: String
) : RuntimeContainerSystem {
    private val ProjectId.dockerNetworkName: String get() = "${id}.shepherd"
    private val ProjectId.dockerContainerName: String get() = "shepherd_${id}"
    private val ProjectId.dockerPostgresContainerName: String get() = "shepherd_${id}_psql"
    private val ProjectId.dockerImageName: String get() = "shepherd/${id}"

    override fun createProject(project: Project) {
        require(project.additionalServices.isEmpty()) { "Additional services not yet supported" }

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
        DockerClient.killIfExists(id.dockerContainerName)
        DockerClient.killIfExists(id.dockerPostgresContainerName)
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
        return calculateDockerRunCommand(oldProject) != calculateDockerRunCommand(newProject)
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
        if (project.publication.publishOnMainDomain) {
            cmdline.addAll(listOf("--label", "traefik.http.routers.shepherd_${project.id.id}.entrypoints=https"))
            cmdline.addAll(listOf("--label", "traefik.http.routers.shepherd_${project.id.id}.rule=Host(`${project.id.id}.${hostDNS}`)"))
            cmdline.addAll(listOf("--label", "traefik.http.routers.shepherd_${project.id.id}.tls=true"))
            cmdline.addAll(listOf("--label", "traefik.http.routers.shepherd_${project.id.id}.tls.certresolver=default_shepherd"))
            cmdline.addAll(listOf("--label", "traefik.http.routers.shepherd_${project.id.id}.tls.domains[0].main=${hostDNS}"))
            cmdline.addAll(listOf("--label", "traefik.http.routers.shepherd_${project.id.id}.tls.domains[0].sans=*.${hostDNS}"))
        }
        if (project.publication.additionalDomains.isNotEmpty()) {
            val hostClause = project.publication.additionalDomains.joinToString(separator = " || ") { "Host(`${it}`)" }
            cmdline.addAll(listOf("--label", "traefik.http.routers.shepherd_${project.id.id}_http.entrypoints=http"))
            cmdline.addAll(listOf("--label", "traefik.http.routers.shepherd_${project.id.id}_http.rule=$hostClause"))
        }
        // which image to run. Jenkins is configured to build to `dockerImageName`.
        cmdline.add("${project.id.dockerImageName}:latest")
        return cmdline
    }

    override fun restartProject(project: Project) {
        // no need to kill the associated databases; only kill & restart the main container.
        DockerClient.killIfExists(project.id.dockerContainerName)
        if (project.additionalServices.none { it.type == ServiceType.Postgres }) {
            DockerClient.killIfExists(project.id.dockerPostgresContainerName)
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

    override val features: ClientFeatures
        get() = ClientFeatures(false, true, false, false, EnumSet.noneOf(ServiceType::class.java))

    override val name: String
        get() = "Traefik"
}
