package com.github.mvysny.shepherd.web

import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import jakarta.validation.Validation
import jakarta.validation.Validator

fun showErrorNotification(message: String) {
    val n = Notification(message)
    n.addThemeVariants(NotificationVariant.LUMO_ERROR)
    n.position = Notification.Position.TOP_CENTER
    n.duration = 5000
    n.open()
}

private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

/**
 * Validates [obj].
 * @throws ValidationException if validation fails.
 */
fun jsr303Validate(obj: Any) {
    validator.validate(obj)
}
