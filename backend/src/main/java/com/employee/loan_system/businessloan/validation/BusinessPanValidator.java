package com.employee.loan_system.businessloan.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BusinessPanValidator implements ConstraintValidator<BusinessPan, String> {

    private static final String PAN_REGEX = "^[A-Z]{5}[0-9]{4}[A-Z]$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return value.matches(PAN_REGEX);
    }
}
