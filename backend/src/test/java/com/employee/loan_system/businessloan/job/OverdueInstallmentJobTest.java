package com.employee.loan_system.businessloan.job;

import com.employee.loan_system.businessloan.entity.InstallmentStatus;
import com.employee.loan_system.businessloan.entity.LoanAccount;
import com.employee.loan_system.businessloan.entity.RepaymentInstallment;
import com.employee.loan_system.businessloan.repository.RepaymentInstallmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverdueInstallmentJobTest {

    @Mock
    private RepaymentInstallmentRepository installmentRepository;

    @InjectMocks
    private OverdueInstallmentJob overdueInstallmentJob;

    @Test
    void markOverdueInstallmentsShouldOnlyLoadPendingPastDueRows() {
        RepaymentInstallment overdue = installment(1L, InstallmentStatus.PENDING, LocalDate.now().minusDays(3));
        when(installmentRepository.findByStatusAndDueDateBefore(InstallmentStatus.PENDING, LocalDate.now()))
                .thenReturn(List.of(overdue));

        overdueInstallmentJob.markOverdueInstallments();

        assertThat(overdue.getStatus()).isEqualTo(InstallmentStatus.OVERDUE);
        verify(installmentRepository).findByStatusAndDueDateBefore(InstallmentStatus.PENDING, LocalDate.now());
        verify(installmentRepository).saveAll(List.of(overdue));
    }

    @Test
    void markOverdueInstallmentsShouldSkipSaveWhenNothingMatches() {
        when(installmentRepository.findByStatusAndDueDateBefore(InstallmentStatus.PENDING, LocalDate.now()))
                .thenReturn(List.of());

        overdueInstallmentJob.markOverdueInstallments();

        verify(installmentRepository, never()).saveAll(any());
    }

    private RepaymentInstallment installment(Long id, InstallmentStatus status, LocalDate dueDate) {
        LoanAccount account = new LoanAccount();
        account.setId(99L);

        RepaymentInstallment installment = new RepaymentInstallment();
        installment.setId(id);
        installment.setLoanAccount(account);
        installment.setInstallmentNumber(1);
        installment.setDueDate(dueDate);
        installment.setOpeningPrincipal(new BigDecimal("10000.00"));
        installment.setPrincipalDue(new BigDecimal("1000.00"));
        installment.setInterestDue(new BigDecimal("100.00"));
        installment.setPrincipalPaid(BigDecimal.ZERO.setScale(2));
        installment.setInterestPaid(BigDecimal.ZERO.setScale(2));
        installment.setStatus(status);
        return installment;
    }
}
