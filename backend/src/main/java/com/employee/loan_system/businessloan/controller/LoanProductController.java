package com.employee.loan_system.businessloan.controller;

import com.employee.loan_system.businessloan.dto.CreateLoanProductRequest;
import com.employee.loan_system.businessloan.dto.LoanProductResponse;
import com.employee.loan_system.businessloan.service.LoanProductService;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/loan-products")
public class LoanProductController {

    private final LoanProductService loanProductService;

    public LoanProductController(LoanProductService loanProductService) {
        this.loanProductService = loanProductService;
    }

    @PostMapping
    public ResponseEntity<LoanProductResponse> createProduct(@Valid @RequestBody CreateLoanProductRequest request) {
        return new ResponseEntity<>(loanProductService.createProduct(request), HttpStatus.CREATED);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<LoanProductResponse> getProduct(@PathVariable Long productId) {
        return new ResponseEntity<>(loanProductService.getProduct(productId), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<LoanProductResponse>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) Integer maxTenureMonths) {
        return new ResponseEntity<>(
                loanProductService.searchProducts(name, active, amount, maxTenureMonths),
                HttpStatus.OK);
    }
}
