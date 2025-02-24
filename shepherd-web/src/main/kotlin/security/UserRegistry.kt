package com.github.mvysny.shepherd.web.security

import com.github.mvysny.shepherd.api.JsonUtils
import com.github.mvysny.shepherd.web.Services
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

/**
 * Manages list of users. The list of users is persisted as a JSON file at `/etc/shepherd/java/webadmin-users.json`.
 */
class UserRegistry(val configFile: Path) {
    companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(UserRegistry::class.java)
        fun get() = Services.get().userRegistry
    }
    private fun loadUsers(): Users {
        var users: Users
        if (configFile.exists()) {
            users = JsonUtils.fromFile(configFile)
        } else {
            log.warn("$configFile missing")
            users = Users(listOf())
        }
        if (users.users.isEmpty()) {
            log.warn("No users defined, creating a default admin mavi@vaadin.com with password 'admin'")
            val admin = User("mavi@vaadin.com", "Martin Vysny", setOf(UserRoles.USER, UserRoles.ADMIN), "")
            admin.setPassword("admin")
            users = Users(listOf(admin))
            log.warn("Created admin mavi@vaadin.com with password 'admin'. Please change the password as soon as possible.")
        }
        return users
    }

    private fun init() {
        users.addAll(loadUsers().users)
    }

    fun deleteAll() {
        configFile.deleteIfExists()
        users.clear()
        init()
    }

    private fun save() {
        JsonUtils.saveToFile(Users(users.toList()), configFile, false)
    }

    private val users = CopyOnWriteArraySet<User>()

    init {
        init()
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
        if (users.removeIf { it.email == email }) {
            save()
        }
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