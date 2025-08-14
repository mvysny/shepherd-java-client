package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.karibudsl.v10.VaadinDsl
import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.componentColumn
import com.github.mvysny.karibudsl.v10.onClick
import com.github.mvysny.shepherd.api.containsWhitespaces
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.confirmdialog.ConfirmDialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.binder.ValidationResult
import com.vaadin.flow.data.binder.Validator
import com.vaadin.flow.data.binder.ValueContext

@VaadinDsl
fun <T> (@VaadinDsl Grid<T>).iconButtonColumn(
    icon: VaadinIcon,
    tooltip: String,
    onClick: (T) -> Unit
): Grid.Column<T> = componentColumn({ item ->
    button(icon = icon.create()) {
        this.onClick { onClick(item) }
        addThemeVariants(
            ButtonVariant.LUMO_TERTIARY_INLINE,
            ButtonVariant.LUMO_SMALL,
            ButtonVariant.LUMO_ICON
        )
        setTooltipText(tooltip)
    }
}) {
    width = "40px"
    flexGrow = 0
}

fun confirmDialog(question: String, onYes: () -> Unit) {
    ConfirmDialog("", question, "Yes", { onYes() }, "No", { it.source.close() }) .open()
}

fun showErrorNotification(message: String) {
    val n = Notification(message)
    n.addThemeVariants(NotificationVariant.LUMO_ERROR)
    n.position = Notification.Position.TOP_CENTER
    n.duration = 5000
    n.open()
}

fun showInfoNotification(message: String) {
    val n = Notification(message)
    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS)
    n.position = Notification.Position.TOP_CENTER
    n.duration = 5000
    n.open()
}

data class CompositeValidator<T>(val validators: List<Validator<in T?>>) : Validator<T?> {
    override fun apply(
        value: T?,
        context: ValueContext?
    ): ValidationResult {
        val firstError = validators.asSequence()
            .map { it.apply(value, context) }
            .firstOrNull { it.isError } ?: ValidationResult.ok()
        return firstError
    }
}

fun <T> Validator<T?>.and(other: Validator<T?>): Validator<T?> = CompositeValidator(listOf(this, other))

class StringContainsNoWhitespacesValidator(val errorMessage: String = "must not contain whitespaces") : Validator<String?> {
    override fun apply(
        value: String?,
        context: ValueContext?
    ): ValidationResult {
        if (value != null && value.containsWhitespaces()) {
            return ValidationResult.error(errorMessage)
        }
        return ValidationResult.ok()
    }
}

fun <BEAN> Binder.BindingBuilder<BEAN, String?>.validateNoWhitespaces(
    errorMessage: String = "must not contain whitespaces"
): Binder.BindingBuilder<BEAN, String?> =
    withValidator(StringContainsNoWhitespacesValidator(errorMessage))
