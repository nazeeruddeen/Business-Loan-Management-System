package com.employee.loan_system.businessloan.dto;

import lombok.Builder;

@Builder
public record RuleEvaluationResponse(
        String ruleCode,
        String ruleExpression,
        boolean passed,
        String message
) {
}
