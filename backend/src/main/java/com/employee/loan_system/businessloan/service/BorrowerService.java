package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.BorrowerAddressResponse;
import com.employee.loan_system.businessloan.dto.BorrowerDocumentResponse;
import com.employee.loan_system.businessloan.dto.BorrowerKycSummaryResponse;
import com.employee.loan_system.businessloan.dto.BorrowerResponse;
import com.employee.loan_system.businessloan.dto.CreateBorrowerRequest;
import com.employee.loan_system.businessloan.dto.PagedResponse;
import com.employee.loan_system.businessloan.dto.UpdateBorrowerRequest;
import com.employee.loan_system.businessloan.entity.Borrower;
import com.employee.loan_system.businessloan.entity.BorrowerAddress;
import com.employee.loan_system.businessloan.repository.BorrowerRepository;
import com.employee.loan_system.exception.DuplicateResourceException;
import com.employee.loan_system.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BorrowerService {

    private final BorrowerRepository borrowerRepository;
    private final BorrowerDocumentService borrowerDocumentService;

    public BorrowerService(
            BorrowerRepository borrowerRepository,
            BorrowerDocumentService borrowerDocumentService) {
        this.borrowerRepository = borrowerRepository;
        this.borrowerDocumentService = borrowerDocumentService;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public BorrowerResponse createBorrower(CreateBorrowerRequest request) {
        String normalizedPan = request.getBusinessPan().trim().toUpperCase();
        if (borrowerRepository.existsByBusinessPanIgnoreCase(normalizedPan)) {
            throw new DuplicateResourceException("Borrower already exists for business PAN: " + normalizedPan);
        }

        Borrower borrower = new Borrower();
        borrower.setLegalBusinessName(request.getLegalBusinessName().trim());
        borrower.setContactPersonName(request.getContactPersonName().trim());
        borrower.setBusinessPan(normalizedPan);
        borrower.setGstin(request.getGstin() == null ? null : request.getGstin().trim().toUpperCase());
        borrower.setEmail(request.getEmail().trim().toLowerCase());
        borrower.setPhoneNumber(request.getPhoneNumber().trim());
        borrower.setIndustryType(request.getIndustryType().trim());
        borrower.setAnnualTurnover(request.getAnnualTurnover());
        borrower.setMonthlyIncome(request.getMonthlyIncome());

        for (var addressRequest : request.getAddresses()) {
            BorrowerAddress address = new BorrowerAddress();
            address.setAddressType(addressRequest.getAddressType());
            address.setLineOne(addressRequest.getLineOne().trim());
            address.setLineTwo(addressRequest.getLineTwo() == null ? null : addressRequest.getLineTwo().trim());
            address.setCity(addressRequest.getCity().trim());
            address.setState(addressRequest.getState().trim());
            address.setPostalCode(addressRequest.getPostalCode().trim());
            address.setCountry(addressRequest.getCountry().trim());
            borrower.addAddress(address);
        }

        return toResponse(borrowerRepository.save(borrower));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public BorrowerResponse updateBorrower(Long borrowerId, UpdateBorrowerRequest request) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with id: " + borrowerId));

        borrower.setLegalBusinessName(request.getLegalBusinessName().trim());
        borrower.setContactPersonName(request.getContactPersonName().trim());
        borrower.setGstin(request.getGstin() == null ? null : request.getGstin().trim().toUpperCase());
        borrower.setEmail(request.getEmail().trim().toLowerCase());
        borrower.setPhoneNumber(request.getPhoneNumber().trim());
        borrower.setIndustryType(request.getIndustryType().trim());
        borrower.setAnnualTurnover(request.getAnnualTurnover());
        borrower.setMonthlyIncome(request.getMonthlyIncome());

        borrower.getAddresses().clear();
        for (var addressRequest : request.getAddresses()) {
            BorrowerAddress address = new BorrowerAddress();
            address.setAddressType(addressRequest.getAddressType());
            address.setLineOne(addressRequest.getLineOne().trim());
            address.setLineTwo(addressRequest.getLineTwo() == null ? null : addressRequest.getLineTwo().trim());
            address.setCity(addressRequest.getCity().trim());
            address.setState(addressRequest.getState().trim());
            address.setPostalCode(addressRequest.getPostalCode().trim());
            address.setCountry(addressRequest.getCountry().trim());
            borrower.addAddress(address);
        }

        return toResponse(borrowerRepository.save(borrower));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public BorrowerResponse getBorrower(Long borrowerId) {
        return toResponse(borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with id: " + borrowerId)));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public PagedResponse<BorrowerResponse> searchBorrowers(String businessPan, String businessName, Pageable pageable) {
        if (businessPan != null && !businessPan.isBlank()) {
            List<BorrowerResponse> matches = borrowerRepository.findByBusinessPanIgnoreCase(businessPan.trim())
                    .map(this::toResponse)
                    .stream()
                    .toList();
            return manualPage(matches, pageable);
        }
        if (businessName != null && !businessName.isBlank()) {
            Page<BorrowerResponse> page = borrowerRepository
                    .findByLegalBusinessNameContainingIgnoreCase(businessName.trim(), pageable)
                    .map(this::toResponse);
            return PagedResponse.from(page);
        }
        return PagedResponse.from(borrowerRepository.findAll(pageable).map(this::toResponse));
    }

    private PagedResponse<BorrowerResponse> manualPage(List<BorrowerResponse> items, Pageable pageable) {
        int start = Math.min((int) pageable.getOffset(), items.size());
        int end = Math.min(start + pageable.getPageSize(), items.size());
        Page<BorrowerResponse> page = new PageImpl<>(items.subList(start, end), pageable, items.size());
        return PagedResponse.from(page);
    }

    private BorrowerResponse toResponse(Borrower borrower) {
        BorrowerKycSummaryResponse kycSummary = borrowerDocumentService.getKycSummary(borrower);
        List<BorrowerDocumentResponse> documents = borrower.getDocuments().stream()
                .sorted((left, right) -> {
                    if (left.getUploadedAt() == null && right.getUploadedAt() == null) {
                        return 0;
                    }
                    if (left.getUploadedAt() == null) {
                        return 1;
                    }
                    if (right.getUploadedAt() == null) {
                        return -1;
                    }
                    return right.getUploadedAt().compareTo(left.getUploadedAt());
                })
                .map(document -> BorrowerDocumentResponse.builder()
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
                        .requiredDocument(borrowerDocumentService.isRequiredDocument(borrower, document.getDocumentType()))
                        .build())
                .toList();

        return BorrowerResponse.builder()
                .id(borrower.getId())
                .legalBusinessName(borrower.getLegalBusinessName())
                .contactPersonName(borrower.getContactPersonName())
                .businessPan(borrower.getBusinessPan())
                .gstin(borrower.getGstin())
                .email(borrower.getEmail())
                .phoneNumber(borrower.getPhoneNumber())
                .industryType(borrower.getIndustryType())
                .annualTurnover(borrower.getAnnualTurnover())
                .monthlyIncome(borrower.getMonthlyIncome())
                .createdAt(borrower.getCreatedAt())
                .addresses(borrower.getAddresses().stream()
                        .map(address -> BorrowerAddressResponse.builder()
                                .id(address.getId())
                                .addressType(address.getAddressType())
                                .lineOne(address.getLineOne())
                                .lineTwo(address.getLineTwo())
                                .city(address.getCity())
                                .state(address.getState())
                                .postalCode(address.getPostalCode())
                                .country(address.getCountry())
                                .build())
                        .toList())
                .documents(documents)
                .kycSummary(kycSummary)
                .build();
    }
}
