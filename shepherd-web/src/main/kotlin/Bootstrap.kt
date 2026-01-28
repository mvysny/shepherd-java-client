package com.github.mvysny.shepherd.web

import com.github.mvysny.shepherd.api.LocalFS
import com.github.mvysny.shepherd.api.ShepherdClient
import com.github.mvysny.shepherd.web.security.UserLoginService
import com.github.mvysny.shepherd.web.ui.LoginRoute
import com.github.mvysny.vaadinsimplesecurity.SimpleNavigationAccessControl
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.dependency.StyleSheet
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.server.PWA
import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.VaadinServiceInitListener
import com.vaadin.flow.server.VaadinSession
import com.vaadin.flow.theme.Theme
import com.vaadin.flow.theme.lumo.Lumo
import jakarta.servlet.ServletContextEvent
import jakarta.servlet.ServletContextListener
import jakarta.servlet.annotation.WebListener
import org.slf4j.LoggerFactory

lateinit var host: String

@WebListener
class Bootstrap : ServletContextListener {
    companion object {
        fun getClient(): ShepherdClient = Services.get().client
    }
    override fun contextInitialized(sce: ServletContextEvent?) {
        if (!Services.initialized) {
            Services.newReal(LocalFS())
        }
        host = getClient().getConfig().hostDNS
    }

    override fun contextDestroyed(sce: ServletContextEvent?) {
        Services.destroy()
    }
}

@PWA(name = "Project Base for Vaadin", shortName = "Project Base")
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("styles.css")
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
    private val accessControl = SimpleNavigationAccessControl.usingService(UserLoginService::get)

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
