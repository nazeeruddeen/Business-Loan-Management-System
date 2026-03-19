package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.entity.Borrower;
import com.employee.loan_system.businessloan.entity.LoanAccount;
import com.employee.loan_system.businessloan.entity.LoanAccountStatus;
import com.employee.loan_system.businessloan.entity.LoanApplication;
import com.employee.loan_system.businessloan.entity.LoanProduct;
import com.employee.loan_system.businessloan.repository.LoanAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanReportServiceTest {

    @Mock
    private LoanAccountRepository loanAccountRepository;

    @InjectMocks
    private LoanReportService loanReportService;

    @Test
    void getDisbursementReportShouldReturnPageAndTotals() {
        LoanAccount account = sampleAccount();
        Page<LoanAccount> page = new PageImpl<>(List.of(account), PageRequest.of(0, 10), 1);

        when(loanAccountRepository.findByDisbursedAtBetweenOrderByDisbursedAtDesc(any(), any(), any())).thenReturn(page);
        when(loanAccountRepository.countByDisbursedAtBetween(any(), any())).thenReturn(1L);
        when(loanAccountRepository.sumPrincipalDisbursedBetween(any(), any())).thenReturn(new BigDecimal("1000000.00"));
        when(loanAccountRepository.sumOutstandingPrincipalBetween(any(), any())).thenReturn(new BigDecimal("900000.00"));

        var response = loanReportService.getDisbursementReport(LocalDate.now().minusDays(1), LocalDate.now(), PageRequest.of(0, 10));

        assertThat(response.totalElements()).isEqualTo(1L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.totalPrincipalDisbursed()).isEqualByComparingTo("1000000.00");
    }

    @Test
    void exportDisbursementCsvShouldWriteHeaderAndRow() throws Exception {
        LoanAccount account = sampleAccount();
        when(loanAccountRepository.findByDisbursedAtBetweenOrderByDisbursedAtDesc(any(), any()))
                .thenReturn(List.of(account));

        StreamingResponseBody body = loanReportService.exportDisbursementCsv(LocalDate.now().minusDays(1), LocalDate.now());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.writeTo(out);

        String csv = out.toString();
        assertThat(csv).contains("accountNumber,applicationId,borrowerName,productCode");
        assertThat(csv).contains("BLA-000001");
    }

    private LoanAccount sampleAccount() {
        Borrower borrower = new Borrower();
        borrower.setId(1L);
        borrower.setLegalBusinessName("Atlas Foods");

        LoanProduct product = new LoanProduct();
        product.setId(2L);
        product.setProductCode("BL-TERM");

        LoanApplication application = new LoanApplication();
        application.setId(10L);
        application.setBorrower(borrower);
        application.setLoanProduct(product);

        LoanAccount account = new LoanAccount();
        account.setId(100L);
        account.setLoanApplication(application);
        account.setAccountNumber("BLA-000001");
        account.setPrincipalAmount(new BigDecimal("1000000.00"));
        account.setOutstandingPrincipal(new BigDecimal("900000.00"));
        account.setStatus(LoanAccountStatus.ACTIVE);
        account.setDisbursedAt(LocalDateTime.of(2026, 3, 1, 10, 0));
        account.setNextDueDate(LocalDate.now().plusMonths(1));
        return account;
    }
}
