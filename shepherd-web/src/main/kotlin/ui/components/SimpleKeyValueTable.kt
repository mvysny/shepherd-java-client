package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.VaadinDsl
import com.github.mvysny.karibudsl.v10.div
import com.github.mvysny.karibudsl.v10.init
import com.github.mvysny.karibudsl.v10.strong
import com.github.mvysny.kaributools.HtmlSpan
import com.github.mvysny.shepherd.web.Bootstrap
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.html.Span
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
        div.element.style.set("grid-template-columns", "auto 1fr")
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
}

@VaadinDsl
fun (@VaadinDsl HasComponents).simpleKeyValueTable(block: (@VaadinDsl SimpleKeyValueTable).() -> Unit = {}) = init(SimpleKeyValueTable(), block)

@VaadinDsl
fun (@VaadinDsl HasComponents).sheperdStatsTable() {
    val stats = Bootstrap.getClient().getStats()
    simpleKeyValueTable {
        addRow("Project Count", stats.projectCount.toString())
        addRow("Project Runtime Quota", stats.projectMemoryStats.projectRuntimeQuota.toString())
        addRow("Host OS: Memory", stats.hostMemoryStats.memory.toString())
        addRow("Host OS: Swap", stats.hostMemoryStats.swap.toString())
        addRow("Host OS: Disk Space", stats.diskUsage.toString())
    }
}
