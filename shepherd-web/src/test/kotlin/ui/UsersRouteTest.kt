package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.kaributesting.v10._expectOne
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.web.AbstractAppTest
import org.junit.jupiter.api.Test

class UsersRouteTest : AbstractAppTest() {
    @Test fun cantNavigateWithoutLoggingIn() {
        navigateTo<UsersRoute>()
        _expectOne<LoginRoute>()
    }

    @Test fun adminCanNavigate() {
        login()
        navigateTo<UsersRoute>()
        _expectOne<UsersRoute>()
    }
}