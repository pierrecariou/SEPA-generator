package com.pcariou.model;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link IbanValidator}: structure, registered country, the
 * per-country registry length, and MOD-97 check digits. The validator is
 * exercised directly with a {@code null} context, which it supports.
 */
public class IbanValidatorTest {

    private final IbanValidator validator = new IbanValidator();

    private boolean isValid(String iban) {
        return validator.isValid(iban, null);
    }

    // ── Valid IBANs from supported countries ─────────────────────────────────

    @Test
    public void validFrenchIban() {
        assertTrue(isValid("FR7630006000011234567890189"));
    }

    @Test
    public void validGermanIban() {
        assertTrue(isValid("DE89370400440532013000"));
    }

    @Test
    public void validGbAndBelgianAndDutchIbans() {
        assertTrue(isValid("GB29NWBK60161331926819"));
        assertTrue(isValid("BE68539007547034"));
        assertTrue(isValid("NL39RABO0300065264"));
    }

    // ── Safe lexical normalisation ───────────────────────────────────────────

    @Test
    public void whitespaceAndLowerCaseAreNormalisedBeforeValidation() {
        assertTrue("Grouped/lower-case IBAN must validate after canonicalisation",
                isValid(" fr76 3000 6000 0112 3456 7890 189 "));
    }

    // ── Blank handled by @NotBlank ───────────────────────────────────────────

    @Test
    public void blankIsLeftToNotBlankConstraint() {
        assertTrue(isValid(null));
        assertTrue(isValid(""));
    }

    // ── Invalid IBANs ────────────────────────────────────────────────────────

    @Test
    public void malformedStructureIsRejected() {
        assertFalse(isValid("XX"));
        assertFalse(isValid("1234567890"));
        assertFalse(isValid("FRABCDEFGHIJKLMNO"));
    }

    @Test
    public void unrecognisedCountryIsRejected() {
        // ZZ is not a registered IBAN country; structure otherwise plausible.
        assertFalse("Unregistered country code must be rejected",
                isValid("ZZ68539007547034"));
    }

    @Test
    public void wrongLengthForCountryIsRejected() {
        // Valid FR check-digit prefix but one digit short of the 27-char length.
        assertFalse("A French IBAN that is not 27 characters must be rejected",
                isValid("FR763000600001123456789018"));
    }

    @Test
    public void checksumFailureIsRejected() {
        // Correct FR length (27) but tampered check digits.
        assertFalse("A wrong MOD-97 check digit must be rejected",
                isValid("FR7730006000011234567890189"));
    }
}
