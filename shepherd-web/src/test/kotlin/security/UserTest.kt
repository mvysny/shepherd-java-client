package com.github.mvysny.shepherd.web.security

import com.github.mvysny.shepherd.api.JsonUtils
import org.junit.jupiter.api.Test
import kotlin.test.expect

class UserTest {
    @Test fun jsonSerialization() {
        val user = User("foo", "bar", setOf(UserRoles.ADMIN, UserRoles.USER), "foobar")
        expect("""{"email":"foo","name":"bar","roles":["ADMIN","USER"],"hashedPassword":"foobar"}""") { JsonUtils.toJson(user, false) }
    }
    @Test fun jsonDeserialization() {
        val user: User = JsonUtils.fromJson("""{"email":"foo","name":"bar","roles":["ADMIN","USER"],"hashedPassword":"foobar"}""")
        expect(User("foo", "bar", setOf(UserRoles.ADMIN, UserRoles.USER), "foobar")) { user }
    }
    @Test fun passwordNotStoredPlaintext() {
        val user = User("foo", "bar", setOf(UserRoles.ADMIN, UserRoles.USER), "")
        user.setPassword("foobar")
        expect(false) { JsonUtils.toJson(user, false).contains("foobar") }
    }
}
