package com.github.mvysny.shepherd.api

import com.offbytwo.jenkins.model.BuildResult
import java.io.Closeable
import java.time.Instant

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
     * * creates a config file for it on the filesystem;
     * * creates a Kubernetes config file for the project
     * * registers the project to Jenkins and starts first Jenkins build. The build is configured to call the `shepherd-build` script.
     * Fails if the project json config file already exists.
     */
    public fun createProject(project: Project)

    /**
     * Deletes given project: stops and removes all builds, stops and removes all Kubernetes rules,
     * and removes the project json config.
     *
     * This function will still try to unregister the project from Jenkins and Kubernetes
     * even if the project json config is already nonexistent.
     */
    public fun deleteProject(id: ProjectId)

    /**
     * Retrieves the run logs of the main Kubernetes pod (=the app itself). There may be additional pods (e.g. PostgreSQL)
     * but their logs are not returned.
     */
    public fun getRunLogs(id: ProjectId): String
}

public data class ProjectView(
    val project: Project,
    val lastBuildOutcome: BuildResult,
    val lastBuildTimestamp: Instant
) {
    /**
     * Returns URLs on which this project runs (can be browsed to). E.g. for `vaadin-boot-example-gradle`
     * on the `v-herd.eu` [host], this returns `https://v-herd.eu/vaadin-boot-example-gradle`.
     */
    public fun getPublishedURLs(host: String): List<String> = project.getPublishedURLs(host)
}
