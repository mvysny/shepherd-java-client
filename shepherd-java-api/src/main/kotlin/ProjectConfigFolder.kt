package com.github.mvysny.shepherd.api

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Manages the project config [folder].
 */
internal class ProjectConfigFolder(val folder: Path) {
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

    /**
     * Loads config file for given project. Fails if the project json file doesn't exist.
     */
    fun getProjectInfo(id: ProjectId): Project =
        Project.loadFromFile(requireProjectExists(id))

    fun writeProjectJson(project: Project) {
        project.saveToFile(getConfigFile(project.id))
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
