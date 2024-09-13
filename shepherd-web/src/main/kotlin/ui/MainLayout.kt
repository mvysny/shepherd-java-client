package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.drawer
import com.github.mvysny.karibudsl.v10.drawerToggle
import com.github.mvysny.karibudsl.v10.h3
import com.github.mvysny.karibudsl.v10.h4
import com.github.mvysny.karibudsl.v10.navbar
import com.github.mvysny.karibudsl.v10.onClick
import com.github.mvysny.karibudsl.v10.span
import com.github.mvysny.karibudsl.v23.route
import com.github.mvysny.karibudsl.v23.sideNav
import com.github.mvysny.shepherd.web.security.UserLoginService
import com.github.mvysny.shepherd.web.security.getCurrentUser
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.H4
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.router.PageTitle

class MainLayout : AppLayout() {
    private lateinit var routeTitle: H4
    init {
        navbar {
            drawerToggle()
            h3("Vaadin Shepherd")
            span("/")
            routeTitle = h4("")
        }
        drawer {
            sideNav {
                route(ProjectListRoute::class, VaadinIcon.LIST, "Project List")
                if (getCurrentUser().isAdmin) {
                    route(UsersRoute::class, VaadinIcon.USER, "Users")
                }
            }
            button("Log out") {
                addThemeVariants(ButtonVariant.LUMO_SMALL)
                onClick {
                    UserLoginService.get().logout()
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
