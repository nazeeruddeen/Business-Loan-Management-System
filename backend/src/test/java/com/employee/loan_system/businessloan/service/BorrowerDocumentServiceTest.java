package com.employee.loan_system.businessloan.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowerDocumentServiceTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private BorrowerDocumentRepository borrowerDocumentRepository;

    @InjectMocks
    private BorrowerDocumentService borrowerDocumentService;

    @Test
    void createDocumentShouldPersistUploadedDocumentMetadata() {
        Borrower borrower = borrowerWithGstin();
        CreateBorrowerDocumentRequest request = new CreateBorrowerDocumentRequest();
        request.setDocumentType(BorrowerDocumentType.PAN_CARD);
        request.setFileName("pan-card.pdf");
        request.setFileReference("kyc/borrower-101/pan-card.pdf");
        request.setRemarks("Primary business PAN proof");

        when(borrowerRepository.findById(101L)).thenReturn(Optional.of(borrower));
        when(borrowerDocumentRepository.save(any(BorrowerDocument.class))).thenAnswer(invocation -> {
            BorrowerDocument document = invocation.getArgument(0);
            document.setId(11L);
            document.setUploadedAt(LocalDateTime.now());
            return document;
        });

        var response = borrowerDocumentService.createDocument(101L, request);

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.documentStatus()).isEqualTo(BorrowerDocumentStatus.UPLOADED);
        assertThat(response.requiredDocument()).isTrue();
    }

    @Test
    void getKycSummaryShouldDetectMissingRequiredDocuments() {
        Borrower borrower = borrowerWithGstin();
        borrower.setDocuments(new ArrayList<>());

        BorrowerDocument pan = verifiedDocument(borrower, BorrowerDocumentType.PAN_CARD);
        BorrowerDocument registration = verifiedDocument(borrower, BorrowerDocumentType.BUSINESS_REGISTRATION);
        borrower.getDocuments().add(pan);
        borrower.getDocuments().add(registration);

        BorrowerKycSummaryResponse summary = borrowerDocumentService.getKycSummary(borrower);

        assertThat(summary.kycComplete()).isFalse();
        assertThat(summary.missingRequiredDocuments()).containsExactlyInAnyOrder(
                BorrowerDocumentType.BANK_STATEMENT,
                BorrowerDocumentType.GST_CERTIFICATE
        );
    }

    @Test
    void assertKycCompleteShouldFailWhenVerifiedDocumentsAreMissing() {
        Borrower borrower = borrowerWithGstin();
        borrower.setDocuments(new ArrayList<>());

        assertThatThrownBy(() -> borrowerDocumentService.assertKycComplete(borrower))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("KYC is incomplete");
    }

    @Test
    void updateDocumentStatusShouldSetReviewMetadata() {
        Borrower borrower = borrowerWithGstin();
        BorrowerDocument document = new BorrowerDocument();
        document.setId(21L);
        document.setBorrower(borrower);
        document.setDocumentType(BorrowerDocumentType.GST_CERTIFICATE);
        document.setDocumentStatus(BorrowerDocumentStatus.UPLOADED);
        document.setFileName("gst.pdf");
        document.setFileReference("kyc/gst.pdf");
        document.setUploadedBy("officer");

        UpdateBorrowerDocumentStatusRequest request = new UpdateBorrowerDocumentStatusRequest();
        request.setDocumentStatus(BorrowerDocumentStatus.VERIFIED);
        request.setRemarks("Checked and verified");

        when(borrowerDocumentRepository.findByIdAndBorrower_Id(21L, 101L)).thenReturn(Optional.of(document));
        when(borrowerDocumentRepository.save(any(BorrowerDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = borrowerDocumentService.updateDocumentStatus(101L, 21L, request);

        assertThat(response.documentStatus()).isEqualTo(BorrowerDocumentStatus.VERIFIED);
        assertThat(response.reviewedAt()).isNotNull();
    }

    private Borrower borrowerWithGstin() {
        Borrower borrower = new Borrower();
        borrower.setId(101L);
        borrower.setGstin("29ABCDE1234F1Z5");
        borrower.setDocuments(new ArrayList<>());
        return borrower;
    }

    private BorrowerDocument verifiedDocument(Borrower borrower, BorrowerDocumentType type) {
        BorrowerDocument document = new BorrowerDocument();
        document.setBorrower(borrower);
        document.setDocumentType(type);
        document.setDocumentStatus(BorrowerDocumentStatus.VERIFIED);
        document.setUploadedAt(LocalDateTime.now());
        return document;
    }
}
