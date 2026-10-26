package com.pcariou.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InitiatingPartyIdentifierTest {

    @Test
    public void blankFieldsMeanNoIdentifier() {
        assertNull(InitiatingPartyIdentifier.of(" ", "", null, "  "));
        assertNull(InitiatingPartyIdentifier.ofId(null));
    }

    @Test
    public void valuesAreTrimmedAndSchemeCodeIsUppercased() {
        InitiatingPartyIdentifier identifier = InitiatingPartyIdentifier.of(" IE123 ", " bank ", null, " BOFI ");
        assertEquals("IE123", identifier.getId());
        assertEquals("BANK", identifier.getSchemeCode());
        assertEquals("BOFI", identifier.getIssuer());
        assertTrue(identifier.problems().isEmpty());
    }

    @Test
    public void legacySiretAndNonFrenchIdentifiersWithoutSchemeAreValid() {
        assertTrue(InitiatingPartyIdentifier.ofId("12345678901234").problems().isEmpty());
        assertTrue(InitiatingPartyIdentifier.ofId("OIN-778899").problems().isEmpty());
        assertTrue(InitiatingPartyIdentifier.ofId("DE123456789").problems().isEmpty());
    }

    @Test
    public void invalidCombinationsAreReported() {
        assertEquals(1, InitiatingPartyIdentifier.of(null, "BANK", null, null).problems().size());
        assertEquals(1, InitiatingPartyIdentifier.of("X", "BANK", "Other", null).problems().size());
        assertEquals(1, InitiatingPartyIdentifier.of("X", "BANKS", null, null).problems().size());
        assertEquals(1, InitiatingPartyIdentifier.of(repeat('9', 36), null, null, null).problems().size());
        assertEquals(1, InitiatingPartyIdentifier.of("X", null, repeat('p', 36), null).problems().size());
        assertEquals(1, InitiatingPartyIdentifier.of("X", null, null, repeat('i', 36)).problems().size());
    }

    @Test
    public void frenchSchemeCodesKeepTheirDigitChecks() {
        assertEquals(1, InitiatingPartyIdentifier.of("123", "SRET", null, null).problems().size());
        assertTrue(InitiatingPartyIdentifier.of("12345678901234", "SRET", null, null).problems().isEmpty());
        assertEquals(1, InitiatingPartyIdentifier.of("12345", "SREN", null, null).problems().size());
        assertTrue(InitiatingPartyIdentifier.of("123456789", "SREN", null, null).problems().isEmpty());
    }

    @Test
    public void schemeWarningOnlyForPain02WithAConfiguredScheme() {
        InitiatingPartyIdentifier code = InitiatingPartyIdentifier.of("IE1", "BANK", null, null);
        InitiatingPartyIdentifier proprietary = InitiatingPartyIdentifier.of("IE1", null, "Originator ID", null);
        InitiatingPartyIdentifier plain = InitiatingPartyIdentifier.of("12345678901234", null, null, "INSEE");

        assertEquals(InitiatingPartyIdentifier.SCHEME_NOT_IN_PAIN_001_02,
                InitiatingPartyIdentifier.formatWarning(code, PainVersion.PAIN_001_001_02));
        assertEquals(InitiatingPartyIdentifier.SCHEME_NOT_IN_PAIN_001_02,
                InitiatingPartyIdentifier.formatWarning(proprietary, PainVersion.PAIN_001_001_02));
        assertNull(InitiatingPartyIdentifier.formatWarning(plain, PainVersion.PAIN_001_001_02));
        assertNull(InitiatingPartyIdentifier.formatWarning(null, PainVersion.PAIN_001_001_02));
        assertNull(InitiatingPartyIdentifier.formatWarning(code, PainVersion.PAIN_001_001_03));
        assertNull(InitiatingPartyIdentifier.formatWarning(code, PainVersion.PAIN_001_001_09));
    }

    private static String repeat(char c, int count) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            text.append(c);
        }
        return text.toString();
    }
}
