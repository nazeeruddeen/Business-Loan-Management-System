package com.employee.loan_system.businessloan.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = BusinessPanValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessPan {
    String message() default "Business PAN must be a valid PAN value";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
