package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.BorrowerAddressResponse;
import com.employee.loan_system.businessloan.dto.BorrowerResponse;
import com.employee.loan_system.businessloan.dto.CreateBorrowerRequest;
import com.employee.loan_system.businessloan.entity.Borrower;
import com.employee.loan_system.businessloan.entity.BorrowerAddress;
import com.employee.loan_system.businessloan.repository.BorrowerRepository;
import com.employee.loan_system.exception.DuplicateResourceException;
import com.employee.loan_system.exception.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BorrowerService {

    private final BorrowerRepository borrowerRepository;

    public BorrowerService(BorrowerRepository borrowerRepository) {
        this.borrowerRepository = borrowerRepository;
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

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public BorrowerResponse getBorrower(Long borrowerId) {
        return toResponse(borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with id: " + borrowerId)));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public List<BorrowerResponse> searchBorrowers(String businessPan, String businessName) {
        if (businessPan != null && !businessPan.isBlank()) {
            return borrowerRepository.findByBusinessPanIgnoreCase(businessPan.trim())
                    .map(this::toResponse)
                    .stream()
                    .toList();
        }
        if (businessName != null && !businessName.isBlank()) {
            return borrowerRepository.findByLegalBusinessNameContainingIgnoreCase(businessName.trim()).stream()
                    .map(this::toResponse)
                    .toList();
        }
        return borrowerRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private BorrowerResponse toResponse(Borrower borrower) {
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
                .build();
    }
}
