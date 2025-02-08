package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.shepherd.web.security.UserLoginService
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.vaadin.flow.component.ClientCallable
import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.dependency.JavaScript
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.html.Div
import org.slf4j.LoggerFactory

@Tag("google-signin-button")
@JsModule("./src/google-signin-button.js")
@JavaScript(value = "https://accounts.google.com/gsi/client")
class GoogleSignInButton(val clientId: String, val ssoOnlyAllowEmailsEndingWith: String?) : Div() {
    init {
        getElement().setProperty("clientId", clientId)
    }

    companion object {
        private val transport = GoogleNetHttpTransport.newTrustedTransport()
        private val jsonFactory = GsonFactory.getDefaultInstance()
        @JvmStatic
        private val log = LoggerFactory.getLogger(GoogleSignInButton::class.java)
    }

    @ClientCallable
    private fun onSignIn(credential: String) {
        try {
            val verifier = GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(listOf(clientId))
                .build()
            val idToken = verifier.verify(credential)
            require(idToken != null) { "Failed to verify credentials" }
            val email = idToken.payload.email!!
            if (ssoOnlyAllowEmailsEndingWith != null) {
                require(email.endsWith(ssoOnlyAllowEmailsEndingWith)) { "Only $ssoOnlyAllowEmailsEndingWith emails allowed" }
            }
            val name = idToken.payload.get("name") as String
            UserLoginService.get().loginViaGoogleSSO(email, name)
        } catch (ex: Exception) {
            log.error("Google SSO Failed", ex)
            showErrorNotification(ex.message ?: "SSO Failed")
        }
    }
}
