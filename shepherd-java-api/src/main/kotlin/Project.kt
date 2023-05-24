package com.github.mvysny.shepherd.api

import kotlinx.serialization.Serializable

/**
 * The Project ID must:
 *
 * * contain at most 54 characters
 * * contain only lowercase alphanumeric characters or '-'
 * * start with an alphanumeric character
 * * end with an alphanumeric character
 */
@JvmInline
@Serializable
public value class ProjectId(public val id: String) {
    init {
        require(idValidator.matches(id)) { "The ID must contain at most 54 characters, it must contain only lowercase alphanumeric characters or '-', it must start and end with an alphanumeric character" }
    }
    private companion object {
        private val idValidator = "[a-z0-9][a-z0-9\\-]{0,52}[a-z0-9]".toRegex()
    }
}

/**
 * Information about the project owner, how to reach him in case the project needs to be modified.
 * Jenkins may send notification emails about the failed builds to [email].
 * @property name e.g. `Martin Vysny`
 * @property email e.g. `mavi@vaadin.com`
 */
@Serializable
public data class ProjectOwner(
    val name: String,
    val email: String
) {
    override fun toString(): String = "$name <$email>"
}

/**
 * Resources the app needs.
 * @property memoryMb max memory in megabytes
 * @property cpu max CPU cores to use. 1 means 1 CPU core to be used.
 */
@Serializable
public data class Resources(
    val memoryMb: Int,
    val cpu: Float
) {
    init {
        require(memoryMb >= 64) { "Give the process at least 64mb: $memoryMb" }
        require(cpu > 0) { "$cpu" }
    }
    public companion object {
        @JvmStatic
        public val defaultRuntimeResources: Resources = Resources(memoryMb = 256, cpu = 1f)
        public val defaultBuildResources: Resources = Resources(memoryMb = 2048, cpu = 2f)
    }
}

/**
 * How to build the project.
 * @property resources how many resources to allocate for the build.
 */
@Serializable
public data class Build(
    val resources: Resources
)

/**
 * @property id the project ID, must be unique.
 * @property description any additional vital information about the project
 * @property gitRepo the git repository from where the project comes from, e.g. `https://github.com/mvysny/vaadin-boot-example-gradle`
 * @property owner the project owner
 * @property runtimeResources what resources the project needs for running
 * @property build build info
 * @property additionalServices any additional services the project needs, e.g. additional databases and such.
 */
@Serializable
public data class Project(
    val id: ProjectId,
    val description: String,
    val gitRepo: String,
    val owner: ProjectOwner,
    val runtimeResources: Resources,
    val build: Build,
    val additionalServices: List<Service> = listOf()
) {
    /**
     * Returns URLs on which this project runs (can be browsed to). E.g. for `vaadin-boot-example-gradle`
     * on the `v-herd.eu` [host], this returns `https://v-herd.eu/vaadin-boot-example-gradle`.
     */
    public fun getPublishedURLs(host: String): List<String> =
        listOf("https://$host/${id.id}")
}

@Serializable
public enum class ServiceType {
    /**
     * A PostgreSQL database.
     */
    Postgres
}

@Serializable
public data class Service(
    val type: ServiceType
)
