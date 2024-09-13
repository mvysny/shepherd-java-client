package com.github.mvysny.shepherd.web.security

enum class UserRoles {
    USER, ADMIN
}
data class User(val email: String, val name: String, val roles: Set<UserRoles>) {
    val isAdmin: Boolean get() = roles.contains(UserRoles.ADMIN)
}

// @todo mavi security
fun getCurrentUser(): User? = User("mavi@vaadin.com", "Martin Vysny", setOf(UserRoles.USER, UserRoles.ADMIN))
