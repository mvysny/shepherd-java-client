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
import com.github.mvysny.karibudsl.v10.textField
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.shepherd.web.security.User
import com.github.mvysny.shepherd.web.security.UserRegistry
import com.github.mvysny.shepherd.web.showErrorNotification
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

/**
 * Allows admins to manage users.
 */
@Route("users")
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
                componentColumn({ user -> Button("Edit", { edit(user) })})
                componentColumn({ user -> Button("Delete", { delete(user) })})
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
        // @todo mavi implement
        showErrorNotification("Not implemented")
    }

    private fun edit(user: User) {
        // @todo mavi implement
        throw UnsupportedOperationException("Not implemented")
    }

    private fun delete(user: User) {
        UserRegistry.delete(user.email)
        refresh()
    }
}

class UserForm : KFormLayout() {
    val binder = beanValidationBinder<User>()
    init {
        emailField("E-mail") {
            bind(binder).bind(User::email)
        }
        emailField("E-mail") {
            bind(binder).bind(User::email)
        }
    }
}