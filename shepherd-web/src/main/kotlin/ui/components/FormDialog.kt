package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.content
import com.github.mvysny.karibudsl.v10.horizontalLayout
import com.github.mvysny.karibudsl.v10.onClick
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.github.mvysny.kaributools.setPrimary
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ModalityMode
import com.vaadin.flow.component.dialog.Dialog

/**
 * A dialog, editing given [form].
 * @param bean the bean to edit.
 * @param onSave when Save is pressed and validation passes. When this doesn't throw, the dialog is closed automatically.
 */
class FormDialog<B: Any>(val form: Form<B>, private val bean: B, val onSave: (B) -> Unit) : Dialog() {
    init {
        modality = ModalityMode.VISUAL
        isCloseOnEsc = false
        isCloseOnOutsideClick = false

        verticalLayout {
            content { align(right, middle) }

            add(form as Component)
            horizontalLayout {

                button("Cancel") {
                    onClick { close() }
                }
                button("Save") {
                    setPrimary()
                    onClick { save() }
                }
            }
        }

        form.read(bean)
    }

    private fun save() {
        if (form.writeIfValid(bean)) {
            onSave(bean)
            close()
        }
    }
}
