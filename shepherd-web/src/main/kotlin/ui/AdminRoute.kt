package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.onClick
import com.github.mvysny.karibudsl.v10.span
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.shepherd.web.Bootstrap
import com.github.mvysny.shepherd.web.ui.components.shepherdStatsTable
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

/**
 * Allows admins to manage users.
 */
@Route("admin", layout = MainLayout::class)
@RolesAllowed("ADMIN")
@PageTitle("Admin")
class AdminRoute : KComposite() {
    private lateinit var status: Span

    private val layout = ui {
        verticalLayout {
            setSizeFull()

            status = span("")

            shepherdStatsTable()

            button("Shut Down") {
                onClick {
                    Bootstrap.getClient().builder.initiateShutdown()
                    refresh()
                }
            }
        }
    }

    init {
        refresh()
    }

    private fun refresh() {
        val builder = Bootstrap.getClient().builder
        if (builder.isShuttingDown()) {
            val building = builder.getCurrentlyBeingBuilt()
            if (building.isEmpty()) {
                status.text =
                    "Shepherd is shut down, you can now update the host machine. Note that updating Jenkins can abort the shutdown mode and resume Shepherd"
            } else {
                status.text =
                    "Shepherd shutting down; waiting for build queue to empty: $building"
            }
        } else {
            status.text = "Working normally"
        }
    }
}