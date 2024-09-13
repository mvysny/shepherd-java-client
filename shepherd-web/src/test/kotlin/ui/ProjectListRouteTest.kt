package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.kaributesting.v10._expect
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.web.AbstractAppTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProjectListRouteTest : AbstractAppTest() {
    @BeforeEach fun navigate() {
        login()
        navigateTo<ProjectListRoute>()
    }

    @Test fun smoke() {
        _expect<ProjectListRoute>()
    }
}
