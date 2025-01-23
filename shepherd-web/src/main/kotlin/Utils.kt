package com.github.mvysny.shepherd.web

import com.github.mvysny.shepherd.api.GitRepo
import com.vaadin.flow.data.binder.ValidationResult
import com.vaadin.flow.data.binder.ValueContext
import jakarta.validation.Validation
import jakarta.validation.Validator

private val validator: Validator =
    Validation.buildDefaultValidatorFactory().validator

/**
 * Validates [obj].
 * @throws ValidationException if validation fails.
 */
fun jsr303Validate(obj: Any) {
    validator.validate(obj)
}

object GitUrlValidator : com.vaadin.flow.data.binder.Validator<String?> {
    override fun apply(
        value: String?,
        context: ValueContext
    ): ValidationResult =
        try {
            if (value != null) GitRepo.validateGitUrl(value)
            ValidationResult.ok()
        } catch (ex: Exception) {
            ValidationResult.error(ex.message)
        }
}
