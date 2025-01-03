package com.github.mvysny.shepherd.web.ui.components

import com.vaadin.flow.data.binder.Binder

/**
 * A form, editing given bean [B]. At the moment only the buffered mode is supported.
 *
 * The way this works:
 * * Call [read] to populate the form.
 * * Call [writeIfValid] to save new data to given bean.
 *
 * WARNING: the buffered mode isn't completely buffered:
 * [writeIfValid] calls [additionalValidation] after the data has been written to the bean.
 * If the function fails, the data is not reverted back, which is quite strange - I need to revisit this.
 */
interface Form<B: Any> {
    /**
     * Binds Vaadin fields to bean fields.
     */
    val binder: Binder<B>

    /**
     * Populates the form with data from given [bean].
     */
    fun read(bean: B) {
        binder.readBean(bean)
    }

    /**
     * @throws Exception if validation fails.
     */
    fun additionalValidation(bean: B) {}

    /**
     * Validates everything and writes the data to given bean. If all went okay, returns true.
     *
     * WARNING: calls [additionalValidation] after the data has been written to the bean.
     * If the function fails, the data is not reverted back, which is quite strange - I need to revisit this.
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
