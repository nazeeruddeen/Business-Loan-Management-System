package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.ApplicationStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ApplicationStatusHistoryResponse(
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        String remarks,
        String changedBy,
        LocalDateTime changedAt
) {
}
