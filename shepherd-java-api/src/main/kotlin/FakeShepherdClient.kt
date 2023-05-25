@file:OptIn(ExperimentalSerializationApi::class)

package com.github.mvysny.shepherd.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

private val fakeProject = Project(
    id = ProjectId("vaadin-boot-example-gradle"),
    description = "Gradle example for Vaadin Boot",
    gitRepo = "https://github.com/mvysny/vaadin-boot-example-gradle",
    owner = ProjectOwner("Martin Vysny", "mavi@vaadin.com"),
    runtimeResources = Resources.defaultRuntimeResources,
    build = Build(resources = Resources.defaultBuildResources)
)

/**
 * A fake implementation of [ShepherdClient], doesn't require any Kubernetes running. Initially
 * populated with one fake project.
 */
public object FakeShepherdClient : ShepherdClient {
    private val rootFolder = createTempDirectory("shepherd-fake-client")

    /**
     * Stores projects as json files named `projectid.json`.
     */
    private val projectConfigFolder = rootFolder.resolve("projects")
    private val json = Json { prettyPrint = true }
    init {
        Files.createDirectories(projectConfigFolder)
        createProject(fakeProject)
    }

    override fun getAllProjects(): List<ProjectId> {
        val files = Files.list(projectConfigFolder)
            .map { it.name }
            .toList()
        return files.map { ProjectId(it.removeSuffix(".json")) }
    }

    private fun getConfigFile(id: ProjectId): Path = projectConfigFolder.resolve(id.id + ".json")

    override fun getProjectInfo(id: ProjectId): Project =
        getConfigFile(id).inputStream().buffered().use { stream -> json.decodeFromStream(stream) }

    override fun createProject(project: Project) {
        val configFile = getConfigFile(project.id)
        require(!configFile.exists()) { "${project.id}: the file $configFile already exists" }
        configFile.outputStream().buffered().use { out -> json.encodeToStream(project, out) }
    }

    override fun getRunLogs(project: Project): String = """
2023-05-15 11:49:40.109 [main] INFO com.github.mvysny.vaadinboot.VaadinBoot - Starting App
2023-05-15 11:49:40.195 [main] INFO com.github.mvysny.vaadinboot.Env - Vaadin production mode is on: jar:file:/app/lib/app.jar!/META-INF/VAADIN/config/flow-build-info.json contains '"productionMode": true'
2023-05-15 11:49:40.622 [main] INFO com.github.mvysny.vaadinboot.Env - WebRoot is served from jar:file:/app/lib/app.jar!/webapp
2023-05-15 11:49:46.417 [main] INFO com.vaadin.flow.server.startup.ServletDeployer - Skipping automatic servlet registration because there is already a Vaadin servlet with the name com.vaadin.flow.server.VaadinServlet-3701eaf6


=================================================
Started in PT6.718S. Running on Java Eclipse Adoptium 17.0.6, OS amd64 Linux 5.15.0-71-generic
Please open http://localhost:8080/ in your browser.
=================================================

Press ENTER or CTRL+C to shutdown
No stdin available. press CTRL+C to shutdown
2023-05-22 21:04:15.328 [qtp473581465-15] INFO com.vaadin.flow.server.DefaultDeploymentConfiguration - Vaadin is running in production mode.
    """.trim()
}
