package com.github.mvysny.shepherd.web.security

import com.github.mvysny.shepherd.web.AbstractAppTest
import org.junit.jupiter.api.Test
import kotlin.test.expect

class UserRegistryTest : AbstractAppTest() {
    @Test fun createPasswordlessUserWorks() {
        val user = User("foo@bar.baz", "John Doe", setOf(UserRoles.USER))
        UserRegistry.get().create(user)
        UserRegistry.get().delete("mavi@vaadin.com")
        UserRegistry.get().delete("user@vaadin.com")
        expect(listOf(user)) { UserRegistry.get().getUsers() }
        expect(user) { UserRegistry.get().findByEmail("foo@bar.baz") }
    }
}
