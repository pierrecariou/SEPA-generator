package com.pcariou.model;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link BicValidator} (used for both creditor and debtor BIC).
 *
 * The validator is exercised directly with a {@code null} context, which it
 * supports, so no validation framework bootstrap is required.
 */
public class BicValidatorTest {

    private final BicValidator validator = new BicValidator();

    private boolean isValid(String bic) {
        return validator.isValid(bic, null);
    }

    // ── Valid BICs ───────────────────────────────────────────────────────────

    @Test
    public void validBicWith8Characters() {
        assertTrue("8-character BIC should be valid", isValid("BNPAFRPP"));
    }

    @Test
    public void validBicWith11Characters() {
        assertTrue("11-character BIC should be valid", isValid("BNPAFRPPXXX"));
    }

    @Test
    public void validBicWithDigitsInLocationAndBranch() {
        assertTrue(isValid("DEUTDE2H44A"));
    }

    @Test
    public void lowerCaseBicIsAccepted() {
        assertTrue("Case should be tolerated", isValid("bnpafrpp"));
    }

    // ── Invalid BICs ─────────────────────────────────────────────────────────

    @Test
    public void invalidBicWrongLength() {
        assertFalse("9-character BIC should be invalid", isValid("BNPAFRPPX"));
        assertFalse("7-character BIC should be invalid", isValid("BNPAFRP"));
        assertFalse("12-character BIC should be invalid", isValid("BNPAFRPPXXXX"));
    }

    @Test
    public void invalidBicDigitsInBankCode() {
        assertFalse("Bank code must be letters only", isValid("1NPAFRPP"));
    }

    @Test
    public void invalidBicDigitsInCountryCode() {
        assertFalse("Country code must be letters only", isValid("BNPA12PP"));
    }

    @Test
    public void invalidBicWithSpecialCharacters() {
        assertFalse(isValid("BNPA-RPP"));
    }

    // ── Location code (ISO 9362 / pain.001.001.02 BICIdentifier) ─────────────

    @Test
    public void validLocationCodeWithAllowedDigits() {
        assertTrue("Location code may start with digits 2-9", isValid("ABCDEF29"));
        assertTrue("Branch code stays fully alphanumeric", isValid("ABCDEF2A0X1"));
    }

    @Test
    public void invalidLocationCodeStartingWithZeroOrOne() {
        assertFalse("Location code must not start with 0", isValid("ABCDEF0A"));
        assertFalse("Location code must not start with 1 (rejected by pain.001.001.02)",
                isValid("ABCDEF11"));
    }

    @Test
    public void invalidLocationCodeWithLetterOAsSecondCharacter() {
        assertFalse("Second location character must not be the letter O", isValid("ABCDEF2O"));
    }

    // ── Blank handled by @NotBlank, so the validator must pass it ────────────

    @Test
    public void blankBicIsLeftToNotBlankConstraint() {
        assertTrue(isValid(null));
        assertTrue(isValid(""));
        assertTrue(isValid("   "));
    }
}
