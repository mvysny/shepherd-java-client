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
    description = "vaadin-boot-example-gradle",
    gitRepo = "https://github.com/mvysny/vaadin-boot-example-gradle",
    owner = ProjectOwner("Martin Vysny", "mavi@vaadin.com"),
    runtimeResources = Resources.defaultRuntimeResources,
    build = Build(resources = Resources.defaultBuildResources)
)

/**
 * A fake implementation of [ShepherdClient], doesn't require any Kubernetes running. Initially
 * populated with one fake project.
 */
public object FakeClient : ShepherdClient {
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
}
