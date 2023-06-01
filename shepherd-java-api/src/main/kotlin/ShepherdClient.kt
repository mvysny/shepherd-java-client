package com.github.mvysny.shepherd.api

import java.io.Closeable
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

public interface ShepherdClient : Closeable {
    /**
     * Lists all registered projects.
     */
    public fun getAllProjectIDs(): List<ProjectId>

    /**
     * Gets all projects, including metadata.
     * @param ownerEmail if not null, return only projects owned by given owner (e-mail address, refers to [ProjectOwner.email].
     */
    public fun getAllProjects(ownerEmail: String? = null): List<ProjectView>

    /**
     * Retrieves info about given project. Fails with an exception if there is no such project.
     */
    public fun getProjectInfo(id: ProjectId): Project

    /**
     * Creates new [project]:
     *
     * * creates a config file for it on the filesystem (`/etc/shepherd/projects`);
     * * creates a Kubernetes config file for the project
     * * registers the project to Jenkins and starts first Jenkins build. The build is configured to call the `shepherd-build` script.
     *
     * Fails if the project json config file already exists. Blocks until the project is created.
     */
    public fun createProject(project: Project)

    /**
     * Updates the project:
     * * updates the project config file on the filesystem (`/etc/shepherd/projects`);
     * * update a Kubernetes config file for the project, dropping all Kubernetes objects that are no longer necessary.
     * * updates the project registration in Jenkins.
     * Fails if the project json config file doesn't exist yet.
     *
     * Restarts the project automatically:
     * * either starts a new build in Jenkins if there were any changes in [BuildSpec.buildArgs] or [BuildSpec.dockerFile].
     * * otherwise only re-applies the Kubernetes yaml file (if it has been changed).
     *
     * Note that some properties can not be changed (an exception is thrown by this function if such a change is detected):
     * * [Project.id]
     * * [Project.gitRepo]
     *
     * Blocks until the update is fully completed.
     */
    public fun updateProject(project: Project)

    /**
     * Deletes given project: stops and removes all builds, stops and removes all Kubernetes rules,
     * and removes the project json config (from `/etc/shepherd/projects`).
     *
     * This function will still try to unregister the project from Jenkins and Kubernetes
     * even if the project json config is already nonexistent.
     *
     * Blocks until the project is fully deleted. This may take up to 1 minute since
     * cleanup of Kubernetes objects is a lengthy operation.
     */
    public fun deleteProject(id: ProjectId)

    /**
     * Retrieves the run logs of the main app pod (=the app itself). There may be additional pods (e.g. PostgreSQL)
     * but their logs are not returned.
     */
    public fun getRunLogs(id: ProjectId): String

    /**
     * Returns the current CPU/memory usage of the main app pod.
     */
    public fun getRunMetrics(id: ProjectId): ResourcesUsage

    /**
     * Retrieve the last 30 builds for given project [id].
     * @return the list of builds, sorted by [Build.number] ascending.
     */
    public fun getLastBuilds(id: ProjectId): List<Build>

    /**
     * Retrieves the full build log (stdout).
     * @param buildNumber pass in [Build.number].
     */
    public fun getBuildLog(id: ProjectId, buildNumber: Int): String

    /**
     * INTERNAL, DON'T EXPOSE, may contain sensitive settings, private keys etc. Not
     * supposed to be shown/leaked to users.
     */
    public fun getConfig(): Config

    /**
     * Returns Shepherd runtime statistics.
     */
    public fun getStats(): Stats {
        val config = getConfig()
        val projects = getAllProjectIDs().map { getProjectInfo(it) }
        return Stats.calculate(config, projects)
    }
}

/**
 * @property lastBuild project last build. May be null if there is no build for the project yet.
 */
