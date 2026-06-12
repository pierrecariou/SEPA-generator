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

    // ── Blank handled by @NotBlank, so the validator must pass it ────────────

    @Test
    public void blankBicIsLeftToNotBlankConstraint() {
        assertTrue(isValid(null));
        assertTrue(isValid(""));
        assertTrue(isValid("   "));
    }
}
