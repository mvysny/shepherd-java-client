package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.kaributesting.v10._expect
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.web.AbstractAppTest
import com.github.mvysny.shepherd.web.Bootstrap
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProjectBuildsRouteTest : AbstractAppTest() {
    private val fakeProject: Project = Bootstrap.getClient().getAllProjects(null)[0].project

    @BeforeEach fun navigate() {
        login()
        navigateTo(ProjectBuildsRoute::class, fakeProject.id.id)
    }

    @Test fun smoke() {
        _expect<ProjectBuildsRoute>()
    }
}