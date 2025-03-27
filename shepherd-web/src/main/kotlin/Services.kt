package com.github.mvysny.shepherd.web

import com.github.mvysny.shepherd.api.ConfigFolder
import com.github.mvysny.shepherd.api.FakeShepherdClient
import com.github.mvysny.shepherd.api.KubernetesShepherdClient
import com.github.mvysny.shepherd.api.LocalFS
import com.github.mvysny.shepherd.api.ShepherdClient
import com.github.mvysny.shepherd.web.security.UserRegistry
import java.io.Closeable
import java.nio.file.Path
import kotlin.io.path.div

data class Services(
    val client: ShepherdClient,
    val userRegistry: UserRegistry
): Closeable {
    override fun close() {
        client.close()
    }

    companion object {
        private var services: Services? = null
        private val ConfigFolder.userRegistryFolder: Path get() = rootFolder / "java" / "webadmin-users.json"
        fun newFake() {
            destroy()
            val client = FakeShepherdClient().withFakeProject()
            services = Services(client, UserRegistry(client.configFolder.userRegistryFolder))
        }
        fun newReal(fs: LocalFS) {
            destroy()
            val client = KubernetesShepherdClient(fs)
            services = Services(client, UserRegistry(fs.configFolder.userRegistryFolder))
        }
        val initialized: Boolean get() = services != null
        fun get(): Services = checkNotNull(services) { "App not initialized" }
        fun destroy() {
            services?.close()
            services = null
        }
    }
}