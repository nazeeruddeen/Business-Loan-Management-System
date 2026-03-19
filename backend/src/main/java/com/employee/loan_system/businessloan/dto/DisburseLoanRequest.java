package com.employee.loan_system.businessloan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DisburseLoanRequest {

    @NotNull(message = "Disbursement date is required")
    private LocalDate disbursementDate;

    @NotBlank(message = "Disbursement reference is required")
    private String disbursementReference;
}
