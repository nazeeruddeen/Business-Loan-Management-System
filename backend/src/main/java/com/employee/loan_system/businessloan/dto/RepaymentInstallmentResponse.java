package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.InstallmentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record RepaymentInstallmentResponse(
        Long id,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal openingPrincipal,
        BigDecimal principalDue,
        BigDecimal interestDue,
        BigDecimal principalPaid,
        BigDecimal interestPaid,
        BigDecimal remainingDue,
        InstallmentStatus status,
        LocalDateTime paidAt,
        String remarks
) {
}
