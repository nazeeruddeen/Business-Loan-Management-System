package com.employee.loan_system.businessloan.controller;

import com.employee.loan_system.businessloan.dto.EligibilityEvaluationResponse;
import com.employee.loan_system.businessloan.dto.EvaluateEligibilityRequest;
import com.employee.loan_system.businessloan.service.EligibilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/eligibility")
public class EligibilityController {

    private final EligibilityService eligibilityService;

    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<EligibilityEvaluationResponse> evaluate(@Valid @RequestBody EvaluateEligibilityRequest request) {
        return new ResponseEntity<>(eligibilityService.evaluate(request), HttpStatus.OK);
    }
}
