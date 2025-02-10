package com.github.mvysny.shepherd.web.security

import com.github.mvysny.shepherd.web.AbstractAppTest
import org.junit.jupiter.api.Test
import kotlin.test.expect

class UserLoginServiceTest : AbstractAppTest() {
    @Test fun testGoogleSSOLogin() {
        UserLoginService.get().loginViaGoogleSSO("foo@bar.baz", "John Doe")
        expect(true) { UserLoginService.get().isLoggedIn }
    }
}