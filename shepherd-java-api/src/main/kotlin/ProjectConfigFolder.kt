@file:OptIn(ExperimentalSerializationApi::class)

package com.github.mvysny.shepherd.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Manages the project config [folder].
 */
internal class ProjectConfigFolder(val folder: Path) {
    private val json = Json { prettyPrint = true }

    fun getAllProjects(): List<ProjectId> {
        val files = Files.list(folder)
            .map { it.name }
            .toList()
        return files.map { ProjectId(it.removeSuffix(".json")) }
    }

    private fun getConfigFile(id: ProjectId): Path = folder.resolve(id.id + ".json")

    fun requireProjectExists(id: ProjectId): Path {
        val configFile = getConfigFile(id)
        require(configFile.exists()) { "Project $id doesn't exist: no such file $configFile" }
        return configFile
    }

    fun requireProjectDoesntExist(id: ProjectId) {
        val configFile = getConfigFile(id)
        require(!configFile.exists()) { "Project $id already exists: the file $configFile exists" }
    }

    fun getProjectInfo(id: ProjectId): Project =
        getConfigFile(id)
            .inputStream().buffered().use { stream -> json.decodeFromStream(stream) }

    fun writeProjectJson(project: Project) {
        getConfigFile(project.id).outputStream().buffered().use { out -> json.encodeToStream(project, out) }
    }

    /**
     * Deletes config file for given project. Does nothing if the file doesn't exist.
     */
    fun deleteIfExists(id: ProjectId) {
        val f = getConfigFile(id)
        if (!f.deleteIfExists()) {
            log.warn("File $f doesn't exist, not deleted")
        }
    }

    companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(ProjectConfigFolder::class.java)
    }
}