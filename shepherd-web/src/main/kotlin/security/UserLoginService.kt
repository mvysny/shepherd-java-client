package com.github.mvysny.shepherd.web.security

import com.github.mvysny.vaadinsimplesecurity.AbstractLoginService
import com.github.mvysny.vaadinsimplesecurity.SimpleUserWithRoles
import javax.security.auth.login.FailedLoginException
import kotlin.random.Random

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
        val user = UserRegistry.get().findByEmail(email) ?: throw FailedLoginException(
            "Invalid email or password"
        )
        if (!user.passwordMatches(password)) {
            throw FailedLoginException("Invalid username or password")
        }
        login(user)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun loginViaGoogleSSO(email: String, name: String) {
        var user = UserRegistry.get().findByEmail(email)
        if (user == null) {
            user = User(email, name, setOf(UserRoles.USER), null)
            UserRegistry.get().create(user)
        }
        login(user)
    }

    companion object {
        fun get(): UserLoginService = get(UserLoginService::class.java) { UserLoginService() }
    }
}

/**
 * Helper function to get the currently logged-in user. Fails if no-one is logged in.
 */
fun getCurrentUser(): User = checkNotNull(UserLoginService.get().currentUser) { "No user logged in" }
