package com.pcariou.model;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator for {@link ValidBic}. Checks the standard BIC/SWIFT shape:
 * 4 letters (bank code) + 2 letters (country code) + 2 alphanumerics
 * (location code) + optional 3 alphanumerics (branch code), i.e. 8 or 11
 * characters. Matching is done after trimming and upper-casing, so case
 * differences are tolerated.
 */
public class BicValidator implements ConstraintValidator<ValidBic, String> {

    static final String BIC_PATTERN = "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$";

    @Override
    public boolean isValid(String bic, ConstraintValidatorContext context) {
        if (bic == null || bic.trim().isEmpty()) {
            return true; // blankness is handled by @NotBlank
        }
        String cleaned = bic.trim().toUpperCase();
        if (!cleaned.matches(BIC_PATTERN)) {
            buildMessage(context,
                    "BIC \"" + bic + "\" is not valid. A BIC must be 8 or 11 characters: "
                            + "4-letter bank code, 2-letter country code, 2-character location code, "
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
