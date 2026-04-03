package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.CreateEligibilityRuleRequest;
import com.employee.loan_system.businessloan.dto.EligibilityRuleResponse;
import com.employee.loan_system.businessloan.entity.EligibilityRule;
import com.employee.loan_system.businessloan.repository.EligibilityRuleRepository;
import com.employee.loan_system.exception.DuplicateResourceException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EligibilityRuleService {

    private final EligibilityRuleRepository eligibilityRuleRepository;

    public EligibilityRuleService(EligibilityRuleRepository eligibilityRuleRepository) {
        this.eligibilityRuleRepository = eligibilityRuleRepository;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public EligibilityRuleResponse createRule(CreateEligibilityRuleRequest request) {
        String normalizedCode = request.getRuleCode().trim().toUpperCase();
        if (eligibilityRuleRepository.existsByRuleCodeIgnoreCase(normalizedCode)) {
            throw new DuplicateResourceException("Eligibility rule already exists with code: " + normalizedCode);
        }

        EligibilityRule rule = new EligibilityRule();
        rule.setRuleCode(normalizedCode);
        rule.setRuleExpression(request.getRuleExpression().trim().toUpperCase());
        rule.setRuleType(request.getRuleType());
        rule.setMinValue(request.getMinValue());
        rule.setMaxValue(request.getMaxValue());
        rule.setRuleValueText(request.getRuleValueText() == null ? null : request.getRuleValueText().trim());
        rule.setActive(request.isActive());

        return toResponse(eligibilityRuleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public List<EligibilityRuleResponse> listActiveRules() {
        return eligibilityRuleRepository.findByActiveTrueOrderByRuleCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private EligibilityRuleResponse toResponse(EligibilityRule rule) {
        return EligibilityRuleResponse.builder()
                .id(rule.getId())
                .version(rule.getVersion())
                .ruleCode(rule.getRuleCode())
                .ruleExpression(rule.getRuleExpression())
                .ruleType(rule.getRuleType())
                .minValue(rule.getMinValue())
                .maxValue(rule.getMaxValue())
                .ruleValueText(rule.getRuleValueText())
                .active(rule.isActive())
                .build();
    }
}
