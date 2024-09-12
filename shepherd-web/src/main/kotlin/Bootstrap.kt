package com.github.mvysny.shepherd.web

import com.github.mvysny.shepherd.api.KubernetesShepherdClient
import com.github.mvysny.shepherd.api.ShepherdClient
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.server.PWA
import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.VaadinServiceInitListener
import com.vaadin.flow.server.VaadinSession
import com.vaadin.flow.theme.Theme
import jakarta.servlet.ServletContextEvent
import jakarta.servlet.ServletContextListener
import jakarta.servlet.annotation.WebListener
import org.slf4j.LoggerFactory

val host = "v-herd.eu"

@WebListener
class Bootstrap : ServletContextListener {
    companion object {
        @JvmField
        var client: ShepherdClient? = null
        fun getClient(): ShepherdClient = checkNotNull(client) { "shepherd client is not initialized" }
    }
    override fun contextInitialized(sce: ServletContextEvent?) {
        if (client == null) {
            client = KubernetesShepherdClient()
        }
    }

    override fun contextDestroyed(sce: ServletContextEvent?) {
        client?.close()
        client = null
    }
}

@PWA(name = "Project Base for Vaadin", shortName = "Project Base")
@Theme("my-theme")
class AppShell : AppShellConfigurator

/**
 * Configures Vaadin. Registered via the Java Service Loader API.
 */
class MyServiceInitListener : VaadinServiceInitListener {
    override fun serviceInit(event: ServiceInitEvent) {
        event.source.addSessionInitListener { initSession(it.session) }
    }

    private fun initSession(session: VaadinSession) {
        session.setErrorHandler {
            log.error("Internal error", it.throwable)
            if (UI.getCurrent() != null) {
                val n = Notification.show(
                    "We're sorry, an internal error occurred",
                    3000,
                    Notification.Position.TOP_CENTER
                )
                n.addThemeVariants(NotificationVariant.LUMO_ERROR)
                n.open()
            }
        }
    }

    companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(MyServiceInitListener::class.java)
    }
}
