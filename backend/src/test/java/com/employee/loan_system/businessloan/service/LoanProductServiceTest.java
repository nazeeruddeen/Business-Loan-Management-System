package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.CreateLoanProductRequest;
import com.employee.loan_system.businessloan.entity.LoanProduct;
import com.employee.loan_system.businessloan.repository.LoanProductRepository;
import com.employee.loan_system.exception.DuplicateResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanProductServiceTest {

    @Mock
    private LoanProductRepository loanProductRepository;

    @InjectMocks
    private LoanProductService loanProductService;

    @Test
    void createProductShouldNormalizeProductCode() {
        CreateLoanProductRequest request = new CreateLoanProductRequest();
        request.setProductCode("bl-term-01");
        request.setName("Business Term Loan");
        request.setMinAmount(new BigDecimal("100000"));
        request.setMaxAmount(new BigDecimal("5000000"));
        request.setInterestRate(new BigDecimal("11.75"));
        request.setTenureMonths(48);
        request.setEligibilityCriteria("{\"minIncome\":500000}");
        request.setActive(true);

        when(loanProductRepository.existsByProductCodeIgnoreCase("BL-TERM-01")).thenReturn(false);
        when(loanProductRepository.save(any(LoanProduct.class))).thenAnswer(invocation -> {
            LoanProduct product = invocation.getArgument(0);
            product.setId(7L);
            product.setCreatedAt(LocalDateTime.now());
            return product;
        });

        var response = loanProductService.createProduct(request);

        ArgumentCaptor<LoanProduct> captor = ArgumentCaptor.forClass(LoanProduct.class);
        verify(loanProductRepository).save(captor.capture());
        assertThat(captor.getValue().getProductCode()).isEqualTo("BL-TERM-01");
        assertThat(response.id()).isEqualTo(7L);
    }

    @Test
    void createProductShouldRejectDuplicateCode() {
        CreateLoanProductRequest request = new CreateLoanProductRequest();
        request.setProductCode("BL-TL-01");
        request.setName("Business Term Loan");
        request.setMinAmount(new BigDecimal("100000"));
        request.setMaxAmount(new BigDecimal("5000000"));
        request.setInterestRate(new BigDecimal("11.75"));
        request.setTenureMonths(48);

        when(loanProductRepository.existsByProductCodeIgnoreCase("BL-TL-01")).thenReturn(true);

        assertThatThrownBy(() -> loanProductService.createProduct(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("BL-TL-01");
    }
}
