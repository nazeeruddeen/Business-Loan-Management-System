package com.employee.loan_system.businessloan.controller;

import com.employee.loan_system.businessloan.dto.BorrowerResponse;
import com.employee.loan_system.businessloan.dto.CreateBorrowerRequest;
import com.employee.loan_system.businessloan.dto.PagedResponse;
import com.employee.loan_system.businessloan.dto.UpdateBorrowerRequest;
import com.employee.loan_system.businessloan.service.BorrowerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/borrowers")
public class BorrowerController {

    private final BorrowerService borrowerService;

    public BorrowerController(BorrowerService borrowerService) {
        this.borrowerService = borrowerService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public ResponseEntity<BorrowerResponse> createBorrower(@Valid @RequestBody CreateBorrowerRequest request) {
        return new ResponseEntity<>(borrowerService.createBorrower(request), HttpStatus.CREATED);
    }

    @PutMapping("/{borrowerId}")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public ResponseEntity<BorrowerResponse> updateBorrower(
            @PathVariable Long borrowerId,
            @Valid @RequestBody UpdateBorrowerRequest request) {
        return new ResponseEntity<>(borrowerService.updateBorrower(borrowerId, request), HttpStatus.OK);
    }

    @GetMapping("/{borrowerId}")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<BorrowerResponse> getBorrower(@PathVariable Long borrowerId) {
        return new ResponseEntity<>(borrowerService.getBorrower(borrowerId), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<PagedResponse<BorrowerResponse>> searchBorrowers(
            @RequestParam(required = false) String businessPan,
            @RequestParam(required = false) String businessName,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return new ResponseEntity<>(borrowerService.searchBorrowers(businessPan, businessName, pageable), HttpStatus.OK);
    }
}
