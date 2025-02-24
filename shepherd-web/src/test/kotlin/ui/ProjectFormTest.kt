package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.kaributesting.v10._expectValid
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._setValue
import com.github.mvysny.shepherd.api.GitRepo
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.web.AbstractAppTest
import com.github.mvysny.shepherd.web.Bootstrap
import com.github.mvysny.shepherd.web.security.getCurrentUser
import com.github.mvysny.shepherd.web.ui.components.SimpleStringSetField
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Test
import kotlin.test.expect

class ProjectFormTest : AbstractAppTest() {
    private val fakeProject: Project = Bootstrap.getClient().getAllProjects(null)[0].project

    @Test fun smoke() {
        login()
        var form = ProjectForm(false)
        form.binder.readBean(fakeProject.toMutable())
        form.binder.readBean(fakeProject.toMutable())
        form = ProjectForm(true)
        form.binder.readBean(fakeProject.toMutable())
        form.binder.readBean(fakeProject.toMutable())
    }

    @Test fun createProjectFillAllFields() {
        login()
        var form = ProjectForm(true)
        val project = MutableProject.newEmpty(getCurrentUser())
        form.binder.readBean(project)
        UI.getCurrent().add(form)
        _get<TextField> { id = "projectid" }._setValue("my-project-2")
        _get<TextField> { id = "description" } ._setValue("A cool project")
        _get<TextField> { id = "gitRepoURL" } ._setValue("git@github.com:mvysny/shepherd-java-client.git")
        _get<TextField> { id = "gitRepoBranch" } ._setValue("main")
        _get<SimpleStringSetField> { id = "additionalAdmins" } ._setValue(setOf("foo", "bar", "baz"))

        form.binder._expectValid()
        expect(true) { form.writeIfValid(project) }

        val myproject = project.toProject()
        expect("my-project-2") { myproject.id.id }
        expect("A cool project") { myproject.description }
        expect(GitRepo("git@github.com:mvysny/shepherd-java-client.git", "main")) { myproject.gitRepo }
        expect(setOf("mavi@vaadin.com", "foo", "bar", "baz")) { myproject.allAdmins }
    }
}
