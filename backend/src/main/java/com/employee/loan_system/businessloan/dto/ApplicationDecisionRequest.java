package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplicationDecisionRequest {

    @NotNull(message = "Decision status is required")
    private ApplicationStatus decisionStatus;

    @NotBlank(message = "Decision remarks are required")
    private String remarks;
}
