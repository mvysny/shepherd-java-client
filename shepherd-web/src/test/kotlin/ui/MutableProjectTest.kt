package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.web.AbstractAppTest
import com.github.mvysny.shepherd.web.Bootstrap
import com.github.mvysny.shepherd.web.ui.toMutable
import kotlin.test.Test
import kotlin.test.expect

class MutableProjectTest : AbstractAppTest() {
    private val fakeProject: Project = Bootstrap.getClient().getAllProjects(null)[0].project

    @Test fun smoke() {
        fakeProject.toMutable()
    }

    @Test fun allInformationCopiedCorrectly() {
        expect(fakeProject) { fakeProject.toMutable().toProject() }
    }
}
