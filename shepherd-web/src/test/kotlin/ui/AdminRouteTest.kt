package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.kaributesting.v10._expectOne
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.web.AbstractAppTest
import com.vaadin.flow.router.AccessDeniedException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AdminRouteTest : AbstractAppTest() {
    @Test
    fun cantNavigateWithoutLoggingIn() {
        navigateTo<AdminRoute>()
        _expectOne<LoginRoute>()
    }

    @Test
    fun adminCanNavigate() {
        login()
        navigateTo<AdminRoute>()
        _expectOne<AdminRoute>()
    }

    @Test
    fun userCantNavigate() {
        loginUser()
        assertThrows<AccessDeniedException> { navigateTo<AdminRoute>() }
    }
}