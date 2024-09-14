package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.horizontalLayout
import com.github.mvysny.karibudsl.v10.onClick
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.kaributools.navigateTo
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.web.security.checkProjectId
import com.github.mvysny.shepherd.web.ui.components.ProjectQuickDetailsTable
import com.github.mvysny.shepherd.web.ui.components.projectQuickDetailsTable
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route("project/overview", layout = MainLayout::class)
@PageTitle("Project Overview")
@PermitAll
class ProjectOverviewRoute : KComposite(), HasUrlParameter<String> {
    private lateinit var quickDetails: ProjectQuickDetailsTable
    private lateinit var project: Project

    private val layout = ui {
        verticalLayout {
            setSizeFull()

            quickDetails = projectQuickDetailsTable()

            horizontalLayout {
                button("Edit Project") {
                    onClick {
                        navigateTo(EditProjectRoute::class, project.id.id)
                    }
                }
                button("Last Builds") {
                    onClick {
                        navigateTo(ProjectBuildsRoute::class, project.id.id)
                    }
                }
                // @todo mavi runtime stats, runtime logs
            }
        }
    }

    override fun setParameter(event: BeforeEvent, parameter: String) {
        project = checkProjectId(parameter)
        quickDetails.showProject(project)
    }
}
