package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.VaadinDsl
import com.github.mvysny.karibudsl.v10.anchor
import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.column
import com.github.mvysny.karibudsl.v10.componentColumn
import com.github.mvysny.karibudsl.v10.grid
import com.github.mvysny.karibudsl.v10.horizontalLayout
import com.github.mvysny.karibudsl.v10.isExpand
import com.github.mvysny.karibudsl.v10.onClick
import com.github.mvysny.karibudsl.v10.routerLink
import com.github.mvysny.karibudsl.v10.text
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.api.ProjectView
import com.github.mvysny.shepherd.web.Bootstrap
import com.github.mvysny.shepherd.web.Services
import com.github.mvysny.shepherd.web.host
import com.github.mvysny.shepherd.web.security.getCurrentUser
import com.github.mvysny.shepherd.web.ui.components.FormDialog
import com.github.mvysny.shepherd.web.ui.components.ProjectQuickDetailsTable
import com.github.mvysny.shepherd.web.ui.components.confirmDialog
import com.github.mvysny.shepherd.web.ui.components.iconButtonColumn
import com.github.mvysny.shepherd.web.ui.components.shepherdStatsTable
import com.github.mvysny.shepherd.web.ui.components.showInfoNotification
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.icon.VaadinIcon
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

            shepherdStatsTable()

            horizontalLayout {
                button("Create New Project") {
                    onClick { createNewProject() }
                }
            }

            val user = getCurrentUser()
            val ownerEmail = if (user.isAdmin) null else user.email
            val projects: List<ProjectView> = Bootstrap.getClient().getAllProjects(ownerEmail)
            grid<ProjectView> {
                isExpand = true
                setItems(projects)

                componentColumn({ p ->
                    val pid = p.project.id.id
                    routerLink(text = pid, viewType = ProjectOverviewRoute::class, parameter = pid)
                }) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Project ID")
                }
                column({ p -> p.project.owner }) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Owner")
                }
                componentColumn({ p -> val web = p.project.resolveWebpage(); anchor(web, web) }) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Home Page")
                }
                componentColumn({ p -> publishedURLsAsVerticalLayout(p.project) }) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Deployed At")
                }
                componentColumn({ p -> buildLinks(p) }) {
                    isExpand = false; isAutoWidth = true; isResizable = true
                    setHeader("Builds")
                }
                iconButtonColumn(VaadinIcon.REFRESH, "Restart main project docker container") {
                    Bootstrap.getClient().restartContainers(it.project.id)
                    showInfoNotification("Project ${it.project.id.id} restarted successfully")
                }
                iconButtonColumn(VaadinIcon.TRASH, "Delete project") {
                    deleteProject(it.project.id)
                }

                setItemDetailsRenderer(ComponentRenderer { p -> ProjectQuickDetailsTable(p.project) })
            }
        }
    }

    private fun createNewProject() {
        val project = MutableProject.newEmpty(getCurrentUser())
        val form = ProjectForm(true)
        FormDialog(form, project) {
            Bootstrap.getClient().createProject(project.toProject(Services.get().client))
            UI.getCurrent().page.reload()
        } .open()
    }
}

@VaadinDsl
private fun (@VaadinDsl HasComponents).buildLinks(project: ProjectView): HorizontalLayout {
    return horizontalLayout {
        isSpacing = false
        routerLink(text = "Builds") {
            setRoute(ProjectBuildsRoute::class.java, project.project.id.id)
        }
        val lastBuild = project.lastBuild
        if (lastBuild != null) {
            text(" (")
            anchor(Downloads.buildLog(project.project.id, lastBuild), "#${lastBuild.number}: ${lastBuild.outcome}")
            text(")")
        }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).publishedURLsAsVerticalLayout(project: Project) : VerticalLayout {
    return verticalLayout {
        isPadding = false
        isSpacing = false
        project.getPublishedURLs(Bootstrap.getClient()).map { it -> Anchor(it, it) } .forEach { add(it) }
    }
}

fun deleteProject(id: ProjectId) {
    confirmDialog("Delete project ${id.id}? This action can not be reverted! Please wait patiently - this action can take up to 1 minute.") {
        Bootstrap.getClient().deleteProject(id)
        showInfoNotification("Project ${id.id} deleted successfully")
        navigateTo<ProjectListRoute>()
    }
}
