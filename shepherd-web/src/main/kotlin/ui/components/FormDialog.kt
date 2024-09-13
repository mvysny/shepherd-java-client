package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.horizontalLayout
import com.github.mvysny.karibudsl.v10.onClick
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.kaributools.setPrimary
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.dialog.Dialog

/**
 * @param bean may be null in buffered mode, if the bean is set upfront to the form.
 * @param onSave when Save is pressed and validation passes.
 */
class FormDialog<B>(val form: Form<B>, private val bean: B?, val onSave: (B) -> Unit) : Dialog() {
    init {
        isModal = true

        verticalLayout {
            add(form as Component)
            horizontalLayout {
                button("Save") {
                    setPrimary()
                    onClick { save() }
                }
                button("Cancel") {
                    onClick { close() }
                }
            }
        }

        if (form.binder.bean == null) {
            form.binder.readBean(bean!!)
        }
    }

    private fun save() {
        if (form.writeIfValid(bean)) {
            onSave(form.binder.bean ?: bean ?: throw RuntimeException("Foo"))
            close()
        }
    }
}
