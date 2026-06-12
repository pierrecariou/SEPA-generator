package com.pcariou.model;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates an optional {@link PostalAddress}: an empty or {@code null}
 * address is valid (it is simply omitted from the output), but as soon as
 * any field is provided the address must contain at least a town/city and
 * a valid 2-letter ISO country code.
 *
 * <p>The {@link #label()} names the address owner (e.g. "creditor",
 * "debtor") so error messages tell the user exactly which address to fix.
 */
@Documented
@Constraint(validatedBy = PostalAddressValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPostalAddress
{
	String message() default "The postal address is invalid";

	/** Owner of the address, used in error messages (e.g. "creditor", "debtor"). */
	String label() default "";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
