package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.shepherd.web.showErrorNotification
import com.vaadin.flow.data.binder.Binder

interface Form<B> {
    val binder: Binder<B>

    /**
     * @param bean bean being edited.
     * @throws Exception if validation fails.
     */
    fun additionalValidation(bean: B) {}

    /**
     * Validates everything and writes the data to given bean. If all went okay, returns true.
     * @param toBean if binder is in buffered mode, writes changes to this bean. If binder is in unbuffered mode, this is ignored and may be null.
     */
    fun writeIfValid(toBean: B?): Boolean {
        try {
            additionalValidation(binder.bean ?: toBean ?: throw UnsupportedOperationException("toBean must not be null when binder in buffered mode"))
        } catch (e: Exception) {
            showErrorNotification("There are errors in the form: ${e.message}")
            return false
        }
        if (!binder.validate().isOk) {
            showErrorNotification("There are errors in the form")
            return false
        }
        if (binder.bean == null && !binder.writeBeanIfValid(toBean)) {
            showErrorNotification("There are errors in the form")
            return false
        }
        return true
    }
}
