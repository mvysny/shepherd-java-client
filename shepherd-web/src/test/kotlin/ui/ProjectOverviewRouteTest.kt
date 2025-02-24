package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.kaributesting.v10._expect
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.web.AbstractAppTest
import com.github.mvysny.shepherd.web.Bootstrap
import com.vaadin.flow.router.NotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.expect

class ProjectOverviewRouteTest : AbstractAppTest() {
    private val adminsProject: Project = Bootstrap.getClient().getAllProjects(null)
        .first { it.project.id.id == "vaadin-boot-example-gradle" }
        .project
    private val projectSharedWithUser: Project = Bootstrap.getClient().getAllProjects(null)
        .first { it.project.id.id == "my-fake-project2" }
        .project

    @BeforeEach fun navigate() {
        login()
        navigateTo(ProjectOverviewRoute::class, adminsProject.id.id)
    }

    @Test fun smoke() {
        _expect<ProjectOverviewRoute>()
    }

    @Test fun userCantSeeAdminsProject() {
        loginUser()
        assertThrows<NotFoundException> {
            navigateTo(ProjectOverviewRoute::class, adminsProject.id.id)
        }
    }

    @Test fun userCanSeeHisProject() {
        loginUser()
        navigateTo(ProjectOverviewRoute::class, projectSharedWithUser.id.id)
    }

    @Test fun adminCanSeeUserProject() {
        navigateTo(ProjectOverviewRoute::class, projectSharedWithUser.id.id)
    }

    @Test fun navigateToNonExistingProject() {
        assertThrows<NotFoundException> {
            navigateTo(ProjectOverviewRoute::class, "non-existing-project")
        }
    }

    @Test fun navigateToURLWithInvalidProjectID() {
        expect(false) { ProjectId.isValid("---invalid") }
        assertThrows<NotFoundException> {
            navigateTo(ProjectOverviewRoute::class, "---invalid")
        }
    }
}