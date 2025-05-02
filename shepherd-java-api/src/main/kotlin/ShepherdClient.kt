package com.github.mvysny.shepherd.api

import java.io.Closeable
import java.io.File
import java.time.Duration
import java.time.Instant

/**
 * Accesses the Shepherd backend. Allows you to create/modify/delete/list apps, get the build/runtime logs, statistics.
 */
public interface ShepherdClient : Closeable {
    /**
     * Lists all registered projects.
     */
    public fun getAllProjectIDs(): List<ProjectId>

    /**
     * Gets all projects, including metadata.
     * @param ownerEmail if not null, return only projects owned/administrated by given owner (e-mail address, refers to [ProjectOwner.email].
     */
    public fun getAllProjects(ownerEmail: String? = null): List<ProjectView>

    /**
     * Retrieves info about given project. Fails with an exception if there is no such project.
     * @throws NoSuchProjectException if the project doesn't exist.
     */
    public fun getProjectInfo(id: ProjectId): Project

    public fun existsProject(id: ProjectId): Boolean

    /**
     * Creates new [project]:
     *
     * * creates a config file for it on the filesystem (`/etc/shepherd/projects`);
     * * creates a Kubernetes config file for the project
     * * registers the project to Jenkins and starts first Jenkins build. The build is configured to call the `shepherd-build` script.
     *
     * Fails if the project json config file already exists. Blocks until the project is created.
     * @throws IllegalArgumentException if the updated project memory or CPU requirements would overflow the
     * available memory or configured max values (see [getConfig] and [Config.maxProjectRuntimeResources]/[Config.maxProjectBuildResources]).
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
     * @throws IllegalArgumentException if the updated project memory or CPU requirements would overflow the
     * available memory or configured max values (see [getConfig] and [Config.maxProjectRuntimeResources]/[Config.maxProjectBuildResources]).
     * @throws NoSuchProjectException if the project doesn't exist.
     */
    public fun updateProject(project: Project)

    /**
     * Deletes given project: stops and removes all builds, stops and removes all Kubernetes rules,
     * and removes the project json config (from `/etc/shepherd/projects`).
     * Kills all running project containers.
     *
     * This function will still try to unregister the project from Jenkins and Kubernetes
     * even if the project json config is already nonexistent.
     *
     * Blocks until the project is fully deleted. This may take up to 1 minute since
     * cleanup of Kubernetes objects is a lengthy operation.
     *
     * Does nothing if the project doesn't exist.
     * @throws Exception if the deletion fails.
     */
    public fun deleteProject(id: ProjectId)

    /**
     * Retrieves the run logs of the main app pod (=the app itself). There may be additional pods (e.g. PostgreSQL)
     * but their logs are not returned.
     * @return main container runtime logs. May return an empty string if the container is not running or has not been created yet (e.g. first build is ongoing).
     * @throws NoSuchProjectException if the project doesn't exist.
     */
    public fun getRunLogs(id: ProjectId): String

    /**
     * Returns the current CPU/memory usage of the main app pod.
     * @return main container runtime metrics. May return [ResourcesUsage.zero] if the container is not running or has not been created yet (e.g. first build is ongoing).
     * @throws NoSuchProjectException if the project doesn't exist.
     */
    public fun getRunMetrics(id: ProjectId): ResourcesUsage

    /**
     * Retrieve the last 30 builds for given project [id].
     * @return the list of builds, sorted by [Build.number] ascending.
     * @throws NoSuchProjectException if the project doesn't exist.
     */
    public fun getLastBuilds(id: ProjectId): List<Build>

