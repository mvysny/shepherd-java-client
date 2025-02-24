package com.github.mvysny.shepherd.web

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.Routes
import com.github.mvysny.kaributesting.v10._expectOne
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.api.BuildSpec
import com.github.mvysny.shepherd.api.GitRepo
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.api.ProjectOwner
import com.github.mvysny.shepherd.api.ProjectRuntime
import com.github.mvysny.shepherd.api.Resources
import com.github.mvysny.shepherd.web.security.User
import com.github.mvysny.shepherd.web.security.UserLoginService
import com.github.mvysny.shepherd.web.security.UserRegistry
import com.github.mvysny.shepherd.web.security.UserRoles
import com.github.mvysny.shepherd.web.ui.LoginRoute
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

        private val fakeProject2 = Project(
            id = ProjectId("my-fake-project2"),
            description = "Gradle example for Vaadin Boot",
            gitRepo = GitRepo("https://github.com/mvysny/vaadin-boot-example-gradle", "master"),
            owner = ProjectOwner("Martin Vysny", "mavi@vaadin.com"),
            runtime = ProjectRuntime(Resources.defaultRuntimeResources),
            build = BuildSpec(resources = Resources.defaultBuildResources),
            additionalAdmins = setOf("user@vaadin.com"),
        )

        @BeforeAll @JvmStatic fun bootstrap() {
            Services.newFake()
            Services.get().client.createProject(fakeProject2)
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

    fun logout() {
        UserLoginService.get().logout()
        _expectOne<LoginRoute>()
    }

    /**
     * Logs in user `user@vaadin.com`. The user is not an admin.
     */
    fun loginUser() {
        logout()
        if (UserRegistry.get().findByEmail("user@vaadin.com") == null) {
            val user = User("user@vaadin.com", "User", setOf(UserRoles.USER))
            user.setPassword("user")
            UserRegistry.get().create(user)
        }
        UserLoginService.get().login("user@vaadin.com", "user")
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
