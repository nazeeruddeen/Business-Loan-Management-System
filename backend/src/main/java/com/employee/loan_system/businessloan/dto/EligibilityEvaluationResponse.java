package com.employee.loan_system.businessloan.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record EligibilityEvaluationResponse(
        boolean eligible,
        String summary,
        List<RuleEvaluationResponse> ruleResults
) {
}
