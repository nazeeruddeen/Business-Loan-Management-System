package com.employee.loan_system.businessloan.dto;

import lombok.Builder;

import java.io.Serializable;
import java.math.BigDecimal;

@Builder
public record BusinessLoanDashboardResponse(
        long totalLoanApplications,
        long approvedLoanApplications,
        long disbursedLoanAccounts,
        long activeLoanAccounts,
        long overdueInstallments,
        BigDecimal totalPrincipalDisbursed,
        BigDecimal totalOutstandingPrincipal,
        BigDecimal totalRepaidAmount
) implements Serializable {
}
