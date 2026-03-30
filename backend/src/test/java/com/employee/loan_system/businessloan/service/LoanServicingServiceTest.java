package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.RecordRepaymentRequest;
import com.employee.loan_system.businessloan.entity.ApplicationStatus;
import com.employee.loan_system.businessloan.entity.Borrower;
import com.employee.loan_system.businessloan.entity.InstallmentStatus;
import com.employee.loan_system.businessloan.entity.LoanAccount;
import com.employee.loan_system.businessloan.entity.LoanAccountStatus;
import com.employee.loan_system.businessloan.entity.LoanApplication;
import com.employee.loan_system.businessloan.entity.LoanProduct;
import com.employee.loan_system.businessloan.entity.LoanRepaymentTransaction;
import com.employee.loan_system.businessloan.entity.PaymentMode;
import com.employee.loan_system.businessloan.entity.RepaymentInstallment;
import com.employee.loan_system.businessloan.repository.LoanAccountRepository;
import com.employee.loan_system.businessloan.repository.LoanApplicationRepository;
import com.employee.loan_system.businessloan.repository.LoanRepaymentTransactionRepository;
import com.employee.loan_system.businessloan.repository.RepaymentInstallmentRepository;
import com.employee.loan_system.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServicingServiceTest {

    @Mock
    private LoanAccountRepository loanAccountRepository;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private RepaymentInstallmentRepository repaymentInstallmentRepository;

    @Mock
    private LoanRepaymentTransactionRepository loanRepaymentTransactionRepository;

    @InjectMocks
    private LoanServicingService loanServicingService;

    @Test
    void recordRepaymentShouldCloseFirstInstallment() {
        LoanAccount account = activeAccount();
        when(loanAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(loanAccountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecordRepaymentRequest request = new RecordRepaymentRequest();
        request.setAmount(new BigDecimal("47073.00"));
        request.setPaymentMode(PaymentMode.NEFT);
        request.setTransactionReference("TXN-1");
        request.setPaymentDate(LocalDate.now());
        request.setNotes("First EMI");

        var response = loanServicingService.recordRepayment(1L, request);

        assertThat(response.amount()).isEqualByComparingTo("47073.00");
        assertThat(account.getInstallments().get(0).getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(account.getInstallments().get(1).getStatus()).isEqualTo(InstallmentStatus.PENDING);
        assertThat(account.getOutstandingPrincipal()).isEqualByComparingTo("958000.00");
    }

    @Test
    void recordRepaymentShouldRejectClosedAccount() {
        LoanAccount account = activeAccount();
        account.setStatus(LoanAccountStatus.CLOSED);
        when(loanAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        RecordRepaymentRequest request = new RecordRepaymentRequest();
        request.setAmount(new BigDecimal("1000"));
        request.setPaymentMode(PaymentMode.UPI);
        request.setTransactionReference("TXN-2");
        request.setPaymentDate(LocalDate.now());

        assertThatThrownBy(() -> loanServicingService.recordRepayment(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active accounts");
    }

    @Test
    void dashboardShouldAggregateCountsAndAmounts() {
        when(loanApplicationRepository.count()).thenReturn(4L);
        when(loanApplicationRepository.countByStatus(ApplicationStatus.APPROVED)).thenReturn(2L);
        when(loanAccountRepository.countByStatus(LoanAccountStatus.ACTIVE)).thenReturn(1L);
        when(loanAccountRepository.countByStatus(LoanAccountStatus.CLOSED)).thenReturn(1L);
        when(repaymentInstallmentRepository.countOverdue(LocalDate.now())).thenReturn(3L);
        when(loanAccountRepository.sumPrincipalDisbursed()).thenReturn(new BigDecimal("1500000.00"));
        when(loanAccountRepository.sumOutstandingPrincipalByStatus(LoanAccountStatus.ACTIVE)).thenReturn(new BigDecimal("900000.00"));
        when(loanRepaymentTransactionRepository.sumAmount()).thenReturn(new BigDecimal("72073.00"));

        var dashboard = loanServicingService.getDashboard();

        assertThat(dashboard.totalLoanApplications()).isEqualTo(4L);
        assertThat(dashboard.disbursedLoanAccounts()).isEqualTo(2L);
        assertThat(dashboard.totalRepaidAmount()).isEqualByComparingTo("72073.00");
    }

    private LoanAccount activeAccount() {
        LoanApplication application = new LoanApplication();
        application.setId(10L);
        application.setStatus(ApplicationStatus.DISBURSED);
        application.setRequestedAmount(new BigDecimal("1000000"));

        Borrower borrower = new Borrower();
        borrower.setId(7L);
        borrower.setLegalBusinessName("Atlas Foods");
        application.setBorrower(borrower);

        LoanProduct product = new LoanProduct();
        product.setId(5L);
        product.setProductCode("BL-TERM");
        product.setInterestRate(new BigDecimal("12"));
        application.setLoanProduct(product);

        LoanAccount account = new LoanAccount();
        account.setId(1L);
        account.setLoanApplication(application);
        account.setAccountNumber("BLA-000001");
        account.setPrincipalAmount(new BigDecimal("1000000"));
        account.setAnnualInterestRate(new BigDecimal("12"));
        account.setTenureMonths(24);
        account.setMonthlyInstallmentAmount(new BigDecimal("47073.00"));
        account.setOutstandingPrincipal(new BigDecimal("1000000.00"));
        account.setDisbursementReference("DISB-1");
        account.setDisbursedAt(LocalDateTime.now());
        account.setNextDueDate(LocalDate.now().plusMonths(1));

        RepaymentInstallment first = installment(1, "42000.00", "5073.00", LocalDate.now().plusMonths(1));
        RepaymentInstallment second = installment(2, "42500.00", "4900.00", LocalDate.now().plusMonths(2));
        account.addInstallment(first);
        account.addInstallment(second);
        return account;
    }

    private RepaymentInstallment installment(int number, String principalDue, String interestDue, LocalDate dueDate) {
        RepaymentInstallment installment = new RepaymentInstallment();
        installment.setInstallmentNumber(number);
        installment.setDueDate(dueDate);
        installment.setOpeningPrincipal(new BigDecimal("0.00"));
        installment.setPrincipalDue(new BigDecimal(principalDue));
        installment.setInterestDue(new BigDecimal(interestDue));
        installment.setPrincipalPaid(BigDecimal.ZERO.setScale(2));
        installment.setInterestPaid(BigDecimal.ZERO.setScale(2));
        installment.setStatus(InstallmentStatus.PENDING);
        return installment;
    }

    private LoanRepaymentTransaction transaction(String reference, String amount) {
        LoanRepaymentTransaction transaction = new LoanRepaymentTransaction();
        transaction.setTransactionReference(reference);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setAppliedPrincipalAmount(new BigDecimal(amount));
        transaction.setAppliedInterestAmount(BigDecimal.ZERO.setScale(2));
        transaction.setPaymentMode(PaymentMode.NEFT);
        transaction.setPaymentDate(LocalDate.now());
        transaction.setRecordedBy("officer");
        transaction.setRecordedAt(LocalDateTime.now());
        return transaction;
    }
}
