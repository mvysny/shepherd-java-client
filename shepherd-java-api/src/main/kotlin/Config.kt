@file:OptIn(ExperimentalSerializationApi::class)

package com.github.mvysny.shepherd.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.inputStream

/**
 * @property memoryQuotaMb The max memory for Shepherd Kubernetes to run VM stuff - projects and their builds.
 * That's the machine memory minus Jenkins usage (by default 512mb) minus Kubernetes itself (say 1000mb),
 * possibly minus 500mb for the future shepherd-ui project, minus OS usage (say 200mb)
 * @property concurrentJenkinsBuilders Number of concurrent job runners in Jenkins. Defaults to 2,
 * can be configured in Jenkins as `# of executors` at `http://localhost:8080/manage/configure`.
 * @property maxProjectRuntimeResources how much memory+cpu a project can ask for its runtime.
 * @property maxProjectBuildResources how much memory+cpu a project can ask for its build.
 */
@Serializable
public data class Config(
    val memoryQuotaMb: Int,
    val concurrentJenkinsBuilders: Int,
    val maxProjectRuntimeResources: Resources,
    val maxProjectBuildResources: Resources,
    val jenkins: JenkinsConfig = JenkinsConfig()
) {
    public companion object {
        /**
         * Location of the config file on the filesystem.
         */
        public var location: Path = Path("/etc/shepherd/java/config.json")

        /**
         * Loads the current config file from the filesystem (from [location]).
         */
        public fun load(): Config = location.inputStream().buffered().use { Json.decodeFromStream(it) }
    }
}

@Serializable
public data class JenkinsConfig(
    val url: String = "http://localhost:8080",
    val username: String = "admin",
    val password: String = "admin"
)
