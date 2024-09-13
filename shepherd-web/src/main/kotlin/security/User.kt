package com.github.mvysny.shepherd.web.security

import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.api.JsonUtils
import com.github.mvysny.shepherd.api.ProjectConfigFolder
import com.github.mvysny.shepherd.web.ui.ProjectListRoute
import com.github.mvysny.vaadinsimplesecurity.AbstractLoginService
import com.github.mvysny.vaadinsimplesecurity.HasPassword
import com.github.mvysny.vaadinsimplesecurity.SimpleUserWithRoles
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import kotlinx.serialization.Serializable
import org.hibernate.validator.constraints.Length
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
    @field:NotNull
    @field:NotEmpty
    @field:Email
    @field:Length(min = 1, max = 255)
    var email: String,
    @field:NotNull
    @field:NotEmpty
    @field:Length(min = 1, max = 255)
    var name: String,
    @field:NotNull
    var roles: Set<UserRoles>,
    @field:NotNull
    @field:NotEmpty
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

/**
 * Manages list of users. The list of users is persisted as a JSON file at `/etc/shepherd/java/webadmin-users.json`.
 */
object UserRegistry {
    @JvmStatic
    private val log = LoggerFactory.getLogger(UserRegistry::class.java)
    private val configFile = ProjectConfigFolder.LINUX_ROOT_FOLDER / "java" / "webadmin-users.json"
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

    init {
        users.addAll(loadUsers().users)
    }

    /**
     * Returns a deep copy of all users. Changes to the user objects will not be reflected to the actual internal user list.
     */
    fun getUsers(): List<User> = users.map { it.copy() }

    fun findByEmail(email: String): User? = users.find { it.email == email }

    /**
     * Deletes user with given [email] from the registry.
     */
    fun delete(email: String) {
        users.removeIf { it.email == email }
        save()
    }

    fun update(user: User) {
        users.removeIf { it.email == user.email }
        users.add(user.copy())
        save()
    }

    fun create(user: User) {
        require(findByEmail(user.email) == null) { "User ${user.email} already exists." }
        users.add(user.copy())
        save()
    }
}

/**
 * Session-scoped service which gives access to the currently logged-in user.
 */
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
