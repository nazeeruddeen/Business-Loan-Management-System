package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.BorrowerDocumentStatus;
import com.employee.loan_system.businessloan.entity.BorrowerDocumentType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BorrowerDocumentResponse(
        Long id,
        BorrowerDocumentType documentType,
        BorrowerDocumentStatus documentStatus,
        String fileName,
        String fileReference,
        String uploadedBy,
        LocalDateTime uploadedAt,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String remarks,
        boolean requiredDocument
) {
}
