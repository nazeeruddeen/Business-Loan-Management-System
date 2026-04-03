package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.CreateEligibilityRuleRequest;
import com.employee.loan_system.businessloan.entity.EligibilityRule;
import com.employee.loan_system.businessloan.entity.EligibilityRuleType;
import com.employee.loan_system.businessloan.repository.EligibilityRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EligibilityRuleServiceTest {

    @Mock
    private EligibilityRuleRepository eligibilityRuleRepository;

    @Test
    void createRuleShouldReturnVersionedResponse() {
        EligibilityRuleService service = new EligibilityRuleService(eligibilityRuleRepository);
        when(eligibilityRuleRepository.existsByRuleCodeIgnoreCase("INCOME_MIN")).thenReturn(false);
        when(eligibilityRuleRepository.save(any(EligibilityRule.class))).thenAnswer(invocation -> {
            EligibilityRule rule = invocation.getArgument(0);
            rule.setId(7L);
            rule.setVersion(0L);
            return rule;
        });

        CreateEligibilityRuleRequest request = new CreateEligibilityRuleRequest();
        request.setRuleCode("income_min");
        request.setRuleExpression("Monthly income should meet policy threshold");
        request.setRuleType(EligibilityRuleType.MIN_VALUE);
        request.setMinValue(new BigDecimal("100000"));
        request.setActive(true);

        var response = service.createRule(request);

        ArgumentCaptor<EligibilityRule> captor = ArgumentCaptor.forClass(EligibilityRule.class);
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.version()).isEqualTo(0L);
        assertThat(response.ruleCode()).isEqualTo("INCOME_MIN");
        assertThat(response.ruleExpression()).isEqualTo("MONTHLY INCOME SHOULD MEET POLICY THRESHOLD");
    }

    @Test
    void listActiveRulesShouldExposeVersionNumbers() {
        EligibilityRuleService service = new EligibilityRuleService(eligibilityRuleRepository);
        EligibilityRule rule = new EligibilityRule();
        rule.setId(1L);
        rule.setVersion(3L);
        rule.setRuleCode("INCOME_MIN");
        rule.setRuleExpression("MONTHLY INCOME SHOULD MEET POLICY THRESHOLD");
        rule.setRuleType(EligibilityRuleType.MIN_VALUE);
        rule.setMinValue(new BigDecimal("100000"));
        rule.setActive(true);
        when(eligibilityRuleRepository.findByActiveTrueOrderByRuleCodeAsc()).thenReturn(List.of(rule));

        var responses = service.listActiveRules();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).version()).isEqualTo(3L);
    }
}
