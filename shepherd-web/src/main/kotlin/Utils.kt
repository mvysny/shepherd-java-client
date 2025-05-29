package com.github.mvysny.shepherd.web

import com.github.mvysny.shepherd.api.GitRepo
import com.github.mvysny.shepherd.api.ProjectId
import com.vaadin.flow.data.binder.ValidationResult
import com.vaadin.flow.data.binder.ValueContext
import jakarta.validation.Validation
import jakarta.validation.Validator

private val validator: Validator =
    Validation.buildDefaultValidatorFactory().validator

/**
 * Validates [obj].
 * @throws jakarta.validation.ValidationException if validation fails.
 */
fun jsr303Validate(obj: Any) {
    validator.validate(obj)
}

/**
 * Vaadin validator which validates Strings whether they are a valid
 * Git URL.
 */
object GitUrlValidator : com.vaadin.flow.data.binder.Validator<String?> {
    override fun apply(
        value: String?,
        context: ValueContext
    ): ValidationResult =
        try {
            if (value != null) {
                GitRepo.validateGitUrl(value)
            }
            ValidationResult.ok()
        } catch (ex: Exception) {
            ValidationResult.error(ex.message)
        }
}

/**
 * Vaadin validator which validates Strings whether they are a valid
 * [ProjectId].
 */
object ProjectIdValidator : com.vaadin.flow.data.binder.Validator<String?> {
    override fun apply(
        value: String?,
        context: ValueContext
    ): ValidationResult {
        if (value != null) {
            if (!ProjectId.isValid(value)) {
                return ValidationResult.error("The ID must contain at most 54 characters, it must contain only lowercase alphanumeric characters or '-', it must start and end with an alphanumeric character")
            }
            if (ProjectId(value).isReserved) {
                return ValidationResult.error("This ID is reserved and can not be used for projects")
            }
        }
        return ValidationResult.ok()
    }
}

@Suppress("DEPRECATION")
fun <V> com.vaadin.flow.data.binder.Validator<V>.validate(obj: V): ValidationResult = apply(obj, ValueContext())
