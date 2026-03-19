package com.employee.loan_system.businessloan.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record DisbursementReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long disbursedCount,
        BigDecimal totalPrincipalDisbursed,
        BigDecimal totalOutstandingPrincipal,
        List<DisbursementReportItem> items
) {
}
