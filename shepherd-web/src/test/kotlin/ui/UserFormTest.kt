package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._expectNone
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.shepherd.web.AbstractAppTest
import com.github.mvysny.shepherd.web.security.User
import com.github.mvysny.shepherd.web.security.UserRoles
import com.github.mvysny.shepherd.web.ui.components.FormDialog
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Test
import kotlin.test.expect

class UserFormTest : AbstractAppTest() {
    @Test fun smoke() {
        FormDialog(UserForm(true), User("", "", setOf(UserRoles.USER))) { } .open()
    }

    @Test fun createSimpleUser() {
        lateinit var createdUser: User
        FormDialog(UserForm(true), User("", "", setOf(UserRoles.USER))) { user ->
            createdUser = user
        } .open()

        _get<EmailField> { label = "E-mail" } ._value = "mavi@vaadin.com"
        _get<TextField> { label = "Name" } ._value = "Martin Vysny"
        _get<PasswordField> { label = "Password" } ._value = "my-secret-password"
        _get<Button> { text = "Save" } ._click()

        // assert that the user got created
        _expectNone<Dialog>()
        expect("mavi@vaadin.com") { createdUser.email }
        expect("Martin Vysny") { createdUser.name }
        expect(false) { createdUser.isAdmin }
        expect(true) { createdUser.passwordMatches("my-secret-password") }
    }

    @Test fun createPasswordlessUser() {
        lateinit var createdUser: User
        FormDialog(UserForm(true), User("", "", setOf(UserRoles.USER))) { user ->
            createdUser = user
        } .open()

        _get<EmailField> { label = "E-mail" } ._value = "mavi@vaadin.com"
        _get<TextField> { label = "Name" } ._value = "Martin Vysny"
        _get<Button> { text = "Save" } ._click()

        // assert that the user got created
        _expectNone<Dialog>()
        expect("mavi@vaadin.com") { createdUser.email }
        expect("Martin Vysny") { createdUser.name }
        expect(null) { createdUser.hashedPassword }
        expect(false) { createdUser.isAdmin }
        expect(false) { createdUser.passwordMatches("my-secret-password") }
    }
}
