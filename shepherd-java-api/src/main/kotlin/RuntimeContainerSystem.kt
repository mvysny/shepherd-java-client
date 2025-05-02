package com.github.mvysny.shepherd.api

/**
 * Runtime container system which is capable of running project docker containers.
 * There are two implementations: Kubernetes and pure Docker.
 */
public interface RuntimeContainerSystem {
    /**
     * Creates the project in the underlying runtime env: prepares all necessary files
     * and resources. Note that the project will be compiled by Jenkins first, and started
     * later on.
     */
    public fun createProject(project: Project)

    /**
     * Kills all project's running containers and deletes all files & resources
     * used by the project. Does nothing if the project containers aren't running and
     * there are no resources to clean up.
     *
     * The project exists, but it may not be running (e.g. if no Jenkins build passed yet).
     */
    public fun deleteProject(id: ProjectId)

    /**
     * Updates runtime system files and objects to the new project configuration.
     *
     * The project exists, but it may not be running (e.g. if no Jenkins build passed yet).
     * @return true if the project needs to be restarted (via [restartProject]).
     */
    public fun updateProjectConfig(project: Project): Boolean

    /**
     * Checks whether the main project container is running or not.
     *
     * The project exists, but it may not be running (e.g. if no Jenkins build passed yet).
     * @return true if the main project container is running, false if not.
     */
    public fun isProjectRunning(id: ProjectId): Boolean

    /**
     * Restarts project [id]. Only called if the project runtime container is running at the moment.
     */
    public fun restartProject(id: ProjectId)

    /**
     * Retrieves the run logs of the main app pod (=the app itself). There may be additional pods (e.g. PostgreSQL)
     * but their logs are not returned.
     *
     * The project exists, but it may not be running (e.g. if no Jenkins build passed yet).
     * If it's not running, returns an empty string.
     */
    public fun getRunLogs(id: ProjectId): String

    /**
     * Returns the current CPU/memory usage of the main app pod.
     *
     * The project exists, but it may not be running (e.g. if no Jenkins build passed yet).
     * If it's not running, return [ResourcesUsage.zero].
     */
    public fun getRunMetrics(id: ProjectId): ResourcesUsage
}