package com.github.mvysny.shepherd.web.security

import com.github.mvysny.vaadinsimplesecurity.HasPassword
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import kotlinx.serialization.Serializable
import org.hibernate.validator.constraints.Length

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
    private var hashedPassword: String?,
): java.io.Serializable, HasPassword {
    val isAdmin: Boolean get() = roles.contains(UserRoles.ADMIN)
    override fun getHashedPassword(): String? = hashedPassword
    override fun setHashedPassword(hashedPassword: String?) {
        this.hashedPassword = hashedPassword
    }
}

@Serializable
data class Users(val users: List<User>)
