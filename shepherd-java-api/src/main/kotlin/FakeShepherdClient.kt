package com.github.mvysny.shepherd.api

import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import kotlin.io.path.*
import kotlin.random.Random

private val fakeProject = Project(
    id = ProjectId("vaadin-boot-example-gradle"),
    description = "Gradle example for Vaadin Boot",
    gitRepo = GitRepo("https://github.com/mvysny/vaadin-boot-example-gradle", "master"),
    owner = ProjectOwner("Martin Vysny", "mavi@vaadin.com"),
    runtime = ProjectRuntime(Resources.defaultRuntimeResources),
    build = BuildSpec(resources = Resources.defaultBuildResources)
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
    private val projectConfigFolder = ProjectConfigFolder(rootFolder / "java" / "projects")
    init {
        Files.createDirectories(projectConfigFolder.folder)
        createProject(fakeProject)
    }

    override fun getAllProjectIDs(): List<ProjectId> = projectConfigFolder.getAllProjects()

    override fun getAllProjects(ownerEmail: String?): List<ProjectView> {
        var projects = getAllProjectIDs().map { getProjectInfo(it) }
        if (ownerEmail != null) {
            projects = projects.filter { it.owner.email == ownerEmail }
        }
        return projects.map { ProjectView(it, Build(1, Duration.ofMinutes(3), Duration.ofMinutes(5), Instant.now(), BuildResult.BUILDING)) }
    }

    override fun getProjectInfo(id: ProjectId): Project = projectConfigFolder.getProjectInfo(id)

    override fun createProject(project: Project) {
        projectConfigFolder.requireProjectDoesntExist(project.id)
        projectConfigFolder.writeProjectJson(project)
    }

    override fun updateProject(project: Project) {
        val oldProject = projectConfigFolder.getProjectInfo(project.id)
        require(oldProject.gitRepo == project.gitRepo) { "gitRepo is not allowed to be changed: new ${project.gitRepo} old ${project.gitRepo}" }
        projectConfigFolder.writeProjectJson(project)
    }

    override fun deleteProject(id: ProjectId) {
        projectConfigFolder.deleteIfExists(id)
    }

    override fun getRunLogs(id: ProjectId): String {
        projectConfigFolder.requireProjectExists(id)
        return """
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

    override fun getRunMetrics(id: ProjectId): ResourcesUsage = ResourcesUsage(
        Random.nextInt(64, 256),
        Random.nextFloat()
    )

    override fun getLastBuilds(id: ProjectId): List<Build> = listOf(Build(1, Duration.ofMinutes(3), Duration.ofMinutes(5), Instant.now(), BuildResult.BUILDING))

    override fun getBuildLog(id: ProjectId, buildNumber: Int): String = """
Dummy build log
    """.trim()

    override fun getConfig(): Config = Config(10240, 2)

    override fun close() {}
}
