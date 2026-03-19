package com.employee.loan_system.businessloan.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record LoanProductResponse(
        Long id,
        String productCode,
        String name,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal interestRate,
        Integer tenureMonths,
        String eligibilityCriteria,
        boolean active,
        LocalDateTime createdAt
) {
}
