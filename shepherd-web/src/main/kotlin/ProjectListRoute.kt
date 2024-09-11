package com.github.mvysny.shepherd.web

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.column
import com.github.mvysny.karibudsl.v10.componentColumn
import com.github.mvysny.karibudsl.v10.grid
import com.github.mvysny.karibudsl.v10.h1
import com.github.mvysny.karibudsl.v10.horizontalLayout
import com.github.mvysny.karibudsl.v10.isExpand
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.api.ProjectView
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.router.Route

@Route("")
class ProjectListRoute : KComposite() {
    private val layout = ui {
        verticalLayout {
            setSizeFull()

            h1("Welcome!")

            val projects: List<ProjectView> = Bootstrap.getClient().getAllProjects()
            grid<ProjectView> {
                isExpand = true
                setItems(projects)

                column({ p -> p.project.id.id }) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Project ID")
                }
                column({ p -> p.project.owner }) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Owner")
                }
                column({ p -> p.project.description }) {
                    isExpand = true; isResizable = true
                    setHeader("Description")
                }
                componentColumn({ p -> val web = p.project.resolveWebpage(); Anchor(web, web) }) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Home Page")
                }
                componentColumn({ p -> p.project.getPublishedURLsInVerticalLayout() }) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Deployed At")
                }
                column({ p -> p.lastBuild?.let { "#${it.number}: ${it.outcome}" }}) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Last Build")
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
