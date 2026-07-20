package com.pcariou.model;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Validates an IBAN against the official ISO 13616 / SWIFT IBAN registry:
 * the basic structure, the registered country code, the exact length expected
 * for that country, and the MOD-97 check digits. Validating the country and
 * length locally turns a preventable formatting problem into a clear,
 * row-contextualised error, instead of letting it slip through to schema
 * validation (which only enforces the generic ISO {@code IBAN2007} pattern,
 * not the per-country length).
 *
 * <p>The value is compared in its canonical form ({@link SepaFieldNormalizer#iban}),
 * which is the same form generation emits, so the value validated here is the
 * value written to the file.
 */
public class IbanValidator implements ConstraintValidator<ValidIban, String> {

    /**
     * Registered IBAN length per country (ISO 13616 registry). Offline and
     * deterministic; extend as new countries join the registry.
     */
    private static final Map<String, Integer> IBAN_LENGTHS = new HashMap<>();

    static {
        IBAN_LENGTHS.put("AD", 24); IBAN_LENGTHS.put("AE", 23); IBAN_LENGTHS.put("AL", 28);
        IBAN_LENGTHS.put("AT", 20); IBAN_LENGTHS.put("AZ", 28); IBAN_LENGTHS.put("BA", 20);
        IBAN_LENGTHS.put("BE", 16); IBAN_LENGTHS.put("BG", 22); IBAN_LENGTHS.put("BH", 22);
        IBAN_LENGTHS.put("BR", 29); IBAN_LENGTHS.put("BY", 28); IBAN_LENGTHS.put("CH", 21);
        IBAN_LENGTHS.put("CR", 22); IBAN_LENGTHS.put("CY", 28); IBAN_LENGTHS.put("CZ", 24);
        IBAN_LENGTHS.put("DE", 22); IBAN_LENGTHS.put("DK", 18); IBAN_LENGTHS.put("DO", 28);
        IBAN_LENGTHS.put("EE", 20); IBAN_LENGTHS.put("EG", 29); IBAN_LENGTHS.put("ES", 24);
        IBAN_LENGTHS.put("FI", 18); IBAN_LENGTHS.put("FO", 18); IBAN_LENGTHS.put("FR", 27);
        IBAN_LENGTHS.put("GB", 22); IBAN_LENGTHS.put("GE", 22); IBAN_LENGTHS.put("GI", 23);
        IBAN_LENGTHS.put("GL", 18); IBAN_LENGTHS.put("GR", 27); IBAN_LENGTHS.put("GT", 28);
        IBAN_LENGTHS.put("HR", 21); IBAN_LENGTHS.put("HU", 28); IBAN_LENGTHS.put("IE", 22);
        IBAN_LENGTHS.put("IL", 23); IBAN_LENGTHS.put("IQ", 23); IBAN_LENGTHS.put("IS", 26);
        IBAN_LENGTHS.put("IT", 27); IBAN_LENGTHS.put("JO", 30); IBAN_LENGTHS.put("KW", 30);
        IBAN_LENGTHS.put("KZ", 20); IBAN_LENGTHS.put("LB", 28); IBAN_LENGTHS.put("LC", 32);
        IBAN_LENGTHS.put("LI", 21); IBAN_LENGTHS.put("LT", 20); IBAN_LENGTHS.put("LU", 20);
        IBAN_LENGTHS.put("LV", 21); IBAN_LENGTHS.put("LY", 25); IBAN_LENGTHS.put("MC", 27);
        IBAN_LENGTHS.put("MD", 24); IBAN_LENGTHS.put("ME", 22); IBAN_LENGTHS.put("MK", 19);
        IBAN_LENGTHS.put("MR", 27); IBAN_LENGTHS.put("MT", 31); IBAN_LENGTHS.put("MU", 30);
        IBAN_LENGTHS.put("NL", 18); IBAN_LENGTHS.put("NO", 15); IBAN_LENGTHS.put("PK", 24);
        IBAN_LENGTHS.put("PL", 28); IBAN_LENGTHS.put("PS", 29); IBAN_LENGTHS.put("PT", 25);
        IBAN_LENGTHS.put("QA", 29); IBAN_LENGTHS.put("RO", 24); IBAN_LENGTHS.put("RS", 22);
        IBAN_LENGTHS.put("SA", 24); IBAN_LENGTHS.put("SC", 31); IBAN_LENGTHS.put("SE", 24);
        IBAN_LENGTHS.put("SI", 19); IBAN_LENGTHS.put("SK", 24); IBAN_LENGTHS.put("SM", 27);
        IBAN_LENGTHS.put("ST", 25); IBAN_LENGTHS.put("SV", 28); IBAN_LENGTHS.put("TL", 23);
        IBAN_LENGTHS.put("TN", 24); IBAN_LENGTHS.put("TR", 26); IBAN_LENGTHS.put("UA", 29);
        IBAN_LENGTHS.put("VA", 22); IBAN_LENGTHS.put("VG", 24); IBAN_LENGTHS.put("XK", 20);
    }

    @Override
    public boolean isValid(String iban, ConstraintValidatorContext context) {
        if (iban == null || iban.isEmpty()) {
            return true;
        }
        String cleaned = SepaFieldNormalizer.iban(iban);
        if (!cleaned.matches("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$")) {
            buildMessage(context, iban,
                    "the format is invalid (expected two letters, two check digits, "
                            + "then the account identifier, e.g. FR7630006000011234567890189)");
            return false;
        }
        String country = cleaned.substring(0, 2);
        Integer expectedLength = IBAN_LENGTHS.get(country);
        if (expectedLength == null) {
            buildMessage(context, iban,
                    "the country code \"" + country + "\" is not a recognised IBAN country");
            return false;
        }
        if (cleaned.length() != expectedLength) {
            buildMessage(context, iban,
                    "the length is incorrect for country " + country + " (expected "
                            + expectedLength + " characters but found " + cleaned.length() + ")");
            return false;
        }
        if (!checkMod97(cleaned)) {
            buildMessage(context, iban, "the MOD 97 check digits are incorrect");
            return false;
        }
        return true;
    }
    
    private boolean checkMod97(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numericIban = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numericIban.append(c - 'A' + 10);
            } else {
                numericIban.append(c);
            }
        }
        BigInteger numericValue = new BigInteger(numericIban.toString());
        return numericValue.mod(BigInteger.valueOf(97)).intValue() == 1;
    }
    
    private void buildMessage(ConstraintValidatorContext context, String value, String reason) {
        if (context == null) {
            return;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                "The IBAN \"" + value + "\" is invalid: " + reason + "."
        ).addConstraintViolation();
    }
}
