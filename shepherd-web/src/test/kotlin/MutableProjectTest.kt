package com.github.mvysny.shepherd.web

import com.github.mvysny.shepherd.api.Project
import kotlin.test.Test
import kotlin.test.expect

class MutableProjectTest : AbstractAppTest() {
    private val fakeProject: Project = Bootstrap.getClient().getAllProjects(null).get(0).project

    @Test fun smoke() {
        fakeProject.toMutable()
    }

    @Test fun allInformationCopiedCorrectly() {
        expect(fakeProject) { fakeProject.toMutable().toProject() }
    }
}
