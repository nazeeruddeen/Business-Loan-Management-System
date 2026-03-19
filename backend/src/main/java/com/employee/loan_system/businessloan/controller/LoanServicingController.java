package com.employee.loan_system.businessloan.controller;

import com.employee.loan_system.businessloan.dto.BusinessLoanDashboardResponse;
import com.employee.loan_system.businessloan.dto.LoanAccountResponse;
import com.employee.loan_system.businessloan.dto.LoanRepaymentTransactionResponse;
import com.employee.loan_system.businessloan.dto.RecordRepaymentRequest;
import com.employee.loan_system.businessloan.service.LoanServicingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loan-accounts")
public class LoanServicingController {

    private final LoanServicingService loanServicingService;

    public LoanServicingController(LoanServicingService loanServicingService) {
        this.loanServicingService = loanServicingService;
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<LoanAccountResponse> getByAccountNumber(@PathVariable String accountNumber) {
        return new ResponseEntity<>(loanServicingService.getByAccountNumber(accountNumber), HttpStatus.OK);
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<LoanAccountResponse> getByApplicationId(@PathVariable Long applicationId) {
        return new ResponseEntity<>(loanServicingService.getByApplicationId(applicationId), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<LoanAccountResponse>> listAccounts() {
        return new ResponseEntity<>(loanServicingService.listAccounts(), HttpStatus.OK);
    }

    @PostMapping("/{accountId}/repayments")
    public ResponseEntity<LoanRepaymentTransactionResponse> recordRepayment(
            @PathVariable Long accountId,
            @Valid @RequestBody RecordRepaymentRequest request) {
        return new ResponseEntity<>(loanServicingService.recordRepayment(accountId, request), HttpStatus.CREATED);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<BusinessLoanDashboardResponse> dashboard() {
        return new ResponseEntity<>(loanServicingService.getDashboard(), HttpStatus.OK);
    }
}