    /**
     * Retrieves the full build log (stdout).
     * @param buildNumber pass in [Build.number].
     * @throws NoSuchProjectException if the project doesn't exist.
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

    public override fun toString(): String = "Memory: $memoryMb MB; CPU: $cpu cores"
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
 * Runtime statistics, both of Shepherd and of the host machine.
 * @property concurrentJenkinsBuilders [Config.concurrentJenkinsBuilders]
 * @property projectCount number of projects registered to Shepherd
 * @property projectMemoryStats the project quota
 * @property hostMemoryStats the memory stats of the host machine
 * @property diskUsage the hard disk usage stats
 */
public data class Stats(
    val concurrentJenkinsBuilders: Int,
    val projectCount: Int,
    val projectMemoryStats: ProjectMemoryStats,
    val hostMemoryStats: HostMemoryUsageStats,
    val diskUsage: MemoryUsageStats
) {
    public companion object {
        public fun calculate(config: Config, projects: List<Project>): Stats {
            val projectQuota: ProjectMemoryStats = ProjectMemoryStats.calculateQuota(config, projects)
            val diskFreeSpaceMb = (File("/").freeSpace / 1000 / 1000).toInt()
            val diskTotalSpaceMb = (File("/").totalSpace / 1000 / 1000).toInt()
            val diskUsage = MemoryUsageStats(diskTotalSpaceMb - diskFreeSpaceMb, diskTotalSpaceMb)
            return Stats(
                config.concurrentJenkinsBuilders,
                projects.size,
                projectQuota,
                HostMemoryUsageStats.getHostStats(),
                diskUsage
            )
        }
    }
}

/**
 * The project quotas. Every project specifies how much memory it needs for building and for runtime;
 * Shepherd must make sure that this doesn't exceed the memory available on the host machine.
 * @property projectRuntimeQuota project runtime memory: sum of project runtime [Resources.memoryMb] vs how much memory is available for the project runtime.
 * @property totalQuota memory available both for project runtime and for project builds.
 * Calculated as a sum of all project runtime memory usage + top x build memory usage, where x is [Config.concurrentJenkinsBuilders].
 * [MemoryUsageStats.limitMb] equals to [Config.memoryQuotaMb]
 */
public data class ProjectMemoryStats(
    val projectRuntimeQuota: MemoryUsageStats,
    val totalQuota: MemoryUsageStats
) {
    public companion object {
        public fun calculateQuota(config: Config, projects: List<Project>): ProjectMemoryStats {
            val projectRuntimeMemoryMb = projects.sumOf { it.runtime.resources.memoryMb }
            val mostBuildMemIntensiveProjects = projects
                .sortedBy { it.build.resources.memoryMb }
                .takeLast(config.concurrentJenkinsBuilders)
            val projectBuildMemoryUsageMb =
                mostBuildMemIntensiveProjects.sumOf { it.build.resources.memoryMb }
            val totalUsageMb = projectRuntimeMemoryMb + projectBuildMemoryUsageMb
            val totalQuota = config.memoryQuotaMb
            val totalRuntimeQuota = totalQuota - projectBuildMemoryUsageMb
            return ProjectMemoryStats(MemoryUsageStats(projectRuntimeMemoryMb, totalRuntimeQuota), MemoryUsageStats(totalUsageMb, totalQuota))
        }
    }
}

/**
 * Host machine memory stats.
 * @property memory the physical memory stats
 * @property swap the swap file stats
 */
public data class HostMemoryUsageStats(
    val memory: MemoryUsageStats,
    val swap: MemoryUsageStats
) {
    public companion object {
        internal fun getHostStats(): HostMemoryUsageStats {
            //               total        used        free      shared  buff/cache   available
            //Mem:     32287645696  9973731328  7534346240   204955648 15446163456 22313914368
            //Swap:     2147479552   107233280  2040246272
            val result = exec("free", "-b").lines()
            val memline = result[1].splitByWhitespaces()
            val memory = MemoryUsageStats((memline[2].toLong() / 1000 / 1000).toInt(), (memline[1].toLong() / 1000 / 1000).toInt())
            val swapline = result[2].splitByWhitespaces()
            val swap = MemoryUsageStats((swapline[2].toLong() / 1000 / 1000).toInt(), (swapline[1].toLong() / 1000 / 1000).toInt())
            return HostMemoryUsageStats(memory, swap)
        }
    }
}

public data class MemoryUsageStats(
    val usageMb: Int,
    val limitMb: Int
) {
    override fun toString(): String = "$usageMb Mb of $limitMb Mb (${usageMb * 100 / limitMb}%)"
}

public class NoSuchProjectException(public val projectId: ProjectId, cause: Throwable? = null) : Exception("No such project: $projectId", cause)
