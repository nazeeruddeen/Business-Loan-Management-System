package com.employee.loan_system.businessloan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateLoanProductRequest {

    @NotBlank(message = "Product code is required")
    @Size(max = 40, message = "Product code must not exceed 40 characters")
    private String productCode;

    @NotBlank(message = "Product name is required")
    @Size(max = 120, message = "Product name must not exceed 120 characters")
    private String name;

    @NotNull(message = "Minimum amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Minimum amount must be greater than zero")
    private BigDecimal minAmount;

    @NotNull(message = "Maximum amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Maximum amount must be greater than zero")
    private BigDecimal maxAmount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Interest rate must be greater than zero")
    private BigDecimal interestRate;

    @NotNull(message = "Tenure months is required")
    @Positive(message = "Tenure months must be positive")
    private Integer tenureMonths;

    private String eligibilityCriteria;

    private boolean active = true;
}
