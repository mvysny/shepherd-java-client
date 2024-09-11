package com.github.mvysny.shepherd.web

import com.github.mvysny.karibudsl.v10.drawer
import com.github.mvysny.karibudsl.v10.h3
import com.github.mvysny.karibudsl.v10.h4
import com.github.mvysny.karibudsl.v10.navbar
import com.github.mvysny.karibudsl.v10.routerLink
import com.github.mvysny.karibudsl.v10.span
import com.github.mvysny.karibudsl.v10.verticalLayout
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
            }
        }
    }

    override fun afterNavigation() {
        super.afterNavigation()
        val pageTitle = content.javaClass.getAnnotation<PageTitle>(PageTitle::class.java)?.value ?: ""
        routeTitle.text = pageTitle
    }
}
