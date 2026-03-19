package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.LoanAccountStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record DisbursementReportItem(
        Long accountId,
        String accountNumber,
        Long applicationId,
        String borrowerName,
        String productCode,
        BigDecimal principalAmount,
        BigDecimal outstandingPrincipal,
        LoanAccountStatus status,
        LocalDateTime disbursedAt,
        LocalDate nextDueDate
) {
}
