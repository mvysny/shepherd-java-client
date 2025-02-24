package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.columnFor
import com.github.mvysny.karibudsl.v10.grid
import com.github.mvysny.karibudsl.v10.onClick
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.shepherd.web.security.User
import com.github.mvysny.shepherd.web.security.UserRegistry
import com.github.mvysny.shepherd.web.security.UserRoles
import com.github.mvysny.shepherd.web.ui.components.FormDialog
import com.github.mvysny.shepherd.web.ui.components.confirmDialog
import com.github.mvysny.shepherd.web.ui.components.iconButtonColumn
import com.github.mvysny.shepherd.web.ui.components.showInfoNotification
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.icon.VaadinIcon
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
        userGrid.setItems(UserRegistry.get().getUsers())
    }

    private fun createUser() {
        FormDialog(UserForm(true), User("", "", setOf(UserRoles.USER))) { user ->
            UserRegistry.get().create(user)
            refresh()
        } .open()
    }

    private fun edit(user: User) {
        FormDialog(UserForm(false), user) {
            UserRegistry.get().update(user)
            refresh()
        } .open()
    }

    private fun delete(user: User) {
        confirmDialog("Delete user ${user.email}? This action can not be reverted, but you can re-create the user easily. The user won't be able to log in but his projects will remain.") {
            UserRegistry.get().delete(user.email)
            refresh()
            showInfoNotification("User ${user.email} deleted successfully")
        }
    }
}

