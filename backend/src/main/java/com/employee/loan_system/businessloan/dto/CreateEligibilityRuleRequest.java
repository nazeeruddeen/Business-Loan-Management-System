package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.EligibilityRuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateEligibilityRuleRequest {

    @NotBlank(message = "Rule code is required")
    @Size(max = 60, message = "Rule code must not exceed 60 characters")
    private String ruleCode;

    @NotBlank(message = "Rule expression is required")
    @Size(max = 120, message = "Rule expression must not exceed 120 characters")
    private String ruleExpression;

    @NotNull(message = "Rule type is required")
    private EligibilityRuleType ruleType;

    private BigDecimal minValue;

    private BigDecimal maxValue;

    @Size(max = 120, message = "Rule text value must not exceed 120 characters")
    private String ruleValueText;

    private boolean active = true;
}
