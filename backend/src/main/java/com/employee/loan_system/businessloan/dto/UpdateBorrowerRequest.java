package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.validation.Gstin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateBorrowerRequest {

    @NotBlank(message = "Legal business name is required")
    @Size(max = 150, message = "Legal business name must not exceed 150 characters")
    private String legalBusinessName;

    @NotBlank(message = "Contact person name is required")
    @Size(max = 120, message = "Contact person name must not exceed 120 characters")
    private String contactPersonName;

    @Gstin
    private String gstin;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    @NotBlank(message = "Industry type is required")
    @Size(max = 80, message = "Industry type must not exceed 80 characters")
    private String industryType;

    @NotNull(message = "Annual turnover is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Annual turnover must be greater than zero")
    private BigDecimal annualTurnover;

    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly income must be greater than zero")
    private BigDecimal monthlyIncome;

    @Valid
    @NotEmpty(message = "At least one borrower address is required")
    private List<BorrowerAddressRequest> addresses;
}
