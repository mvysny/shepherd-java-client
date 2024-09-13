package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.karibudsl.v10.VaadinDsl
import com.github.mvysny.karibudsl.v10.componentColumn
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.icon.VaadinIcon

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
