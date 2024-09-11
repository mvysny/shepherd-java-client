package com.github.mvysny.shepherd.web

import com.github.mvysny.kaributesting.v10._expect
import com.github.mvysny.kaributools.navigateTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProjectListRouteTest : AbstractAppTest() {
    @BeforeEach fun navigate() {
        navigateTo<ProjectListRoute>()
    }

    @Test fun smoke() {
        _expect<ProjectListRoute>()
    }
}
