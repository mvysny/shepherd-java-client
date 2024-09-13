package com.github.mvysny.shepherd.web.security

import com.github.mvysny.shepherd.api.JsonUtils
import com.github.mvysny.shepherd.api.ProjectConfigFolder
import com.github.mvysny.vaadinsimplesecurity.AbstractLoginService
import com.github.mvysny.vaadinsimplesecurity.HasPassword
import com.github.mvysny.vaadinsimplesecurity.SimpleUserWithRoles
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArraySet
import javax.security.auth.login.FailedLoginException
import kotlin.io.path.div
import kotlin.io.path.exists

enum class UserRoles {
    USER, ADMIN
}

@Serializable
data class User(
    var email: String,
    var name: String,
    var roles: Set<UserRoles>,
    private var hashedPassword: String,
): java.io.Serializable, HasPassword {
    val isAdmin: Boolean get() = roles.contains(UserRoles.ADMIN)
    override fun getHashedPassword(): String = hashedPassword
    override fun setHashedPassword(hashedPassword: String) {
        this.hashedPassword = hashedPassword
    }
}

@Serializable
data class Users(val users: List<User>)

object UserRegistry {
    @JvmStatic
    private val log = LoggerFactory.getLogger(UserRegistry::class.java)
    private val configFile = ProjectConfigFolder.LINUX_ROOT_FOLDER / "java" / "webadmin.json"
    private fun loadUsers(): Users {
        users.clear()
        if (!configFile.exists()) {
            log.warn("$configFile missing, creating a default one.")
            val admin = User("mavi@vaadin.com", "Martin Vysny", setOf(UserRoles.USER, UserRoles.ADMIN), "")
            admin.setPassword("admin")
            log.warn("Created admin mavi@vaadin.com with password 'admin'. Please change the password as soon as possible.")
            val users = Users(listOf(admin))
            try {
                JsonUtils.saveToFile(users, configFile, false)
            } catch (e: Exception) {
                log.error("Failed to save initial user set", e)
            }
            return users
        } else {
            return JsonUtils.fromFile(configFile)
        }
    }

    private fun save() {
        JsonUtils.saveToFile(Users(users.toList()), configFile, false)
    }

    private val users = CopyOnWriteArraySet<User>()

    fun findByEmail(email: String): User? = users.find { it.email == email }
}

class UserLoginService : AbstractLoginService<User>() {
    override fun toUserWithRoles(user: User): SimpleUserWithRoles =
        SimpleUserWithRoles(user.email, user.roles.map { it.name } .toSet())

    /**
     * Logs in user with given username and password. Fails with {@link LoginException}
     * on failure.
     */
    fun login(email: String, password: String) {
        val user = UserRegistry.findByEmail(email) ?: throw FailedLoginException("Invalid email or password")
        if (!user.passwordMatches(password)) {
            throw FailedLoginException("Invalid username or password")
        }
        login(user)
    }

    companion object {
        fun get(): UserLoginService = get(UserLoginService::class.java) { UserLoginService() }
    }
}

fun getCurrentUser(): User? = UserLoginService.get().currentUser
