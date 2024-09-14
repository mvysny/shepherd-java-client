package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.karibudsl.v10.VaadinDsl
import com.github.mvysny.karibudsl.v10.componentColumn
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.confirmdialog.ConfirmDialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant

@VaadinDsl
fun <T> (@VaadinDsl Grid<T>).iconButtonColumn(
    icon: VaadinIcon,
    onClick: (T) -> Unit
): Grid.Column<T> = componentColumn({ item ->
    Button(icon.create(), { e -> onClick(item) }).apply {
        addThemeVariants(
            ButtonVariant.LUMO_TERTIARY_INLINE,
            ButtonVariant.LUMO_SMALL,
            ButtonVariant.LUMO_ICON
        )
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
