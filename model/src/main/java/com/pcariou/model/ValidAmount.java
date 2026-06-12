package com.pcariou.model;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a value is a transaction amount: a decimal number strictly
 * greater than zero with at most 2 decimal places. Both "." and "," are
 * accepted as decimal separator, matching the existing CSV input handling.
 * Blank values are considered valid so the constraint composes with
 * {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = AmountValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAmount {
    String message() default "Amount is invalid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
