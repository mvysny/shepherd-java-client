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
 * @property maxAvailableMemoryMb The max memory for Shepherd Kubernetes to run VM stuff - projects and their builds.
 * That's the machine memory minus Jenkins usage (by default 512mb) minus Kubernetes itself (say 1000mb),
 * possibly minus 500mb for the future shepherd-ui project, minus OS usage (say 200mb)
 * @property concurrentJenkinsBuilders Number of concurrent job runners in Jenkins. Defaults to 2,
 * can be configured in Jenkins as `# of executors` at `http://localhost:8080/manage/configure`.
 *
 */
@Serializable
public data class Config(
    val maxAvailableMemoryMb: Int,
    val concurrentJenkinsBuilders: Int,
) {
    public companion object {
        /**
         * Location of the config file on the filesystem.
         */
        public val location: Path = Path("/etc/shepherd/java/config.json")

        /**
         * Loads the current config file from the filesystem (from [location]).
         */
        public fun load(): Config = location.inputStream().buffered().use { Json.decodeFromStream(it) }
    }
}
