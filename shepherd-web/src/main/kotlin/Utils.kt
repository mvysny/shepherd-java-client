package com.github.mvysny.shepherd.web

import jakarta.validation.Validation
import jakarta.validation.Validator

private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

/**
 * Validates [obj].
 * @throws ValidationException if validation fails.
 */
fun jsr303Validate(obj: Any) {
    validator.validate(obj)
}
