package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._expect
import com.github.mvysny.kaributesting.v10._expectDisabled
import com.github.mvysny.kaributesting.v10._expectEnabled
import com.github.mvysny.kaributesting.v10._expectNone
import com.github.mvysny.kaributesting.v10._expectValid
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._setValue
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.web.AbstractAppTest
import com.github.mvysny.shepherd.web.Bootstrap
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.expect

class EditProjectRouteTest : AbstractAppTest() {
    private val fakeProject: Project = Bootstrap.getClient().getAllProjects(null)[0].project

    @BeforeEach fun navigate() {
        login()
        navigateTo(EditProjectRoute::class, fakeProject.id.id)
    }

    @Test fun smoke() {
        _expect<EditProjectRoute>()
    }

    @Test fun cantChangeStuffWhenEditingProject() {
        _get<TextField> { id = "projectid" } ._expectDisabled()
        _get<TextField> { id = "gitRepoURL" } ._expectDisabled()
        _get<TextField> { id = "gitRepoBranch" } ._expectEnabled()
        _get<TextField> { id = "gitRepoCredentialsID" } ._expectEnabled()
    }

    @Test fun changeGitBranchAndCreds() {
        _get<TextField> { id = "gitRepoBranch" } ._setValue("foobar")
        val cid = UUID.randomUUID().toString()
        _get<TextField> { id = "gitRepoCredentialsID" } ._setValue(cid)

        _get<ProjectForm>().binder._expectValid()
        _get<Button> { text = "Save & Apply" } ._click()
        _expectNone<Dialog>()
        val project = Bootstrap.getClient().getAllProjects(null)[0].project
        expect("foobar") { project.gitRepo.branch }
        expect(cid) { project.gitRepo.credentialsID }
    }

    @Test fun createProjectAllFieldsEnabled() {
        navigateTo<ProjectListRoute>()
        _get<Button> { text = "Create New Project" } ._click()
        _get<TextField> { id = "projectid" } ._expectEnabled()
        _get<TextField> { id = "gitRepoURL" } ._expectEnabled()
        _get<TextField> { id = "gitRepoBranch" } ._expectEnabled()
        _get<TextField> { id = "gitRepoCredentialsID" } ._expectEnabled()
    }

    @Test fun createProject() {
        navigateTo<ProjectListRoute>()
        _get<Button> { text = "Create New Project" } ._click()
        _get<TextField> { id = "projectid" } ._setValue("my-project")
        _get<TextField> { id = "description" } ._setValue("A cool project")
        _get<TextField> { id = "gitRepoURL" } ._setValue("git@github.com:mvysny/shepherd-java-client.git")
        _get<TextField> { id = "gitRepoBranch" } ._setValue("main")

        _get<ProjectForm>().binder._expectValid()
        _get<Button> { text = "Save" } ._click()
        _expectNone<Dialog>()

        val allProjects = Bootstrap.getClient().getAllProjects(null)
        expect(true) {
            allProjects.any { it.project.id.id == "my-project" }
        }
    }
}