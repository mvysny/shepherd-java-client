package com.github.mvysny.shepherd.api

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Manages the project config [folder] which contains project JSONs. Defaults to `/etc/shepherd/java/projects`.
 */
public class ProjectConfigFolder(public val folder: Path) {
    /**
     * Returns all registered projects, sorted ascending by their IDs.
     */
    public fun getAllProjects(): List<ProjectId> {
        val files = Files.list(folder)
            .map { it.name }
            .toList()
        return files.map { ProjectId(it.removeSuffix(".json")) }
            .sorted()
    }

    private fun getConfigFile(id: ProjectId): Path = folder.resolve(id.id + ".json")

    public fun requireProjectExists(id: ProjectId): Path {
        val configFile = getConfigFile(id)
        if (!configFile.exists()) {
            throw NoSuchProjectException(id)
        }
        return configFile
    }

    public fun requireProjectDoesntExist(id: ProjectId) {
        val configFile = getConfigFile(id)
        require(!configFile.exists()) { "Project $id already exists: the file $configFile exists" }
    }

    /**
     * Loads config file for given project. Fails if the project json file doesn't exist.
     */
    public fun getProjectInfo(id: ProjectId): Project =
        Project.loadFromFile(requireProjectExists(id))

    public fun writeProjectJson(project: Project) {
        val file = getConfigFile(project.id)
        log.info("Writing project json file to $file")
        project.saveToFile(file)
    }

    /**
     * Deletes config file for given project. Does nothing if the file doesn't exist.
     */
    public fun deleteIfExists(id: ProjectId) {
        val f = getConfigFile(id)
        if (!f.deleteIfExists()) {
            log.warn("File $f doesn't exist, not deleted")
        } else {
            log.info("Deleted project file $f")
        }
    }

    /**
     * Deletes config file for given project. Does nothing if the file doesn't exist.
     */
    public fun delete(id: ProjectId) {
        val f = getConfigFile(id)
        if (!f.deleteIfExists()) {
            throw NoSuchProjectException(id)
        } else {
            log.info("Deleted project file $f")
        }
    }

    public companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(ProjectConfigFolder::class.java)

        public val LINUX_ROOT_FOLDER: Path = Path("/etc/shepherd")

        public fun defaultLinux(rootFolder: Path = LINUX_ROOT_FOLDER) : ProjectConfigFolder = ProjectConfigFolder(rootFolder / "java" / "projects")
    }
}
