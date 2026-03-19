package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.EligibilityEvaluationResponse;
import com.employee.loan_system.businessloan.dto.EvaluateEligibilityRequest;
import com.employee.loan_system.businessloan.dto.RuleEvaluationResponse;
import com.employee.loan_system.businessloan.entity.Borrower;
import com.employee.loan_system.businessloan.entity.EligibilityRule;
import com.employee.loan_system.businessloan.entity.LoanProduct;
import com.employee.loan_system.businessloan.repository.BorrowerRepository;
import com.employee.loan_system.businessloan.repository.EligibilityRuleRepository;
import com.employee.loan_system.businessloan.repository.LoanProductRepository;
import com.employee.loan_system.exception.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class EligibilityService {

    private final BorrowerRepository borrowerRepository;
    private final LoanProductRepository loanProductRepository;
    private final EligibilityRuleRepository eligibilityRuleRepository;

    public EligibilityService(
            BorrowerRepository borrowerRepository,
            LoanProductRepository loanProductRepository,
            EligibilityRuleRepository eligibilityRuleRepository) {
        this.borrowerRepository = borrowerRepository;
        this.loanProductRepository = loanProductRepository;
        this.eligibilityRuleRepository = eligibilityRuleRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER','BORROWER')")
    public EligibilityEvaluationResponse evaluate(EvaluateEligibilityRequest request) {
        Borrower borrower = borrowerRepository.findById(request.getBorrowerId())
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with id: " + request.getBorrowerId()));
        LoanProduct product = loanProductRepository.findById(request.getLoanProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found with id: " + request.getLoanProductId()));
        return evaluate(borrower, product, request.getRequestedAmount(), request.getRequestedTenureMonths());
    }

    @Transactional(readOnly = true)
    public EligibilityEvaluationResponse evaluate(Borrower borrower, LoanProduct product, BigDecimal requestedAmount, Integer requestedTenureMonths) {
        List<RuleEvaluationResponse> results = new ArrayList<>();

        results.add(checkRange("PRODUCT_AMOUNT_RANGE", "Requested amount should be within product range",
                requestedAmount, product.getMinAmount(), product.getMaxAmount()));
        results.add(checkMax("PRODUCT_MAX_TENURE", "Requested tenure should be within product tenure",
                BigDecimal.valueOf(requestedTenureMonths), BigDecimal.ZERO, BigDecimal.valueOf(product.getTenureMonths())));

        for (EligibilityRule rule : eligibilityRuleRepository.findByActiveTrueOrderByRuleCodeAsc()) {
            results.add(evaluateRule(rule, borrower, requestedAmount, requestedTenureMonths));
        }

        boolean eligible = results.stream().allMatch(RuleEvaluationResponse::passed);
        String summary = eligible
                ? "Eligibility checks passed"
                : results.stream()
                        .filter(result -> !result.passed())
                        .map(result -> result.ruleCode() + ": " + result.message())
                        .reduce((left, right) -> left + "; " + right)
                        .orElse("Eligibility checks failed");

        return EligibilityEvaluationResponse.builder()
                .eligible(eligible)
                .summary(summary)
                .ruleResults(results)
                .build();
    }

    private RuleEvaluationResponse evaluateRule(
            EligibilityRule rule,
            Borrower borrower,
            BigDecimal requestedAmount,
            Integer requestedTenureMonths) {
        return switch (rule.getRuleExpression()) {
            case "BORROWER_MONTHLY_INCOME" -> checkMin(
                    rule.getRuleCode(),
                    "Borrower monthly income should meet the minimum threshold",
                    borrower.getMonthlyIncome(),
                    rule.getMinValue());
            case "BORROWER_ANNUAL_TURNOVER" -> checkMin(
                    rule.getRuleCode(),
                    "Borrower annual turnover should meet the minimum threshold",
                    borrower.getAnnualTurnover(),
                    rule.getMinValue());
            case "REQUESTED_AMOUNT" -> checkRange(
                    rule.getRuleCode(),
                    "Requested amount should be within configured rule range",
                    requestedAmount,
                    rule.getMinValue(),
                    rule.getMaxValue());
            case "REQUESTED_TENURE_MONTHS" -> checkRange(
                    rule.getRuleCode(),
                    "Requested tenure should be within configured range",
                    BigDecimal.valueOf(requestedTenureMonths),
                    rule.getMinValue(),
                    rule.getMaxValue());
            case "INDUSTRY_TYPE" -> {
                boolean passed = borrower.getIndustryType() != null
                        && rule.getRuleValueText() != null
                        && borrower.getIndustryType().trim().equalsIgnoreCase(rule.getRuleValueText().trim());
                yield RuleEvaluationResponse.builder()
                        .ruleCode(rule.getRuleCode())
                        .ruleExpression(rule.getRuleExpression())
                        .passed(passed)
                        .message(passed ? "Industry type matched" : "Industry type did not match required criteria")
                        .build();
            }
            default -> RuleEvaluationResponse.builder()
                    .ruleCode(rule.getRuleCode())
                    .ruleExpression(rule.getRuleExpression())
                    .passed(true)
                    .message("Rule expression not recognized, skipped")
                    .build();
        };
    }

    private RuleEvaluationResponse checkMin(String code, String expression, BigDecimal actual, BigDecimal min) {
        boolean passed = min == null || (actual != null && actual.compareTo(min) >= 0);
        return RuleEvaluationResponse.builder()
                .ruleCode(code)
                .ruleExpression(expression)
                .passed(passed)
                .message(passed ? "Passed" : "Expected minimum value " + min)
                .build();
    }

    private RuleEvaluationResponse checkMax(String code, String expression, BigDecimal actual, BigDecimal ignoredMin, BigDecimal max) {
        boolean passed = max == null || (actual != null && actual.compareTo(max) <= 0);
        return RuleEvaluationResponse.builder()
                .ruleCode(code)
                .ruleExpression(expression)
                .passed(passed)
                .message(passed ? "Passed" : "Exceeded maximum value " + max)
                .build();
    }

    private RuleEvaluationResponse checkRange(String code, String expression, BigDecimal actual, BigDecimal min, BigDecimal max) {
        boolean minOk = min == null || (actual != null && actual.compareTo(min) >= 0);
        boolean maxOk = max == null || (actual != null && actual.compareTo(max) <= 0);
        boolean passed = minOk && maxOk;
        return RuleEvaluationResponse.builder()
                .ruleCode(code)
                .ruleExpression(expression)
                .passed(passed)
                .message(passed ? "Passed" : "Expected value between " + min + " and " + max)
                .build();
    }
}
