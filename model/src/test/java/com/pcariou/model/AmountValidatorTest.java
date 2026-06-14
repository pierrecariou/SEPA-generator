package com.pcariou.model;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AmountValidator} (transaction amount).
 *
 * The validator is exercised directly with a {@code null} context, which it
 * supports, so no validation framework bootstrap is required.
 */
public class AmountValidatorTest {

    private final AmountValidator validator = new AmountValidator();

    private boolean isValid(String amount) {
        return validator.isValid(amount, null);
    }

    // ── Valid amounts ────────────────────────────────────────────────────────

    @Test
    public void positiveAmountWithNoDecimals() {
        assertTrue(isValid("1250"));
    }

    @Test
    public void positiveAmountWithOneDecimal() {
        assertTrue(isValid("1250.5"));
    }

    @Test
    public void positiveAmountWithTwoDecimals() {
        assertTrue(isValid("1250.50"));
    }

    @Test
    public void commaDecimalSeparatorIsAccepted() {
        assertTrue("Comma separator should be accepted like in CSV inputs", isValid("1250,50"));
    }

    @Test
    public void smallestRepresentableAmountIsValid() {
        assertTrue(isValid("0.01"));
    }

    // ── Invalid amounts ──────────────────────────────────────────────────────

    @Test
    public void zeroAmountIsRejected() {
        assertFalse("0 should be rejected", isValid("0"));
        assertFalse("0.00 should be rejected", isValid("0.00"));
    }

    @Test
    public void negativeAmountIsRejected() {
        assertFalse(isValid("-10"));
        assertFalse(isValid("-10.50"));
    }

    @Test
    public void amountWithMoreThanTwoDecimalsIsRejected() {
        assertFalse(isValid("10.123"));
        assertFalse(isValid("10,123"));
    }

    @Test
    public void malformedAmountIsRejected() {
        assertFalse(isValid("abc"));
        assertFalse(isValid("12abc"));
        assertFalse(isValid("12.34.56"));
        assertFalse(isValid("12."));
        assertFalse(isValid(".5"));
        assertFalse(isValid("1 250.50"));
    }

    // ── Blank handled by @NotBlank, so the validator must pass it ────────────

    @Test
    public void blankAmountIsLeftToNotBlankConstraint() {
        assertTrue(isValid(null));
        assertTrue(isValid(""));
        assertTrue(isValid("   "));
    }
}
