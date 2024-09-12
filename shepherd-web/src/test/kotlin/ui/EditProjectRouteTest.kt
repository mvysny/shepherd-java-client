package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.kaributesting.v10._expect
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.web.AbstractAppTest
import com.github.mvysny.shepherd.web.Bootstrap
import com.github.mvysny.shepherd.web.ui.EditProjectRoute
import org.junit.jupiter.api.Test

class EditProjectRouteTest : AbstractAppTest() {
    private val fakeProject: Project = Bootstrap.getClient().getAllProjects(null)[0].project

    @Test fun smoke() {
        navigateTo(EditProjectRoute::class, fakeProject.id.id)
        _expect<EditProjectRoute>()
    }
}