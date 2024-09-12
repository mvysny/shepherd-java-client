package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.web.AbstractAppTest
import com.github.mvysny.shepherd.web.Bootstrap
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
