package com.employee.loan_system.businessloan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateLoanApplicationRequest {

    @NotNull(message = "Borrower id is required")
    private Long borrowerId;

    @NotNull(message = "Loan product id is required")
    private Long loanProductId;

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Requested amount must be greater than zero")
    private BigDecimal requestedAmount;

    @NotNull(message = "Requested tenure months is required")
    @Positive(message = "Requested tenure months must be positive")
    private Integer requestedTenureMonths;

    @NotBlank(message = "Purpose is required")
    @Size(max = 200, message = "Purpose must not exceed 200 characters")
    private String purpose;
}
