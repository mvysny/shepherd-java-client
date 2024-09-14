package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.karibudsl.v10.drawer
import com.github.mvysny.karibudsl.v10.drawerToggle
import com.github.mvysny.karibudsl.v10.h3
import com.github.mvysny.karibudsl.v10.isExpand
import com.github.mvysny.karibudsl.v10.item
import com.github.mvysny.karibudsl.v10.menuBar
import com.github.mvysny.karibudsl.v10.navbar
import com.github.mvysny.karibudsl.v10.span
import com.github.mvysny.karibudsl.v23.route
import com.github.mvysny.karibudsl.v23.sideNav
import com.github.mvysny.shepherd.web.host
import com.github.mvysny.shepherd.web.security.UserLoginService
import com.github.mvysny.shepherd.web.security.getCurrentUser
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.router.PageTitle

class MainLayout : AppLayout() {
    private lateinit var routeTitle: H3
    init {
        navbar {
            drawerToggle()
            h3("Vaadin Shepherd $host")
            span("/")
            routeTitle = h3("")
            // spacer, to push right content to the right.
            span { isExpand = true }
            // rightmost content: shows the username and allows the user to log out.
            menuBar {
                item(getCurrentUser().name) {
                    item("Log out", { it -> UserLoginService.get().logout() })
                }
            }
        }
        drawer {
            sideNav {
                route(ProjectListRoute::class, VaadinIcon.LIST, "Project List")
                if (getCurrentUser().isAdmin) {
                    route(UsersRoute::class, VaadinIcon.USER, "Users")
                }
            }
        }
    }

    override fun afterNavigation() {
        super.afterNavigation()
        val pageTitle = content.javaClass.getAnnotation<PageTitle>(PageTitle::class.java)?.value ?: ""
        routeTitle.text = pageTitle
    }
}
