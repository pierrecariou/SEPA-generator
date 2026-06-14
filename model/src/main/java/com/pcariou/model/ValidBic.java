package com.pcariou.model;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a value follows the standard BIC/SWIFT shape:
 * 4-letter bank code, 2-letter country code, 2 alphanumeric location
 * characters, and an optional 3-character alphanumeric branch code
 * (8 or 11 characters total). Blank values are considered valid so the
 * constraint composes with {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = BicValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBic {
    String message() default "BIC is invalid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
