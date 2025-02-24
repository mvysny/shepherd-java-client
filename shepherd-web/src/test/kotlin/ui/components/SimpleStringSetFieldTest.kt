package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.expect

class SimpleStringSetFieldTest {
    @BeforeEach fun setupVaadin() { MockVaadin.setup() }
    @AfterEach fun teardownVaadin() { MockVaadin.tearDown() }

    @Test fun smoke() {
        UI.getCurrent().add(SimpleStringSetField())
        UI.getCurrent().add(SimpleStringSetField("Foo"))
        UI.getCurrent().add(SimpleStringSetField(""))

        val f = SimpleStringSetField()
        expect(null) { f._value }
    }

    private fun add(value: String) {
        _get<TextField> { id = "addItemTextField" } ._value = value
        _get<Button> { id = "addItemButton" }._click()
        expect("") { _get<TextField> { id = "addItemTextField" } ._value }
        expect(false) { _get<TextField> { id = "addItemTextField" } .isInvalid }
    }

    @Test fun simpleValueAdd() {
        val f = SimpleStringSetField()
        UI.getCurrent().add(f)

        add("offlinekey")

        expect(setOf("offlinekey")) { f._value }
    }

    @Test fun addMultipleValues() {
        val f = SimpleStringSetField()
        UI.getCurrent().add(f)

        add("offlinekey")
        add("foo")
        add("bar")

        expect(setOf("offlinekey", "foo", "bar")) { f._value }
    }
}
