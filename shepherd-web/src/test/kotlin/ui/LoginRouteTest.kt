package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.kaributesting.v10._expectOne
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._login
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.web.AbstractAppTest
import com.vaadin.flow.component.login.LoginForm
import org.junit.jupiter.api.Test
import kotlin.test.expect

class LoginRouteTest : AbstractAppTest() {
    @Test fun projectListRouteProtected() {
        navigateTo<ProjectListRoute>()
        _expectOne<LoginRoute>()
    }

    @Test fun unsuccessfulLogin() {
        _get<LoginForm>()._login("mavi@vaadin.com", "nbusr123")
        _expectOne<LoginRoute>()
        expect(true) { _get<LoginForm>().isError }
    }

    @Test fun successfulLogin() {
        _get<LoginForm>()._login("mavi@vaadin.com", "admin")
        _expectOne<ProjectListRoute>()
    }
}