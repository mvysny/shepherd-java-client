package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.KFormLayout
import com.github.mvysny.karibudsl.v10.beanValidationBinder
import com.github.mvysny.karibudsl.v10.bind
import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.columnFor
import com.github.mvysny.karibudsl.v10.componentColumn
import com.github.mvysny.karibudsl.v10.emailField
import com.github.mvysny.karibudsl.v10.grid
import com.github.mvysny.karibudsl.v10.onClick
import com.github.mvysny.karibudsl.v10.passwordField
import com.github.mvysny.karibudsl.v10.textField
import com.github.mvysny.karibudsl.v10.trimmingConverter
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.karibudsl.v23.multiSelectComboBox
import com.github.mvysny.shepherd.web.security.User
import com.github.mvysny.shepherd.web.security.UserRegistry
import com.github.mvysny.shepherd.web.security.UserRoles
import com.github.mvysny.shepherd.web.ui.components.Form
import com.github.mvysny.shepherd.web.ui.components.FormDialog
import com.github.mvysny.shepherd.web.ui.components.confirmDialog
import com.github.mvysny.shepherd.web.ui.components.iconButtonColumn
import com.github.mvysny.shepherd.web.ui.components.showErrorNotification
import com.github.mvysny.shepherd.web.ui.components.showInfoNotification
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

/**
 * Allows admins to manage users.
 */
@Route("users", layout = MainLayout::class)
@RolesAllowed("ADMIN")
@PageTitle("Users")
class UsersRoute : KComposite() {
    private lateinit var userGrid: Grid<User>

    private val layout = ui {
        verticalLayout {
            setSizeFull()

            button("Create User") {
                onClick { createUser() }
            }
            userGrid = grid {
                columnFor(User::email)
                columnFor(User::name)
                columnFor(User::roles)
                iconButtonColumn(VaadinIcon.EDIT) { edit(it) }
                iconButtonColumn(VaadinIcon.TRASH) { delete(it) }
            }
        }
    }

    init {
        refresh()
    }

    private fun refresh() {
        userGrid.setItems(UserRegistry.getUsers())
    }

    private fun createUser() {
        FormDialog(UserForm(true), User("", "", setOf(UserRoles.USER), "")) { user ->
            UserRegistry.create(user)
            refresh()
        } .open()
    }

    private fun edit(user: User) {
        FormDialog(UserForm(false), user) {
            UserRegistry.update(user)
            refresh()
        } .open()
    }

    private fun delete(user: User) {
        confirmDialog("Delete user ${user.email}? This action can not be reverted, but you can re-create the user easily. The user won't be able to log in but his projects will remain.") {
            UserRegistry.delete(user.email)
            refresh()
            showInfoNotification("User ${user.email} deleted successfully")
        }
    }
}

private class UserForm(val isCreating: Boolean) : KFormLayout(), Form<User> {
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
        if (password.value.isBlank() && isCreating) {
            password.isInvalid = true
            password.errorMessage = "Please provide a password"
            showErrorNotification("Please provide a password")
            return false
        }
        password.isInvalid = false
        if (password.value.isNotBlank()) {
            toBean.setPassword(password.value.trim())
        }
        return super.writeIfValid(toBean)
    }
}
