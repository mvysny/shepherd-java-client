package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.columnFor
import com.github.mvysny.karibudsl.v10.componentColumn
import com.github.mvysny.karibudsl.v10.grid
import com.github.mvysny.karibudsl.v10.h2
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.shepherd.api.Build
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.api.ProjectId
import com.github.mvysny.shepherd.web.Bootstrap
import com.github.mvysny.shepherd.web.security.checkProjectId
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.InputStreamFactory
import com.vaadin.flow.server.StreamResource
import jakarta.annotation.security.PermitAll

@Route("project/builds", layout = MainLayout::class)
@PageTitle("Builds")
@PermitAll
class ProjectBuildsRoute : KComposite(), HasUrlParameter<String> {
    private lateinit var project: Project
    private lateinit var captionComponent: H2
    private lateinit var buildsGrid: Grid<Build>
    private val layout = ui {
        verticalLayout {
            setSizeFull()

            captionComponent = h2()
            buildsGrid = grid {
                columnFor(Build::number)
                columnFor(Build::outcome)
                columnFor(Build::buildStarted)
                columnFor(Build::duration)
                componentColumn({ build -> Anchor(BuildLogStreamResource(project.id, build), "Log") }) {
                    setHeader("Build Log")
                }
            }
        }
    }
    override fun setParameter(event: BeforeEvent, parameter: String) {
        project = checkProjectId(parameter)
        captionComponent.text = "${project.id.id}: Builds"
        val builds = Bootstrap.getClient().getLastBuilds(project.id)
        buildsGrid.setItems(builds)
    }
}

class BuildLogStreamResource(val id: ProjectId, val build: Build) : StreamResource("${id.id}-buildlog-${build.number}.txt",
    InputStreamFactory { Bootstrap.getClient().getBuildLog(id, build.number).byteInputStream() }
)
