package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.drawer
import com.github.mvysny.karibudsl.v10.h3
import com.github.mvysny.karibudsl.v10.h4
import com.github.mvysny.karibudsl.v10.navbar
import com.github.mvysny.karibudsl.v10.onClick
import com.github.mvysny.karibudsl.v10.routerLink
import com.github.mvysny.karibudsl.v10.span
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.shepherd.web.security.UserLoginService
import com.github.mvysny.shepherd.web.security.getCurrentUser
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.html.H4
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.router.PageTitle

class MainLayout : AppLayout() {
    private lateinit var routeTitle: H4
    init {
        navbar {
            h3("Vaadin Shepherd")
            span("/")
            routeTitle = h4("")
        }
        drawer {
            verticalLayout(padding = false) {
                routerLink(VaadinIcon.LIST, "Project List", ProjectListRoute::class)
                if (getCurrentUser().isAdmin) {
                    routerLink(VaadinIcon.USER, "Users", UsersRoute::class)
                }
                button("Log out") {
                    onClick {
                        UserLoginService.get().logout()
                    }
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
