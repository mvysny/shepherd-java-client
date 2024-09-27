package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._expectNone
import com.github.mvysny.kaributesting.v10._expectOne
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.shepherd.web.AbstractAppTest
import com.github.mvysny.shepherd.web.generateRandomOfflineKey
import com.github.mvysny.shepherd.web.ui.NamedVar
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Test
import kotlin.test.expect

class NamedVarSetFieldTest : AbstractAppTest() {
    @Test fun smoke() {
        UI.getCurrent().add(NamedVarSetField())
        UI.getCurrent().add(NamedVarSetField("Foo"))
        UI.getCurrent().add(NamedVarSetField(""))

        val f = NamedVarSetField()
        expect(null) { f._value }
    }

    private fun add(name: String, value: String) {
        _get<Button> { text = "Add" }._click()
        _expectOne<FormDialog<*>>()

        _get<TextField> { label = "Name" } ._value = name
        _get<TextField> { label = "Value" } ._value = value
        _get<Button> { text = "Save" }._click()
    }

    @Test fun simpleValueAdd() {
        val f = NamedVarSetField()
        UI.getCurrent().add(f)

        add("offlinekey", "value")
        _expectNone<FormDialog<*>>()

        expect(setOf(NamedVar("offlinekey", "value"))) { f._value }
    }

    @Test fun addMultipleValues() {
        val f = NamedVarSetField()
        UI.getCurrent().add(f)

        add("offlinekey", "value")
        _expectNone<FormDialog<*>>()
        add("foo", "bar")
        _expectNone<FormDialog<*>>()
        add("bar", "baz")
        _expectNone<FormDialog<*>>()

        expect(setOf(NamedVar("offlinekey", "value"), NamedVar("foo", "bar"), NamedVar("bar", "baz"))) { f._value }
    }

    @Test fun shouldBeAbleToHandleOfflineKey() {
        val f = NamedVarSetField()
        UI.getCurrent().add(f)

        val offlineKey = generateRandomOfflineKey()
        add("offlinekey", offlineKey)
        _expectNone<FormDialog<*>>()

        expect(setOf(NamedVar("offlinekey", offlineKey))) { f._value }
    }
}
