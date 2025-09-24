package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.VaadinDsl
import com.github.mvysny.karibudsl.v10.anchor
import com.github.mvysny.karibudsl.v10.buildSingleComponent
import com.github.mvysny.karibudsl.v10.div
import com.github.mvysny.karibudsl.v10.init
import com.github.mvysny.karibudsl.v10.strong
import com.github.mvysny.kaributools.HtmlSpan
import com.github.mvysny.shepherd.api.Project
import com.github.mvysny.shepherd.web.Bootstrap
import com.github.mvysny.shepherd.web.ui.publishedURLsAsVerticalLayout
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import org.intellij.lang.annotations.Language

/**
 * Shows a simple key-value table pair.
 */
open class SimpleKeyValueTable : KComposite() {
    private val div = ui {
        div()
    }
    init {
        setWidthFull()

        div.element.style.set("font-size", "90%")
        div.element.style.set("display", "grid")
        div.element.style.set("gap", "0px 10px")
        div.element.style.set("grid-template-columns", "repeat(2, auto 1fr)")
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
    @VaadinDsl
    fun row(header: String, dsl: (@VaadinDsl HasComponents).() -> Unit) {
        addRow(header, buildSingleComponent(block = dsl))
    }
    fun removeAll() {
        div.removeAll()
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).simpleKeyValueTable(block: (@VaadinDsl SimpleKeyValueTable).() -> Unit = {}) = init(SimpleKeyValueTable(), block)

@VaadinDsl
fun (@VaadinDsl HasComponents).shepherdStatsTable() {
    val stats = Bootstrap.getClient().getStats()
    simpleKeyValueTable {
        addRow("Project Count", stats.projectCount.toString())
        addRow("Project Runtime Quota", stats.projectMemoryStats.projectRuntimeQuota.toString())
        addRow("Host OS: Memory", stats.hostMemoryStats.memory.toString())
        addRow("Host OS: Swap", stats.hostMemoryStats.swap.toString())
        addRow("Host OS: Disk Space", stats.diskUsage.toString())
        addRow("Backend System", Bootstrap.getClient().description)
        addRow("Builder: Max # of concurrent builds", stats.concurrentJenkinsBuilders.toString())
        val builder = Bootstrap.getClient().builder
        addRow("Builder: Stats", buildString {
            if (builder.isShuttingDown()) append("SHUTTING DOWN; ")
            append("Building: ${builder.getCurrentlyBeingBuilt().size}; Build Queue: ${builder.getQueue().size}")
        })
    }
}

class ProjectQuickDetailsTable(project: Project? = null) : SimpleKeyValueTable() {
    init {
        if (project != null) {
            showProject(project)
        }
    }

    fun showProject(project: Project) {
        removeAll()
        addRow("Project ID", project.id.id)
        addRow("Description", project.description)
        val homepage = project.resolveWebpage()
        addRow("Home Page", Anchor(homepage, homepage))
        addRow("Git", "${project.gitRepo.url} (${project.gitRepo.branch})")
        addRow("Owner", project.owner.toString())
        addRow("Runtime Max Resources", project.runtime.resources.toString())
        addRow("Build Max Resources", project.build.resources.toString())
        addRow("Services", project.additionalServices.joinToString { it.type.toString() })
        row("Published At") { publishedURLsAsVerticalLayout(project) }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).projectQuickDetailsTable(project: Project? = null) = init(ProjectQuickDetailsTable(project))
