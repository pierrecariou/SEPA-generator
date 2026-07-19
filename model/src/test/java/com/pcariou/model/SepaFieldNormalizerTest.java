package com.pcariou.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link SepaFieldNormalizer}: the deterministic, loss-free
 * canonicalisation applied to account identifiers so that the value validated
 * is exactly the value emitted.
 */
public class SepaFieldNormalizerTest {

    @Test
    public void ibanWhitespaceIsRemovedAndUpperCased() {
        assertEquals("FR7630006000011234567890189",
                SepaFieldNormalizer.iban(" fr76 3000 6000 0112 3456 7890 189 "));
    }

    @Test
    public void ibanNullStaysNull() {
        assertNull(SepaFieldNormalizer.iban(null));
    }

    @Test
    public void ibanAllWhitespaceBecomesEmptyForMandatoryRules() {
        assertEquals("", SepaFieldNormalizer.iban("   "));
    }

    @Test
    public void bicIsTrimmedAndUpperCased() {
        assertEquals("BNPAFRPP", SepaFieldNormalizer.bic("  bnpafrpp  "));
    }

    @Test
    public void bicNullStaysNull() {
        assertNull(SepaFieldNormalizer.bic(null));
    }
}
