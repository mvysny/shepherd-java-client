package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.karibudsl.v10.VaadinDsl
import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.init
import com.github.mvysny.karibudsl.v10.isExpand
import com.github.mvysny.karibudsl.v10.onClick
import com.github.mvysny.karibudsl.v10.textField
import com.github.mvysny.karibudsl.v23.multiSelectComboBox
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.textfield.TextFieldVariant

/**
 * A simple field which allows the user to edit a set of short strings.
 */
class SimpleStringSetField(label: String? = null) : CustomField<Set<String>>() {
    private val comboBox: MultiSelectComboBox<String>
    private val addItemTextField: TextField
    private val addItemButton: Button
    private var value = mutableSetOf<String>()
    init {
        this.label = label
        add(HorizontalLayout().apply {
            comboBox = multiSelectComboBox {
                isExpand = true
            }
            addItemTextField = textField {
                addThemeVariants(TextFieldVariant.LUMO_SMALL)
            }
            addItemButton = button(icon = VaadinIcon.PLUS_CIRCLE.create()) {
                addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON)
            }
        })

        addItemButton.onClick {
            val newString = addItemTextField.value.trim()
            if (!newString.isBlank()) {
                value = value.toMutableSet()
                value.add(newString)
                comboBox.setItems(this.value.toMutableSet())
                comboBox.setValue(this.value.toMutableSet())
            }
            addItemTextField.value = ""
        }
        comboBox.addValueChangeListener { e ->
            if (e.isFromClient) {
                value = e.value.toMutableSet()
                updateValue()
            }
        }
    }

    override fun generateModelValue(): Set<String> = value.toMutableSet()

    override fun setPresentationValue(value: Set<String>?) {
        this.value = value.orEmpty().toMutableSet()
        comboBox.setItems(this.value.toMutableSet())
        comboBox.setValue(this.value.toMutableSet())
    }

    var hint: String
        get() = addItemTextField.placeholder
        set(value) {
            addItemTextField.placeholder = value
        }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).simpleStringSetField(label: String? = null, block: (@VaadinDsl SimpleStringSetField).() -> Unit = {}) = init(SimpleStringSetField(label), block)
