package com.github.mvysny.shepherd.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Interacts with the actual shepherd system.
 * @property etcShepherdPath the root shepherd path, `/etc/shepherd`
 */
public class LinuxShepherdClient(
    private val kubernetesClient: SimpleKubernetesClient = SimpleKubernetesClient(),
    private val etcShepherdPath: Path = Path("/etc/shepherd")
) : ShepherdClient {
    /**
     * Project config JSONs are stored here. Defaults to `/etc/shepherd/projects`.
     */
    private val projectConfigFolder = etcShepherdPath / "projects"
    private val json = Json { prettyPrint = true }

    override fun getAllProjects(): List<ProjectId> {
        val files = Files.list(projectConfigFolder)
            .map { it.name }
            .toList()
        return files.map { ProjectId(it.removeSuffix(".json")) }
    }

    private fun getConfigFile(id: ProjectId): Path = projectConfigFolder.resolve(id.id + ".json")

    override fun getProjectInfo(id: ProjectId): Project =
        getConfigFile(id)
            .inputStream().buffered().use { stream -> json.decodeFromStream(stream) }

    override fun createProject(project: Project) {
        val configFile = getConfigFile(project.id)
        require(!configFile.exists()) { "${project.id}: the file $configFile already exists" }
        configFile.outputStream().buffered().use { out -> json.encodeToStream(project, out) }

        // TODO
        // 1. Create Kubernetes config file at /etc/shepherd/k8s/PROJECT_ID.yaml
        // 2. Create Jenkins job
        // 3. Run Jenkins job
    }

    private val Project.namespace: String get() = "shepherd-${id.id}"

    override fun getRunLogs(project: Project): String {
        // main deployment is always called "deployment"
        val podNames = kubernetesClient.getPods(project.namespace)
        val podName = podNames.firstOrNull { it.startsWith("deployment-") }
        requireNotNull(podName) { "No deployment pod for ${project.id.id}: $podNames" }
        return kubernetesClient.getLogs(podName, project.namespace)
    }
}
