package com.employee.loan_system.businessloan.controller;

import com.employee.loan_system.businessloan.dto.BorrowerResponse;
import com.employee.loan_system.businessloan.dto.CreateBorrowerRequest;
import com.employee.loan_system.businessloan.service.BorrowerService;
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
@RequestMapping("/api/v1/borrowers")
public class BorrowerController {

    private final BorrowerService borrowerService;

    public BorrowerController(BorrowerService borrowerService) {
        this.borrowerService = borrowerService;
    }

    @PostMapping
    public ResponseEntity<BorrowerResponse> createBorrower(@Valid @RequestBody CreateBorrowerRequest request) {
        return new ResponseEntity<>(borrowerService.createBorrower(request), HttpStatus.CREATED);
    }

    @GetMapping("/{borrowerId}")
    public ResponseEntity<BorrowerResponse> getBorrower(@PathVariable Long borrowerId) {
        return new ResponseEntity<>(borrowerService.getBorrower(borrowerId), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<BorrowerResponse>> searchBorrowers(
            @RequestParam(required = false) String businessPan,
            @RequestParam(required = false) String businessName) {
        return new ResponseEntity<>(borrowerService.searchBorrowers(businessPan, businessName), HttpStatus.OK);
    }
}
