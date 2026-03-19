package com.employee.loan_system.businessloan.controller;

import com.employee.loan_system.businessloan.dto.CreateEligibilityRuleRequest;
import com.employee.loan_system.businessloan.dto.EligibilityRuleResponse;
import com.employee.loan_system.businessloan.service.EligibilityRuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/eligibility-rules")
public class EligibilityRuleController {

    private final EligibilityRuleService eligibilityRuleService;

    public EligibilityRuleController(EligibilityRuleService eligibilityRuleService) {
        this.eligibilityRuleService = eligibilityRuleService;
    }

    @PostMapping
    public ResponseEntity<EligibilityRuleResponse> createRule(@Valid @RequestBody CreateEligibilityRuleRequest request) {
        return new ResponseEntity<>(eligibilityRuleService.createRule(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<EligibilityRuleResponse>> listRules() {
        return new ResponseEntity<>(eligibilityRuleService.listActiveRules(), HttpStatus.OK);
    }
}
