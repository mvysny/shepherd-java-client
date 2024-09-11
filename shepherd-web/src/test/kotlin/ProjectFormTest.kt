package com.github.mvysny.shepherd.web

import com.github.mvysny.shepherd.api.Project
import org.junit.jupiter.api.Test

class ProjectFormTest : AbstractAppTest() {
    private val fakeProject: Project = Bootstrap.getClient().getAllProjects(null)[0].project

    @Test fun smoke() {
        val form = ProjectForm(false)
        form.binder.bean = fakeProject.toMutable()
        form.disableFieldsForRegularUser()
        form.binder.bean = fakeProject.toMutable()
    }
}
