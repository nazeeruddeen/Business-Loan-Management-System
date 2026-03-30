package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.BorrowerDocumentResponse;
import com.employee.loan_system.businessloan.dto.BorrowerKycSummaryResponse;
import com.employee.loan_system.businessloan.dto.CreateBorrowerDocumentRequest;
import com.employee.loan_system.businessloan.dto.UpdateBorrowerDocumentStatusRequest;
import com.employee.loan_system.businessloan.entity.Borrower;
import com.employee.loan_system.businessloan.entity.BorrowerDocument;
import com.employee.loan_system.businessloan.entity.BorrowerDocumentStatus;
import com.employee.loan_system.businessloan.entity.BorrowerDocumentType;
import com.employee.loan_system.businessloan.repository.BorrowerDocumentRepository;
import com.employee.loan_system.businessloan.repository.BorrowerRepository;
import com.employee.loan_system.exception.BusinessRuleException;
import com.employee.loan_system.exception.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class BorrowerDocumentService {

    private final BorrowerRepository borrowerRepository;
    private final BorrowerDocumentRepository borrowerDocumentRepository;

    public BorrowerDocumentService(
            BorrowerRepository borrowerRepository,
            BorrowerDocumentRepository borrowerDocumentRepository) {
        this.borrowerRepository = borrowerRepository;
        this.borrowerDocumentRepository = borrowerDocumentRepository;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public BorrowerDocumentResponse createDocument(Long borrowerId, CreateBorrowerDocumentRequest request) {
        Borrower borrower = findBorrower(borrowerId);

        BorrowerDocument document = new BorrowerDocument();
        document.setBorrower(borrower);
        document.setDocumentType(request.getDocumentType());
        document.setDocumentStatus(BorrowerDocumentStatus.UPLOADED);
        document.setFileName(request.getFileName().trim());
        document.setFileReference(request.getFileReference().trim());
        document.setUploadedBy(currentActor());
        document.setRemarks(trimToNull(request.getRemarks()));

        return toResponse(borrowerDocumentRepository.save(document), requiredDocumentsFor(borrower).contains(request.getDocumentType()));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public List<BorrowerDocumentResponse> listDocuments(Long borrowerId) {
        Borrower borrower = findBorrower(borrowerId);
        Set<BorrowerDocumentType> requiredDocuments = requiredDocumentsFor(borrower);
        return borrowerDocumentRepository.findByBorrower_IdOrderByUploadedAtDesc(borrowerId).stream()
                .map(document -> toResponse(document, requiredDocuments.contains(document.getDocumentType())))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public BorrowerDocumentResponse updateDocumentStatus(
            Long borrowerId,
            Long documentId,
            UpdateBorrowerDocumentStatusRequest request) {
        BorrowerDocument document = borrowerDocumentRepository.findByIdAndBorrower_Id(documentId, borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower document not found with id: " + documentId));

        document.setDocumentStatus(request.getDocumentStatus());
        document.setRemarks(trimToNull(request.getRemarks()));
        if (request.getDocumentStatus() == BorrowerDocumentStatus.VERIFIED
                || request.getDocumentStatus() == BorrowerDocumentStatus.REJECTED) {
            document.setReviewedBy(currentActor());
            document.setReviewedAt(LocalDateTime.now());
        } else {
            document.setReviewedBy(null);
            document.setReviewedAt(null);
        }

        return toResponse(
                borrowerDocumentRepository.save(document),
                requiredDocumentsFor(document.getBorrower()).contains(document.getDocumentType()));
    }

    @Transactional(readOnly = true)
    public BorrowerKycSummaryResponse getKycSummary(Borrower borrower) {
        Set<BorrowerDocumentType> requiredDocuments = requiredDocumentsFor(borrower);
        List<BorrowerDocument> documents = borrower.getDocuments().stream()
                .sorted(Comparator.comparing(BorrowerDocument::getUploadedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<BorrowerDocumentType> missingRequiredDocuments = requiredDocuments.stream()
                .filter(requiredType -> documents.stream()
                        .noneMatch(document -> document.getDocumentType() == requiredType
                                && document.getDocumentStatus() == BorrowerDocumentStatus.VERIFIED))
                .sorted()
                .toList();

        long verifiedDocumentCount = documents.stream()
                .filter(document -> document.getDocumentStatus() == BorrowerDocumentStatus.VERIFIED)
                .count();

        return BorrowerKycSummaryResponse.builder()
                .kycComplete(missingRequiredDocuments.isEmpty())
                .requiredDocumentCount(requiredDocuments.size())
                .verifiedDocumentCount((int) verifiedDocumentCount)
                .totalDocumentCount(documents.size())
                .missingRequiredDocuments(missingRequiredDocuments)
                .build();
    }

    @Transactional(readOnly = true)
    public void assertKycComplete(Borrower borrower) {
        BorrowerKycSummaryResponse summary = getKycSummary(borrower);
        if (!summary.kycComplete()) {
            throw new BusinessRuleException("KYC is incomplete. Missing verified documents: " + summary.missingRequiredDocuments());
        }
    }

    @Transactional(readOnly = true)
    public boolean isRequiredDocument(Borrower borrower, BorrowerDocumentType documentType) {
        return requiredDocumentsFor(borrower).contains(documentType);
    }

    private Borrower findBorrower(Long borrowerId) {
        return borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with id: " + borrowerId));
    }

    private Set<BorrowerDocumentType> requiredDocumentsFor(Borrower borrower) {
        EnumSet<BorrowerDocumentType> required = EnumSet.of(
                BorrowerDocumentType.PAN_CARD,
                BorrowerDocumentType.BUSINESS_REGISTRATION,
                BorrowerDocumentType.BANK_STATEMENT);
        if (borrower.getGstin() != null && !borrower.getGstin().isBlank()) {
            required.add(BorrowerDocumentType.GST_CERTIFICATE);
        }
        return required;
    }

    private BorrowerDocumentResponse toResponse(BorrowerDocument document, boolean requiredDocument) {
        return BorrowerDocumentResponse.builder()
                .id(document.getId())
                .documentType(document.getDocumentType())
                .documentStatus(document.getDocumentStatus())
                .fileName(document.getFileName())
                .fileReference(document.getFileReference())
                .uploadedBy(document.getUploadedBy())
                .uploadedAt(document.getUploadedAt())
                .reviewedBy(document.getReviewedBy())
                .reviewedAt(document.getReviewedAt())
                .remarks(document.getRemarks())
                .requiredDocument(requiredDocument)
                .build();
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || !authentication.isAuthenticated()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
