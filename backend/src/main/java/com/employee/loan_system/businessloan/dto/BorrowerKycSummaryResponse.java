package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.BorrowerDocumentType;
import lombok.Builder;

import java.util.List;

@Builder
public record BorrowerKycSummaryResponse(
        boolean kycComplete,
        int requiredDocumentCount,
        int verifiedDocumentCount,
        int totalDocumentCount,
        List<BorrowerDocumentType> missingRequiredDocuments
) {
}
