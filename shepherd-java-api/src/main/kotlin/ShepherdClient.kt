package com.github.mvysny.shepherd.api

public interface ShepherdClient {
    /**
     * Lists all registered projects.
     */
    public fun getAllProjects(): List<ProjectId>

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
     * Deletes given project: stops and removes all builds, stops and removes all Kubernetes rules.
     */
    public fun deleteProject(id: ProjectId)

    /**
     * Retrieves the run logs of the main Kubernetes pod (=the app itself). There may be additional pods (e.g. PostgreSQL)
     * but their logs are not returned.
     */
    public fun getRunLogs(id: ProjectId): String
}
