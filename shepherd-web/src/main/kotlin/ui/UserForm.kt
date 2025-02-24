package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.karibudsl.v10.KFormLayout
import com.github.mvysny.karibudsl.v10.beanValidationBinder
import com.github.mvysny.karibudsl.v10.bind
import com.github.mvysny.karibudsl.v10.emailField
import com.github.mvysny.karibudsl.v10.passwordField
import com.github.mvysny.karibudsl.v10.textField
import com.github.mvysny.karibudsl.v10.trimmingConverter
import com.github.mvysny.karibudsl.v23.multiSelectComboBox
import com.github.mvysny.shepherd.web.security.User
import com.github.mvysny.shepherd.web.security.UserRoles
import com.github.mvysny.shepherd.web.ui.components.Form
import com.github.mvysny.shepherd.web.ui.components.showErrorNotification
import com.vaadin.flow.component.textfield.PasswordField

class UserForm(val isCreating: Boolean) : KFormLayout(), Form<User> {
    override val binder = beanValidationBinder<User>()
    private val password: PasswordField
    init {
        emailField("E-mail") {
            isEnabled = isCreating
            bind(binder).trimmingConverter().bind(User::email)
        }
        textField("Name") {
            placeholder = "Full name, e.g. Martin Vysny"
            bind(binder).trimmingConverter().bind(User::name)
        }
        multiSelectComboBox<UserRoles>("Roles") {
            setItems(UserRoles.entries)
            bind(binder).bind(User::roles)
        }
        password = passwordField("Password") {
            if (!isCreating) {
                placeholder = "Leave empty to not to change the password"
            }
        }
    }

    override fun writeIfValid(toBean: User): Boolean {
        if (password.value.isNotBlank()) {
            toBean.setPassword(password.value.trim())
        }
        return super.writeIfValid(toBean)
    }
}