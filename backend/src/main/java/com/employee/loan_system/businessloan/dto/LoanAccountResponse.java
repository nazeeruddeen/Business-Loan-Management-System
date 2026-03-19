package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.LoanAccountStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record LoanAccountResponse(
        Long id,
        Long applicationId,
        String accountNumber,
        String borrowerName,
        String productCode,
        BigDecimal principalAmount,
        BigDecimal annualInterestRate,
        Integer tenureMonths,
        BigDecimal monthlyInstallmentAmount,
        BigDecimal outstandingPrincipal,
        String disbursementReference,
        LoanAccountStatus status,
        LocalDateTime disbursedAt,
        LocalDate nextDueDate,
        List<RepaymentInstallmentResponse> installments,
        List<LoanRepaymentTransactionResponse> transactions
) {
}
