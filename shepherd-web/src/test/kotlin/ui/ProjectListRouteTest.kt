package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.kaributesting.v10._expect
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.expectRow
import com.github.mvysny.kaributesting.v10.expectRows
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.api.ProjectView
import com.github.mvysny.shepherd.web.AbstractAppTest
import com.vaadin.flow.component.grid.Grid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProjectListRouteTest : AbstractAppTest() {
    @BeforeEach fun navigate() {
        login()
        navigateTo<ProjectListRoute>()
    }

    @Test fun smoke() {
        _expect<ProjectListRoute>()
        val grid = _get<Grid<ProjectView>>()
        grid.expectRows(2)
        grid.expectRow(0, "RouterLink[text='my-fake-project2']", "Martin Vysny <mavi@vaadin.com>", "Anchor[text='https://github.com/mvysny/vaadin-boot-example-gradle', href='https://github.com/mvysny/vaadin-boot-example-gradle']", "PublishedURLsAsVerticalLayout[@style='width:100%']", "BuildLinks[]", "Button[icon='vaadin:trash', @theme='small tertiary-inline icon']")
        grid.expectRow(1, "RouterLink[text='vaadin-boot-example-gradle']", "Martin Vysny <mavi@vaadin.com>", "Anchor[text='https://github.com/mvysny/vaadin-boot-example-gradle', href='https://github.com/mvysny/vaadin-boot-example-gradle']", "PublishedURLsAsVerticalLayout[@style='width:100%']", "BuildLinks[]", "Button[icon='vaadin:trash', @theme='small tertiary-inline icon']")
    }

    @Test fun userDoesntSeeAdminProjects() {
        loginUser()
        _expect<ProjectListRoute>()
        val grid = _get<Grid<ProjectView>>()
        grid.expectRows(0)
    }

    @Test fun userSeesProjectForWhichItIsAdditionalAdmin() {
        loginUser()
        _expect<ProjectListRoute>()
        val grid = _get<Grid<ProjectView>>()
        grid.expectRows(1)
        grid.expectRow(0, "RouterLink[text='my-fake-project2']", "Martin Vysny <mavi@vaadin.com>", "Anchor[text='https://github.com/mvysny/vaadin-boot-example-gradle', href='https://github.com/mvysny/vaadin-boot-example-gradle']", "PublishedURLsAsVerticalLayout[@style='width:100%']", "BuildLinks[]", "Button[icon='vaadin:trash', @theme='small tertiary-inline icon']")
    }
}
