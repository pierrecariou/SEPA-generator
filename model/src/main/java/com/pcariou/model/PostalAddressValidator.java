package com.pcariou.model;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Validation rules for an optional {@link PostalAddress}:
 * <ul>
 *   <li>{@code null} or fully empty address — valid (omitted from output)</li>
 *   <li>any field provided — town/city and country become mandatory</li>
 *   <li>country — must be a 2-letter ISO 3166 country code (e.g. FR, DE, NL)</li>
 * </ul>
 */
public class PostalAddressValidator implements ConstraintValidator<ValidPostalAddress, PostalAddress>
{
	private static final Set<String> ISO_COUNTRIES =
			new HashSet<String>(Arrays.asList(Locale.getISOCountries()));

	private String label;

	@Override
	public void initialize(ValidPostalAddress annotation)
	{
		this.label = annotation.label() == null || annotation.label().trim().isEmpty()
				? "postal" : annotation.label().trim();
	}

	@Override
	public boolean isValid(PostalAddress address, ConstraintValidatorContext context)
	{
		if (address == null || address.isEmpty()) {
			return true;
		}

		boolean valid = true;
		context.disableDefaultConstraintViolation();

		if (isBlank(address.getTown())) {
			addViolation(context, "The " + label + " address is incomplete: the town/city is mandatory"
					+ " when any address field is provided. Please add the town/city.");
			valid = false;
		}

		String country = address.getCountry() == null ? "" : address.getCountry().trim();
		if (country.isEmpty()) {
			addViolation(context, "The " + label + " address is incomplete: the country is mandatory"
					+ " when any address field is provided. Please add a 2-letter country code (e.g. FR, DE, NL).");
			valid = false;
		} else if (!ISO_COUNTRIES.contains(country.toUpperCase(Locale.ROOT))) {
			addViolation(context, "The " + label + " address country \"" + country + "\" is not valid."
					+ " Please use a 2-letter ISO country code (e.g. FR, DE, NL).");
			valid = false;
		}

		return valid;
	}

	private static void addViolation(ConstraintValidatorContext context, String message)
	{
		context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}
}
