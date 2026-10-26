package com.pcariou.model;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates an optional {@link InitiatingPartyIdentifier}: {@code null} is valid
 * (the initiating party is written with its name only); otherwise every
 * {@link InitiatingPartyIdentifier#problems() problem} becomes a violation.
 */
@Documented
@Constraint(validatedBy = ValidInitiatingPartyIdentifier.Validator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidInitiatingPartyIdentifier
{
	String message() default "The initiating party identifier is invalid";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	final class Validator
			implements ConstraintValidator<ValidInitiatingPartyIdentifier, InitiatingPartyIdentifier>
	{
		@Override
		public boolean isValid(InitiatingPartyIdentifier identifier, ConstraintValidatorContext context)
		{
			if (identifier == null || identifier.problems().isEmpty()) {
				return true;
			}
			context.disableDefaultConstraintViolation();
			for (String problem : identifier.problems()) {
				context.buildConstraintViolationWithTemplate(escape(problem)).addConstraintViolation();
			}
			return false;
		}

		/** Problems are literal text, never message-interpolation templates. */
		private static String escape(String message)
		{
			return message.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}")
					.replace("$", "\\$");
		}
	}
}
