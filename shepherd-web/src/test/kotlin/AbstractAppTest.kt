package com.github.mvysny.shepherd.web

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.Routes
import com.github.mvysny.kaributesting.v10._expectOne
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.api.FakeShepherdClient
import com.github.mvysny.shepherd.web.security.UserLoginService
import com.github.mvysny.shepherd.web.security.UserRegistry
import com.github.mvysny.shepherd.web.ui.ProjectListRoute
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

abstract class AbstractAppTest {
    companion object {
        private lateinit var routes: Routes
        @BeforeAll @JvmStatic fun discoverRoutes() {
            // Route discovery involves classpath scanning and is an expensive operation.
            // Running the discovery process only once per test run speeds up the test runtime considerably.
            // Discover the routes once and cache the result.
            routes = Routes().autoDiscoverViews("com.github.mvysny.shepherd.web")
        }

        @BeforeAll @JvmStatic fun bootstrap() {
            services = Services.fake()
            UserRegistry.get().deleteAll()
            Bootstrap().contextInitialized(null)
        }
        @AfterAll @JvmStatic fun shutdown() {
            Bootstrap().contextDestroyed(null)
        }
    }

    fun login() {
        UserLoginService.get().login("mavi@vaadin.com", "admin")
        navigateTo<ProjectListRoute>()
        _expectOne<ProjectListRoute>()
    }

    @BeforeEach fun setupVaadin() {
        // MockVaadin.setup() registers all @Routes, prepares the Vaadin instances for us
        // (the UI, the VaadinSession, VaadinRequest, VaadinResponse, ...) and navigates to the root route.
        MockVaadin.setup(routes)
    }
    @AfterEach fun teardownVaadin() { MockVaadin.tearDown() }
}
