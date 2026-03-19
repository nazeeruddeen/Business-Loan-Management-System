package com.employee.loan_system.businessloan.controller;

import com.employee.loan_system.businessloan.dto.ApplicationDecisionRequest;
import com.employee.loan_system.businessloan.dto.AssignReviewerRequest;
import com.employee.loan_system.businessloan.dto.CreateLoanApplicationRequest;
import com.employee.loan_system.businessloan.dto.DisburseLoanRequest;
import com.employee.loan_system.businessloan.dto.LoanApplicationResponse;
import com.employee.loan_system.businessloan.entity.ApplicationStatus;
import com.employee.loan_system.businessloan.service.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loan-applications")
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    public LoanApplicationController(LoanApplicationService loanApplicationService) {
        this.loanApplicationService = loanApplicationService;
    }

    @PostMapping
    public ResponseEntity<LoanApplicationResponse> createDraft(@Valid @RequestBody CreateLoanApplicationRequest request) {
        return new ResponseEntity<>(loanApplicationService.createDraft(request), HttpStatus.CREATED);
    }

    @PostMapping("/{applicationId}/submit")
    public ResponseEntity<LoanApplicationResponse> submit(@PathVariable Long applicationId) {
        return new ResponseEntity<>(loanApplicationService.submit(applicationId), HttpStatus.OK);
    }

    @PostMapping("/{applicationId}/assign-reviewer")
    public ResponseEntity<LoanApplicationResponse> assignReviewer(
            @PathVariable Long applicationId,
            @Valid @RequestBody AssignReviewerRequest request) {
        return new ResponseEntity<>(loanApplicationService.assignReviewer(applicationId, request), HttpStatus.OK);
    }

    @PostMapping("/{applicationId}/decision")
    public ResponseEntity<LoanApplicationResponse> decide(
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationDecisionRequest request) {
        return new ResponseEntity<>(loanApplicationService.decide(applicationId, request), HttpStatus.OK);
    }

    @PostMapping("/{applicationId}/disburse")
    public ResponseEntity<LoanApplicationResponse> disburse(
            @PathVariable Long applicationId,
            @Valid @RequestBody DisburseLoanRequest request) {
        return new ResponseEntity<>(loanApplicationService.disburse(applicationId, request), HttpStatus.OK);
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<LoanApplicationResponse> getApplication(@PathVariable Long applicationId) {
        return new ResponseEntity<>(loanApplicationService.getApplication(applicationId), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<LoanApplicationResponse>> listApplications(
            @RequestParam(required = false) ApplicationStatus status) {
        return new ResponseEntity<>(loanApplicationService.listApplications(status), HttpStatus.OK);
    }
}
