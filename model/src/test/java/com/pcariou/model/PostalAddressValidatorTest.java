package com.pcariou.model;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link PostalAddressValidator} via the validation framework,
 * since the validator builds dynamic, label-aware violation messages.
 */
public class PostalAddressValidatorTest {

    /** Minimal host bean carrying the constraint under test. */
    private static class Holder {
        @ValidPostalAddress(label = "creditor")
        private final PostalAddress address;

        Holder(PostalAddress address) {
            this.address = address;
        }
    }

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private Set<ConstraintViolation<Holder>> validate(PostalAddress address) {
        return validator.validate(new Holder(address));
    }

    private static PostalAddress address(String street, String buildingNumber,
            String postcode, String town, String country) {
        return new PostalAddress(street, buildingNumber, postcode, town, country);
    }

    // ── Valid cases ──────────────────────────────────────────────────────────

    @Test
    public void nullAddressIsValid() {
        assertTrue(validate(null).isEmpty());
    }

    @Test
    public void emptyAddressIsValid() {
        assertTrue(validate(new PostalAddress()).isEmpty());
    }

    @Test
    public void fullAddressIsValid() {
        assertTrue(validate(address("Main Street", "12", "75001", "Paris", "FR")).isEmpty());
    }

    @Test
    public void townAndCountryOnlyIsValid() {
        assertTrue(validate(address(null, null, null, "Berlin", "DE")).isEmpty());
    }

    @Test
    public void lowerCaseCountryIsAccepted() {
        assertTrue(validate(address(null, null, null, "Amsterdam", "nl")).isEmpty());
    }

    // ── Invalid cases ────────────────────────────────────────────────────────

    @Test
    public void streetWithoutTownAndCountryIsInvalid() {
        Set<ConstraintViolation<Holder>> violations =
                validate(address("Main Street", null, null, null, null));
        assertEquals("Expected missing town and missing country", 2, violations.size());
    }

    @Test
    public void missingTownIsInvalid() {
        Set<ConstraintViolation<Holder>> violations =
                validate(address("Main Street", "12", "75001", null, "FR"));
        assertEquals(1, violations.size());
        String message = violations.iterator().next().getMessage();
        assertTrue("Message should mention the owner label", message.contains("creditor"));
        assertTrue("Message should mention the town/city", message.contains("town/city"));
    }

    @Test
    public void missingCountryIsInvalid() {
        Set<ConstraintViolation<Holder>> violations =
                validate(address(null, null, null, "Paris", null));
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("country is mandatory"));
    }

    @Test
    public void invalidCountryCodeIsRejected() {
        Set<ConstraintViolation<Holder>> violations =
                validate(address(null, null, null, "Paris", "XX"));
        assertEquals(1, violations.size());
        String message = violations.iterator().next().getMessage();
        assertTrue("Message should quote the invalid value", message.contains("XX"));
        assertTrue(message.contains("ISO country code"));
    }

    @Test
    public void countryLongerThanTwoLettersIsRejected() {
        assertEquals(1, validate(address(null, null, null, "Paris", "FRA")).size());
    }

    // ── ISO 20022 field-length limits ────────────────────────────────────────

    @Test
    public void streetAtMaximumLengthIsValid() {
        String street = repeat('A', 70);
        assertTrue(validate(address(street, "12", "75001", "Paris", "FR")).isEmpty());
    }

    @Test
    public void streetOverMaximumLengthIsRejected() {
        Set<ConstraintViolation<Holder>> violations =
                validate(address(repeat('A', 71), "12", "75001", "Paris", "FR"));
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("street name"));
    }

    @Test
    public void townOverMaximumLengthIsRejected() {
        Set<ConstraintViolation<Holder>> violations =
                validate(address("Main Street", "12", "75001", repeat('B', 36), "FR"));
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("town/city"));
    }

    @Test
    public void buildingNumberAndPostcodeOverMaximumLengthAreRejected() {
        Set<ConstraintViolation<Holder>> violations =
                validate(address("Main Street", repeat('1', 17), repeat('9', 17), "Paris", "FR"));
        assertEquals(2, violations.size());
    }

    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