public data class ProjectView(
    val project: Project,
    val lastBuild: Build?
) {
    val lastBuildOngoing: Boolean get() = lastBuild != null && !lastBuild.isCompleted
    /**
     * Returns URLs on which this project runs (can be browsed to). E.g. for `vaadin-boot-example-gradle`
     * on the `v-herd.eu` [host], this returns `https://v-herd.eu/vaadin-boot-example-gradle`.
     */
    public fun getPublishedURLs(host: String): List<String> = project.getPublishedURLs(host)
}

public enum class BuildResult {
    FAILURE,
    UNSTABLE,
    REBUILDING,
    BUILDING,

    /**
     * This means a job was already running and has been aborted.
     */
    ABORTED,

    /**
     *
     */
    SUCCESS,

    /**
     * ?
     */
    UNKNOWN,

    /**
     * This is returned if a job has never been built.
     */
    NOT_BUILT,

    /**
     * This will be the result of a job in cases where it has been cancelled
     * during the time in the queue.
     */
    CANCELLED
}

/**
 * Resources the app uses.
 * @property memoryMb current memory usage, in megabytes.
 * @property cpu CPU usage, relative to one core. 1 means 1 CPU core is fully used; 0.5 means that
 * half of one CPU core is used; 2 means that two CPU cores are fully utilized.
 */
public data class ResourcesUsage(
    val memoryMb: Int,
    val cpu: Float
) {
    init {
        require(memoryMb >= 0) { "memoryMb: must be 0 or higher but got $memoryMb" }
        require(cpu >= 0) { "cpu: must be 0 or higher but was $cpu" }
    }
    public companion object {
        @JvmStatic
        public val zero: ResourcesUsage = ResourcesUsage(0, 0f)
    }
}

/**
 * Contains information about a particular build of an app.
 * @property number the build number, starts with 1, second build has the number of 2 etc.
 * @property duration how long the build took. 0 if the build is not yet complete.
 * @property estimatedDuration estimated duration based on previous builds
 * @property buildStarted when the build started
 * @property outcome the outcome or [BuildResult.BUILDING] if not yet completed.
 */
public data class Build(
    val number: Int,
    val duration: Duration,
    val estimatedDuration: Duration,
    val buildStarted: Instant,
    val outcome: BuildResult
) {
    val isCompleted: Boolean get() = outcome != BuildResult.BUILDING

    /**
     * When the build ended; null if the build is not yet completed.
     */
    val buildEnded: Instant? get() = if (!isCompleted) {
        null
    } else {
        buildStarted + duration
    }
}

/**
 * Shepherd runtime statistics.
 * @property maxAvailableMemoryMb [Config.maxAvailableMemoryMb]
 * @property concurrentJenkinsBuilders [Config.concurrentJenkinsBuilders]
 * @property currentMemoryUsageMb current memory usage: assume all VMs are running and no builds are running
 * @property currentMaxMemoryUsageMb current max memory usage: assume all VMs are running and the most memory-intensive builds are running.
 * Calculated as a sum of all project runtime memory usage + top x build memory usage, where x is [Config.concurrentJenkinsBuilders].
 * @property projectCount number of projects registered to Shepherd
 */
public data class Stats(
    val maxAvailableMemoryMb: Int,
    val concurrentJenkinsBuilders: Int,
    val currentMemoryUsageMb: Int,
    val currentMaxMemoryUsageMb: Int,
    val projectCount: Int
) {
    public companion object {
        public fun calculate(config: Config, projects: List<Project>): Stats {
            val currentMemoryUsageMb = projects.sumOf { it.runtime.resources.memoryMb }
            val mostBuildMemIntensiveProjects = projects
                .sortedBy { it.build.resources.memoryMb }
                .takeLast(config.concurrentJenkinsBuilders)
            val currentMaxMemoryUsageMb = currentMemoryUsageMb + mostBuildMemIntensiveProjects.sumOf { it.build.resources.memoryMb }
            return Stats(config.maxAvailableMemoryMb, config.concurrentJenkinsBuilders, currentMemoryUsageMb, currentMaxMemoryUsageMb, projects.size)
        }
    }
}
