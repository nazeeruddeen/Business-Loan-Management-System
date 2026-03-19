package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.PaymentMode;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record LoanRepaymentTransactionResponse(
        Long id,
        String transactionReference,
        BigDecimal amount,
        BigDecimal appliedPrincipalAmount,
        BigDecimal appliedInterestAmount,
        PaymentMode paymentMode,
        LocalDate paymentDate,
        String notes,
        String recordedBy,
        LocalDateTime recordedAt
) {
}
