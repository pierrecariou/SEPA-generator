package com.pcariou.model;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator for {@link ValidBic}. Checks the standard BIC/SWIFT shape:
 * 4 letters (bank code) + 2 letters (country code) + 2-character location
 * code + optional 3 alphanumerics (branch code), i.e. 8 or 11 characters.
 * Matching is done after trimming and upper-casing, so case differences are
 * tolerated.
 *
 * <p>The location code follows ISO 9362 (and the pain.001.001.02 schema's
 * {@code BICIdentifier} pattern, the strictest of the supported schemas):
 * its first character must not be the digit 0 or 1, and its second character
 * must not be the letter O. This is the intersection of the BIC rules of all
 * supported message versions, so a BIC accepted here is valid in
 * pain.001.001.02 and pain.001.001.09 alike.
 */
public class BicValidator implements ConstraintValidator<ValidBic, String> {

    /** Canonical BIC pattern (intersection of all supported schemas); reused by UI-level checks. */
    public static final String BIC_PATTERN = "^[A-Z]{4}[A-Z]{2}[A-Z2-9][A-NP-Z0-9]([A-Z0-9]{3})?$";

    @Override
    public boolean isValid(String bic, ConstraintValidatorContext context) {
        if (bic == null || bic.trim().isEmpty()) {
            return true; // blankness is handled by @NotBlank
        }
        String cleaned = bic.trim().toUpperCase();
        if (!cleaned.matches(BIC_PATTERN)) {
            buildMessage(context,
                    "BIC \"" + bic + "\" is not valid. A BIC must be 8 or 11 characters: "
                            + "4-letter bank code, 2-letter country code, 2-character location code "
                            + "(not starting with 0 or 1, second character not the letter O), "
                            + "and an optional 3-character branch code (e.g. BNPAFRPP or BNPAFRPPXXX)");
            return false;
        }
        return true;
    }

    private void buildMessage(ConstraintValidatorContext context, String message) {
        if (context == null) {
            return;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
