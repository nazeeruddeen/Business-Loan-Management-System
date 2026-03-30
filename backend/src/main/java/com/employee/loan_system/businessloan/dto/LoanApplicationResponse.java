package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.ApplicationStatus;
import com.employee.loan_system.businessloan.entity.BorrowerDocumentType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record LoanApplicationResponse(
        Long id,
        Long borrowerId,
        String borrowerName,
        Long loanProductId,
        String loanProductCode,
        BigDecimal requestedAmount,
        Integer requestedTenureMonths,
        String purpose,
        ApplicationStatus status,
        boolean eligibilityPassed,
        String eligibilitySummary,
        String reviewerUsername,
        boolean borrowerKycComplete,
        List<BorrowerDocumentType> missingRequiredDocuments,
        LocalDateTime submittedAt,
        LocalDateTime decisionedAt,
        LocalDateTime disbursedAt,
        String decisionRemarks,
        List<ApplicationStatusHistoryResponse> history
) {
}
