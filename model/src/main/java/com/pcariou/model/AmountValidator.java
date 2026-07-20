package com.pcariou.model;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

/**
 * Validator for {@link ValidAmount}. An amount is valid when it is a decimal
 * number, strictly greater than zero, with at most 2 decimal places. Both
 * "." and "," are accepted as decimal separator, matching the existing CSV
 * input handling.
 */
public class AmountValidator implements ConstraintValidator<ValidAmount, String> {

    private static final String DECIMAL_PATTERN = "^-?[0-9]+([.][0-9]+)?$";
    private static final String MAX_2_DECIMALS_PATTERN = "^-?[0-9]+([.][0-9]{1,2})?$";

    /**
     * EPC maximum amount per transaction for SEPA Credit Transfer
     * (999 999 999.99 EUR). The ISO schema allows a far larger
     * {@code fractionDigits}-bounded value, so this ceiling is an EPC business
     * rule enforced separately from schema validity.
     */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999.99");

    @Override
    public boolean isValid(String amount, ConstraintValidatorContext context) {
        if (amount == null || amount.trim().isEmpty()) {
            return true; // blankness is handled by @NotBlank
        }
        String normalized = amount.trim().replace(',', '.');

        if (!normalized.matches(DECIMAL_PATTERN)) {
            buildMessage(context,
                    "Amount \"" + amount + "\" is not a valid number. "
                            + "Expected a positive decimal amount such as 1250 or 1250.50");
            return false;
        }
        if (!normalized.matches(MAX_2_DECIMALS_PATTERN)) {
            buildMessage(context,
                    "Amount \"" + amount + "\" has more than 2 decimal places. "
                            + "Amounts must have at most 2 decimal places (e.g. 1250.50)");
            return false;
        }
        if (new BigDecimal(normalized).compareTo(BigDecimal.ZERO) <= 0) {
            buildMessage(context,
                    "Amount \"" + amount + "\" must be greater than 0");
            return false;
        }
        if (new BigDecimal(normalized).compareTo(MAX_AMOUNT) > 0) {
            buildMessage(context,
                    "Amount \"" + amount + "\" exceeds the maximum permitted SEPA amount of "
                            + "999999999.99");
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
