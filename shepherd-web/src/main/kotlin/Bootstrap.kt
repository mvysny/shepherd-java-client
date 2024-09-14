package com.github.mvysny.shepherd.web

import com.github.mvysny.shepherd.api.KubernetesShepherdClient
import com.github.mvysny.shepherd.api.ShepherdClient
import com.github.mvysny.shepherd.web.security.UserLoginService
import com.github.mvysny.shepherd.web.ui.LoginRoute
import com.github.mvysny.vaadinsimplesecurity.SimpleNavigationAccessControl
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
    // Handles authorization - pulls in the currently-logged-in user from given service and checks
    // whether the user can access given route.
    //
    // InMemoryLoginService remembers the currently logged-in user in the Vaadin Session; it
    // retrieves the users from the InMemoryUserRegistry.
    private val accessControl = SimpleNavigationAccessControl.usingService(UserLoginService::get);

    init {
        accessControl.setLoginView(LoginRoute::class.java)
    }

    override fun serviceInit(event: ServiceInitEvent) {
        event.source.addSessionInitListener { initSession(it.session) }
        event.source.addUIInitListener { it.ui.addBeforeEnterListener(accessControl) }
    }

    private fun initSession(session: VaadinSession) {
        session.setErrorHandler {
            log.error("Internal error", it.throwable)
            if (UI.getCurrent() != null) {
                val n = Notification.show(
                    "We're sorry, an internal error occurred: ${it.throwable}",
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
