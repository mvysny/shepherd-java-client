package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.shepherd.web.showErrorNotification
import com.vaadin.flow.data.binder.Binder

/**
 * A form, editing given bean [B].
 */
interface Form<B> {
    /**
     * Binds Vaadin fields to bean fields. Always in buffered mode!
     */
    val binder: Binder<B>

    /**
     * @throws Exception if validation fails.
     */
    fun additionalValidation(bean: B) {}

    /**
     * Validates everything and writes the data to given bean. If all went okay, returns true.
     * @param toBean writes changes to this bean.
     */
    fun writeIfValid(toBean: B): Boolean {
        if (!binder.validate().isOk || !binder.writeBeanIfValid(toBean)) {
            showErrorNotification("There are errors in the form")
            return false
        }
        try {
            additionalValidation(toBean)
        } catch (e: Exception) {
            showErrorNotification("There are errors in the form: ${e.message}")
            return false
        }
        return true
    }
}
