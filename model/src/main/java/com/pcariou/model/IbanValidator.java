package com.pcariou.model;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.math.BigInteger;

public class IbanValidator implements ConstraintValidator<ValidIban, String> {
    
    @Override
    public boolean isValid(String iban, ConstraintValidatorContext context) {
        if (iban == null || iban.isEmpty()) {
            return true;
        }
        String cleaned = iban.replaceAll("\\s", "").toUpperCase();
        if (!cleaned.matches("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$")) {
            buildMessage(context, iban, "format invalide (attendu: XX00XXXXXXXXXXX)");
            return false;
        }
        if (!checkMod97(cleaned)) {
            buildMessage(context, iban, "échec de la somme de contrôle MOD 97");
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
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                "IBAN invalide — valeur reçue: [" + value + "] — raison: " + reason
        ).addConstraintViolation();
    }
}
