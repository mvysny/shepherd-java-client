package com.github.mvysny.shepherd.web

import com.github.mvysny.shepherd.api.ConfigFolder
import com.github.mvysny.shepherd.api.FakeShepherdClient
import com.github.mvysny.shepherd.api.KubernetesShepherdClient
import com.github.mvysny.shepherd.api.ShepherdClient
import com.github.mvysny.shepherd.web.security.UserRegistry
import java.io.Closeable
import java.nio.file.Path
import kotlin.io.path.div

var services: Services? = null

val ConfigFolder.userRegistryFolder: Path get() = rootFolder / "java" / "webadmin-users.json"

data class Services(
    val client: ShepherdClient,
    val userRegistry: UserRegistry
): Closeable {
    override fun close() {
        client.close()
    }

    companion object {
        fun fake(): Services {
            val client = FakeShepherdClient().withFakeProject()
            return Services(client, UserRegistry(client.configFolder.userRegistryFolder))
        }
        fun real(configFolder: ConfigFolder): Services {
            val client = KubernetesShepherdClient(projectConfigFolder = configFolder.projects)
            return Services(client, UserRegistry(configFolder.userRegistryFolder))
        }
        fun get(): Services = checkNotNull(services) { "App not initialized" }
    }
}