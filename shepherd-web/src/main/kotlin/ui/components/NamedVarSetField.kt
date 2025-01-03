package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.karibudsl.v10.VaadinDsl
import com.github.mvysny.karibudsl.v10.beanValidationBinder
import com.github.mvysny.karibudsl.v10.bind
import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.column
import com.github.mvysny.karibudsl.v10.content
import com.github.mvysny.karibudsl.v10.grid
import com.github.mvysny.karibudsl.v10.init
import com.github.mvysny.karibudsl.v10.onClick
import com.github.mvysny.karibudsl.v10.textField
import com.github.mvysny.karibudsl.v10.trimmingConverter
import com.github.mvysny.shepherd.web.ui.NamedVar
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.GridVariant
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.function.SerializablePredicate
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.Length

/**
 * Edits a set of [NamedVar].
 *
 * The set is not mutated in-place: Whenever a [NamedVar] is added, removed
 * or changed, a new set is constructed and set as an underlying value.
 * That means that the field works with buffered forms as well.
 */
class NamedVarSetField(label: String? = null) : CustomField<Set<NamedVar>>() {
    private var value = setOf<NamedVar>()
    private lateinit var varGrid: Grid<NamedVar>

    private val layout = content {
        varGrid = grid {
            height = "100px"
            addThemeVariants(GridVariant.LUMO_COMPACT)

            column(NamedVar::name) {
                isAutoWidth = true
                flexGrow = 0
            }
            column(NamedVar::value)
            iconButtonColumn(VaadinIcon.EDIT) { edit(it) }
            iconButtonColumn(VaadinIcon.TRASH) { delete(it) }
        }
        button("Add") {
            onClick { createNew() }
        }
    }

    init {
        this.label = label
    }

    private fun edit(v: NamedVar) {
        FormDialog(NamedVarForm(value.map { it.name } .toSet() - v.name, false), MutableNamedVar(v.name, v.value)) {
            delete(v)
            add(it)
        } .open()
    }

    private fun refresh() {
        varGrid.setItems(value)
        varGrid.recalculateColumnWidths()
    }

    private fun createNew() {
        FormDialog(NamedVarForm(value.map { it.name } .toSet(), true), MutableNamedVar()) {
            add(it)
        } .open()
    }

    private fun add(v: MutableNamedVar) {
        value = (value + NamedVar(v.name, v.value)).toSet()
        refresh()
        updateValue()
    }

    private fun delete(v: NamedVar) {
        value = (value - v).toSet()
        refresh()
        updateValue()
    }

    override fun generateModelValue(): Set<NamedVar> = value.toSet()

    override fun setPresentationValue(newPresentationValue: Set<NamedVar>?) {
        value = newPresentationValue?.toSet() ?: setOf()
        refresh()
    }
}

data class MutableNamedVar(
    @field:NotNull
    @field:NotBlank
    @field:Length(max = 255)
    var name: String = "",
    @field:NotNull
    @field:NotBlank
    @field:Length(max = 2500) // should be able to accept offlineVaadinKey which is 2267 chars long
    var value: String = ""
)

private class NamedVarForm(val existingNames: Set<String>, val creating: Boolean) : FormLayout(), Form<MutableNamedVar> {
    override val binder = beanValidationBinder<MutableNamedVar>()
    init {
        textField("Name") {
            isReadOnly = !creating
            bind(binder)
                .trimmingConverter()
                .withValidator(SerializablePredicate { it -> !existingNames.contains(it) }, "This name is already present")
                .withValidator(StringContainsNoWhitespacesValidator())
                .bind(MutableNamedVar::name)
        }
        textField("Value") {
            bind(binder).trimmingConverter().bind(MutableNamedVar::value)
        }
    }
}

@VaadinDsl
fun (@VaadinDsl HasComponents).namedVarSetField(label: String? = null, block: (@VaadinDsl NamedVarSetField).() -> Unit = {}) = init(NamedVarSetField(label), block)
