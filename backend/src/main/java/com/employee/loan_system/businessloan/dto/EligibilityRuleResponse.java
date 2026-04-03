package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.EligibilityRuleType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record EligibilityRuleResponse(
        Long id,
        Long version,
        String ruleCode,
        String ruleExpression,
        EligibilityRuleType ruleType,
        BigDecimal minValue,
        BigDecimal maxValue,
        String ruleValueText,
        boolean active
) {
}
