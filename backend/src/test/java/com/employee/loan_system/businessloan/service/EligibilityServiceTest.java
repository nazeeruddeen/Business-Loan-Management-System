package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.EvaluateEligibilityRequest;
import com.employee.loan_system.businessloan.entity.Borrower;
import com.employee.loan_system.businessloan.entity.EligibilityRule;
import com.employee.loan_system.businessloan.entity.EligibilityRuleType;
import com.employee.loan_system.businessloan.entity.LoanProduct;
import com.employee.loan_system.businessloan.repository.BorrowerRepository;
import com.employee.loan_system.businessloan.repository.EligibilityRuleRepository;
import com.employee.loan_system.businessloan.repository.LoanProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private LoanProductRepository loanProductRepository;

    @Mock
    private EligibilityRuleRepository eligibilityRuleRepository;

    @InjectMocks
    private EligibilityService eligibilityService;

    @Test
    void evaluateShouldPassWhenRulesAreSatisfied() {
        Borrower borrower = borrower("Manufacturing", "700000", "20000000");
        LoanProduct product = product("100000", "5000000", 48);
        EligibilityRule rule = new EligibilityRule();
        rule.setRuleCode("MIN_MONTHLY_INCOME");
        rule.setRuleExpression("BORROWER_MONTHLY_INCOME");
        rule.setRuleType(EligibilityRuleType.MIN_VALUE);
        rule.setMinValue(new BigDecimal("500000"));
        rule.setActive(true);

        when(borrowerRepository.findById(1L)).thenReturn(Optional.of(borrower));
        when(loanProductRepository.findById(2L)).thenReturn(Optional.of(product));
        when(eligibilityRuleRepository.findByActiveTrueOrderByRuleCodeAsc()).thenReturn(List.of(rule));

        EvaluateEligibilityRequest request = new EvaluateEligibilityRequest();
        request.setBorrowerId(1L);
        request.setLoanProductId(2L);
        request.setRequestedAmount(new BigDecimal("1200000"));
        request.setRequestedTenureMonths(24);

        var response = eligibilityService.evaluate(request);

        assertThat(response.eligible()).isTrue();
        assertThat(response.ruleResults()).hasSize(3);
    }

    @Test
    void evaluateShouldFailWhenRuleFails() {
        Borrower borrower = borrower("Manufacturing", "100000", "20000000");
        LoanProduct product = product("100000", "5000000", 48);
        EligibilityRule rule = new EligibilityRule();
        rule.setRuleCode("MIN_MONTHLY_INCOME");
        rule.setRuleExpression("BORROWER_MONTHLY_INCOME");
        rule.setRuleType(EligibilityRuleType.MIN_VALUE);
        rule.setMinValue(new BigDecimal("500000"));
        rule.setActive(true);

        when(borrowerRepository.findById(1L)).thenReturn(Optional.of(borrower));
        when(loanProductRepository.findById(2L)).thenReturn(Optional.of(product));
        when(eligibilityRuleRepository.findByActiveTrueOrderByRuleCodeAsc()).thenReturn(List.of(rule));

        EvaluateEligibilityRequest request = new EvaluateEligibilityRequest();
        request.setBorrowerId(1L);
        request.setLoanProductId(2L);
        request.setRequestedAmount(new BigDecimal("1200000"));
        request.setRequestedTenureMonths(24);

        var response = eligibilityService.evaluate(request);

        assertThat(response.eligible()).isFalse();
        assertThat(response.summary()).contains("MIN_MONTHLY_INCOME");
    }

    private Borrower borrower(String industryType, String monthlyIncome, String annualTurnover) {
        Borrower borrower = new Borrower();
        borrower.setId(1L);
        borrower.setIndustryType(industryType);
        borrower.setMonthlyIncome(new BigDecimal(monthlyIncome));
        borrower.setAnnualTurnover(new BigDecimal(annualTurnover));
        return borrower;
    }

    private LoanProduct product(String minAmount, String maxAmount, int tenure) {
        LoanProduct product = new LoanProduct();
        product.setId(2L);
        product.setProductCode("BL-TERM");
        product.setMinAmount(new BigDecimal(minAmount));
        product.setMaxAmount(new BigDecimal(maxAmount));
        product.setTenureMonths(tenure);
        return product;
    }
}
