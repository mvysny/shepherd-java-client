package com.github.mvysny.shepherd.web.security

import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.web.ui.ProjectListRoute
import com.github.mvysny.vaadinsimplesecurity.AbstractLoginService
import com.github.mvysny.vaadinsimplesecurity.SimpleUserWithRoles
import javax.security.auth.login.FailedLoginException

/**
 * Session-scoped service which gives access to the currently logged-in user.
 */
class UserLoginService : AbstractLoginService<User>() {
    override fun toUserWithRoles(user: User): SimpleUserWithRoles =
        SimpleUserWithRoles(user.email, user.roles.map { it.name }.toSet())

    /**
     * Logs in user with given username and password. Fails with {@link LoginException}
     * on failure.
     */
    fun login(email: String, password: String) {
        val user = UserRegistry.findByEmail(email) ?: throw FailedLoginException(
            "Invalid email or password"
        )
        if (!user.passwordMatches(password)) {
            throw FailedLoginException("Invalid username or password")
        }
        login(user)
        navigateTo<ProjectListRoute>()
    }

    companion object {
        fun get(): UserLoginService = get(UserLoginService::class.java) { UserLoginService() }
    }
}

/**
 * Helper function to get the currently logged-in user. Fails if no-one is logged in.
 */
fun getCurrentUser(): User = checkNotNull(UserLoginService.get().currentUser) { "No user logged in" }
