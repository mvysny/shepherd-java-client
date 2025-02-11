package com.github.mvysny.shepherd.web.ui

import com.github.mvysny.shepherd.web.Services
import com.github.mvysny.shepherd.web.devMode
import com.github.mvysny.shepherd.web.security.UserLoginService
import com.github.mvysny.shepherd.web.ui.components.showErrorNotification
import com.github.mvysny.vaadinsimplesecurity.externalauth.google.GoogleSignInButton
import com.vaadin.flow.component.ComponentEventListener
import com.vaadin.flow.component.login.AbstractLogin
import com.vaadin.flow.component.login.LoginForm
import com.vaadin.flow.component.login.LoginI18n
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import org.slf4j.LoggerFactory
import javax.security.auth.login.LoginException

var devModeAutomaticLogInPerformed = false

@Route("login")
@PageTitle("Login")
@AnonymousAllowed
class LoginRoute : VerticalLayout(), ComponentEventListener<AbstractLogin.LoginEvent> {
    companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(LoginRoute::class.java)
    }

    private val login = LoginForm()

    init {
        setSizeFull()
        justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        alignItems = FlexComponent.Alignment.CENTER

        login.addLoginListener(this)
        val loginI18n = LoginI18n.createDefault();
        loginI18n.form.username = "E-mail"
        login.setI18n(loginI18n)
        add(login)

        maybeAddGoogleSignInButton()

        if (devMode && !devModeAutomaticLogInPerformed) {
            devModeAutomaticLogInPerformed = true
            UserLoginService.get().login("mavi@vaadin.com", "admin")
        }
    }

    private fun maybeAddGoogleSignInButton() {
        val config = Services.get().client.getConfig()
        val googleSSOClientId = config.googleSSOClientId
        if (googleSSOClientId != null) {
            val googleSSOButton = GoogleSignInButton(googleSSOClientId)
            googleSSOButton.buttonTheme = GoogleSignInButton.Theme.Filled_Black
            if (config.ssoOnlyAllowEmailsEndingWith != null) {
                googleSSOButton.hd = config.ssoOnlyAllowEmailsEndingWith!!.dropWhile { it == '@' }
            }
            googleSSOButton.addSignInListener { e ->
                try {
                    if (e.failure != null) {
                        throw e.failure!!
                    }
                    val userInfo = e.userInfo!!
                    if (config.ssoOnlyAllowEmailsEndingWith != null && !userInfo.email.endsWith(config.ssoOnlyAllowEmailsEndingWith!!, true)) {
                        throw RuntimeException("Only ${config.ssoOnlyAllowEmailsEndingWith} e-mails accepted")
                    }
                    UserLoginService.get().loginViaGoogleSSO(userInfo.email, userInfo.name)
                } catch (e: Exception) {
                    log.warn("Google Login failed", e)
                    showErrorNotification("Google Login failed")
                }
            }
            add(googleSSOButton)
        }
    }

    override fun onComponentEvent(event: AbstractLogin.LoginEvent) {
        try {
            UserLoginService.get().login(event.username, event.password)
        } catch (ex: LoginException) {
            log.warn("Login failed", ex)
            login.isError = true
        }
    }
}
