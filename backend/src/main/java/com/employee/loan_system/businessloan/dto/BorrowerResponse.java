package com.employee.loan_system.businessloan.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record BorrowerResponse(
        Long id,
        String legalBusinessName,
        String contactPersonName,
        String businessPan,
        String gstin,
        String email,
        String phoneNumber,
        String industryType,
        BigDecimal annualTurnover,
        BigDecimal monthlyIncome,
        LocalDateTime createdAt,
        List<BorrowerAddressResponse> addresses,
        List<BorrowerDocumentResponse> documents,
        BorrowerKycSummaryResponse kycSummary
) {
}
