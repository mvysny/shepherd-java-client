package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.VaadinDsl
import com.github.mvysny.karibudsl.v10.div
import com.github.mvysny.karibudsl.v10.init
import com.github.mvysny.karibudsl.v10.strong
import com.github.mvysny.kaributools.HtmlSpan
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.web.Bootstrap
import com.github.mvysny.shepherd.web.ui.PublishedURLsAsVerticalLayout
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import org.intellij.lang.annotations.Language

/**
 * Shows a simple key-value table pair.
 */
open class SimpleKeyValueTable(columns: Int = 1) : KComposite() {
    private val div = ui {
        div()
    }
    init {
        setWidthFull()

        div.element.style.set("font-size", "90%")
        div.element.style.set("display", "grid")
        div.element.style.set("gap", "0px 10px")
        div.element.style.set("grid-template-columns", "repeat($columns, auto 1fr)")
    }

    fun addRow(header: String, body: String) {
        addRow(header, Span(body))
    }
    fun addHtmlRow(header: String, @Language("html") body: String) {
        addRow(header, HtmlSpan(body))
    }
    fun addRow(header: String, body: Component) {
        div.strong(header)
        div.add(body)
    }
    fun removeAll() {
        div.removeAll()
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).simpleKeyValueTable(columns: Int = 1, block: (@VaadinDsl SimpleKeyValueTable).() -> Unit = {}) = init(SimpleKeyValueTable(columns), block)

@VaadinDsl
fun (@VaadinDsl HasComponents).shepherdStatsTable() {
    val stats = Bootstrap.getClient().getStats()
    simpleKeyValueTable {
        addRow("Project Count", stats.projectCount.toString())
        addRow("Project Runtime Quota", stats.projectMemoryStats.projectRuntimeQuota.toString())
        addRow("Host OS: Memory", stats.hostMemoryStats.memory.toString())
        addRow("Host OS: Swap", stats.hostMemoryStats.swap.toString())
        addRow("Host OS: Disk Space", stats.diskUsage.toString())
    }
}

class ProjectQuickDetailsTable(project: Project? = null) : SimpleKeyValueTable(2) {
    init {
        if (project != null) {
            showProject(project)
        }
    }

    fun showProject(project: Project) {
        removeAll()
        addRow("Project ID", project.id.id)
        addRow("Description", project.description)
        addRow("Home Page", project.resolveWebpage())
        addRow("Git", "${project.gitRepo.url} (${project.gitRepo.branch})")
        addRow("Owner", project.owner.toString())
        addRow("Runtime Resources", project.runtime.resources.toString())
        addRow("Build Resources", project.build.resources.toString())
        addRow("Published at", PublishedURLsAsVerticalLayout(project))
        addRow("Services", project.additionalServices.joinToString { it.type.toString() })
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).projectQuickDetailsTable(project: Project? = null) = init(ProjectQuickDetailsTable(project))
