package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.BorrowerAddressRequest;
import com.employee.loan_system.businessloan.dto.BorrowerKycSummaryResponse;
import com.employee.loan_system.businessloan.dto.CreateBorrowerRequest;
import com.employee.loan_system.businessloan.entity.AddressType;
import com.employee.loan_system.businessloan.entity.Borrower;
import com.employee.loan_system.businessloan.repository.BorrowerRepository;
import com.employee.loan_system.exception.DuplicateResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowerServiceTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private BorrowerDocumentService borrowerDocumentService;

    @InjectMocks
    private BorrowerService borrowerService;

    @Test
    void createBorrowerShouldPersistBorrowerWithAddresses() {
        CreateBorrowerRequest request = buildBorrowerRequest();
        when(borrowerRepository.existsByBusinessPanIgnoreCase("ABCDE1234F")).thenReturn(false);
        when(borrowerRepository.save(any(Borrower.class))).thenAnswer(invocation -> {
            Borrower borrower = invocation.getArgument(0);
            borrower.setId(101L);
            borrower.setCreatedAt(LocalDateTime.now());
            return borrower;
        });
        when(borrowerDocumentService.getKycSummary(any(Borrower.class))).thenReturn(
                BorrowerKycSummaryResponse.builder()
                        .kycComplete(false)
                        .requiredDocumentCount(4)
                        .verifiedDocumentCount(0)
                        .totalDocumentCount(0)
                        .missingRequiredDocuments(List.of())
                        .build());

        var response = borrowerService.createBorrower(request);

        ArgumentCaptor<Borrower> captor = ArgumentCaptor.forClass(Borrower.class);
        verify(borrowerRepository).save(captor.capture());
        Borrower saved = captor.getValue();
        assertThat(saved.getBusinessPan()).isEqualTo("ABCDE1234F");
        assertThat(saved.getAddresses()).hasSize(2);
        assertThat(saved.getAddresses()).allMatch(address -> address.getBorrower() == saved);
        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.addresses()).hasSize(2);
    }

    @Test
    void createBorrowerShouldRejectDuplicatePan() {
        CreateBorrowerRequest request = buildBorrowerRequest();
        when(borrowerRepository.existsByBusinessPanIgnoreCase("ABCDE1234F")).thenReturn(true);

        assertThatThrownBy(() -> borrowerService.createBorrower(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ABCDE1234F");
    }

    private CreateBorrowerRequest buildBorrowerRequest() {
        CreateBorrowerRequest request = new CreateBorrowerRequest();
        request.setLegalBusinessName("Atlas Foods Private Limited");
        request.setContactPersonName("Ravi Kumar");
        request.setBusinessPan("abcde1234f");
        request.setGstin("29ABCDE1234F1Z5");
        request.setEmail("ops@atlasfoods.com");
        request.setPhoneNumber("9876543210");
        request.setIndustryType("Manufacturing");
        request.setAnnualTurnover(new BigDecimal("15000000"));
        request.setMonthlyIncome(new BigDecimal("650000"));

        BorrowerAddressRequest registered = new BorrowerAddressRequest();
        registered.setAddressType(AddressType.REGISTERED);
        registered.setLineOne("12 Industrial Estate");
        registered.setCity("Bengaluru");
        registered.setState("Karnataka");
        registered.setPostalCode("560001");
        registered.setCountry("India");

        BorrowerAddressRequest operational = new BorrowerAddressRequest();
        operational.setAddressType(AddressType.OPERATIONAL);
        operational.setLineOne("44 Export Park");
        operational.setCity("Bengaluru");
        operational.setState("Karnataka");
        operational.setPostalCode("560048");
        operational.setCountry("India");

        request.setAddresses(List.of(registered, operational));
        return request;
    }
}
