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
 *   <li>each provided field — must fit its ISO 20022 maximum length</li>
 * </ul>
 */
public class PostalAddressValidator implements ConstraintValidator<ValidPostalAddress, PostalAddress>
{
	private static final Set<String> ISO_COUNTRIES =
			new HashSet<String>(Arrays.asList(Locale.getISOCountries()));

	private static final int MAX_STREET = 70;
	private static final int MAX_BUILDING_NUMBER = 16;
	private static final int MAX_POSTCODE = 16;
	private static final int MAX_TOWN = 35;

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

		valid &= checkLength(context, address.getStreet(), MAX_STREET, "street name");
		valid &= checkLength(context, address.getBuildingNumber(), MAX_BUILDING_NUMBER, "building number");
		valid &= checkLength(context, address.getPostcode(), MAX_POSTCODE, "post code");
		valid &= checkLength(context, address.getTown(), MAX_TOWN, "town/city");

		return valid;
	}

	private boolean checkLength(ConstraintValidatorContext context, String value, int max, String fieldName)
	{
		if (value != null && value.trim().length() > max) {
			addViolation(context, "The " + label + " address " + fieldName + " must be at most "
					+ max + " characters (found " + value.trim().length() + ").");
			return false;
		}
		return true;
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
