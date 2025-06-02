package com.github.mvysny.shepherd.api.containers

import com.github.mvysny.shepherd.api.ClientFeatures
import com.github.mvysny.shepherd.api.Config
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.api.ResourcesUsage

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
     * The project exists, but the main container may not be running or not even created yet (e.g. if no Jenkins build passed yet).
     */
    public fun deleteProject(id: ProjectId)

    /**
     * Updates runtime system files and objects to the new project configuration.
     *
     * The project exists, but the main container may not be running. However,
     * the main container has been created (was running at least once previously).
     * @param newProject the updated information about the project.
     * @param oldProject the previous project data. The [Project.id] can not change.
     * @return true if the project needs to be restarted (via [restartProject]).
     */
    public fun updateProjectConfig(newProject: Project, oldProject: Project): Boolean

    /**
     * Checks whether the main project container is running or not.
     *
     * The project exists, but the main container may not be running or not even created yet (e.g. if no Jenkins build passed yet).
     * @return true if the main project container is running, false if it's stopped or hasn't been created yet.
     */
    public fun isProjectRunning(id: ProjectId): Boolean

    /**
     * Restarts the main container of given project [project]. If the service set changed, stops obsolete containers and
     * starts new ones. If the main container is already running, kills the old one and starts a new main container.
     *
     * May be called when the main container is not running yet or does not even exist yet - in such case the container
     * should simply be started.
     */
    public fun restartProject(project: Project)

    /**
     * Retrieves the run logs of the main app pod (=the app itself). There may be additional pods (e.g. PostgreSQL)
     * but their logs are not returned.
     *
     * The project exists, but the main container may not be running or not even created yet (e.g. if no Jenkins build passed yet).
     * If it's not running, you may return an empty string. If the container doesn't exist yet, you may return an empty string, or you may fail with an exception.
     */
    public fun getRunLogs(id: ProjectId): String

    /**
     * Returns the current CPU/memory usage of the main app pod.
     *
     * The project exists, but it may not be running (e.g. if no Jenkins build passed yet).
     * If it's not running, return [ResourcesUsage.zero]. If the container doesn't exist yet, you may return [ResourcesUsage.zero], or you may fail with an exception.
     */
    public fun getRunMetrics(id: ProjectId): ResourcesUsage

    /**
     * Returns the URL of the main domain (e.g. `https://v-herd.eu/pid` or `http://pid.v-herd.eu`) where the project is deployed.
     */
    public fun getMainDomainDeployURL(id: ProjectId): String

    /**
     * Returns the supported client features.
     */
    public val features: ClientFeatures
}