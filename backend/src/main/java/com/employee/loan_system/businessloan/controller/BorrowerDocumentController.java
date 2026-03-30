package com.employee.loan_system.businessloan.controller;

import com.employee.loan_system.businessloan.dto.BorrowerDocumentResponse;
import com.employee.loan_system.businessloan.dto.CreateBorrowerDocumentRequest;
import com.employee.loan_system.businessloan.dto.UpdateBorrowerDocumentStatusRequest;
import com.employee.loan_system.businessloan.service.BorrowerDocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/borrowers/{borrowerId}/documents")
public class BorrowerDocumentController {

    private final BorrowerDocumentService borrowerDocumentService;

    public BorrowerDocumentController(BorrowerDocumentService borrowerDocumentService) {
        this.borrowerDocumentService = borrowerDocumentService;
    }

    @PostMapping
    public ResponseEntity<BorrowerDocumentResponse> createDocument(
            @PathVariable Long borrowerId,
            @Valid @RequestBody CreateBorrowerDocumentRequest request) {
        return new ResponseEntity<>(borrowerDocumentService.createDocument(borrowerId, request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BorrowerDocumentResponse>> listDocuments(@PathVariable Long borrowerId) {
        return ResponseEntity.ok(borrowerDocumentService.listDocuments(borrowerId));
    }

    @PatchMapping("/{documentId}/status")
    public ResponseEntity<BorrowerDocumentResponse> updateDocumentStatus(
            @PathVariable Long borrowerId,
            @PathVariable Long documentId,
            @Valid @RequestBody UpdateBorrowerDocumentStatusRequest request) {
        return ResponseEntity.ok(borrowerDocumentService.updateDocumentStatus(borrowerId, documentId, request));
    }
}
