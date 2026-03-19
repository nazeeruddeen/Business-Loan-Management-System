package com.employee.loan_system.businessloan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EvaluateEligibilityRequest {

    @NotNull(message = "Borrower id is required")
    private Long borrowerId;

    @NotNull(message = "Loan product id is required")
    private Long loanProductId;

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Requested amount must be greater than zero")
    private BigDecimal requestedAmount;

    @NotNull(message = "Requested tenure is required")
    @Positive(message = "Requested tenure must be positive")
    private Integer requestedTenureMonths;
}
