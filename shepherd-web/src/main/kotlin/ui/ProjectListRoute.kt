package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.anchor
import com.github.mvysny.karibudsl.v10.column
import com.github.mvysny.karibudsl.v10.componentColumn
import com.github.mvysny.karibudsl.v10.grid
import com.github.mvysny.karibudsl.v10.isExpand
import com.github.mvysny.karibudsl.v10.routerLink
import com.github.mvysny.karibudsl.v10.text
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.api.ProjectView
import com.github.mvysny.shepherd.web.Bootstrap
import com.github.mvysny.shepherd.web.host
import com.github.mvysny.shepherd.web.security.getCurrentUser
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLink
import jakarta.annotation.security.PermitAll

@Route("", layout = MainLayout::class)
@PageTitle("Projects")
@PermitAll
class ProjectListRoute : KComposite() {
    private val layout = ui {
        verticalLayout {
            setSizeFull()

            val user = getCurrentUser()!!
            val ownerEmail = if (user.isAdmin) null else user.email
            val projects: List<ProjectView> = Bootstrap.getClient().getAllProjects()
            grid<ProjectView> {
                isExpand = true
                setItems(projects)

                componentColumn({ p ->
                    val pid = p.project.id.id
                    RouterLink(pid, EditProjectRoute::class.java, pid)
                }) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Project ID")
                }
                column({ p -> p.project.owner }) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Owner")
                }
                componentColumn({ p -> val web = p.project.resolveWebpage(); Anchor(web, web) }) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Home Page")
                }
                componentColumn({ p -> p.project.getPublishedURLsInVerticalLayout() }) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Deployed At")
                }
                componentColumn({ p -> BuildLinks(p) }) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Builds")
                }

                setItemDetailsRenderer(ComponentRenderer { p -> Div(p.project.toString()) })
            }
        }
    }

    private fun Project.getPublishedURLsInVerticalLayout(): Component {
        val l = VerticalLayout()
        l.isPadding = false
        getPublishedURLs(host).map { it -> Anchor(it, it) } .forEach { l.add(it) }
        return l
    }
}

private class BuildLinks(val project: ProjectView) : HorizontalLayout() {
    init {
        isSpacing = false
        routerLink(text = "Builds") {
            setRoute(ProjectBuildsRoute::class.java, project.project.id.id)
        }
        val lastBuild = project.lastBuild
        if (lastBuild != null) {
            text(" (")
            anchor(BuildLogStreamResource(project.project.id, lastBuild), "#${lastBuild.number}: ${lastBuild.outcome}")
            text(")")
        }
    }
}
