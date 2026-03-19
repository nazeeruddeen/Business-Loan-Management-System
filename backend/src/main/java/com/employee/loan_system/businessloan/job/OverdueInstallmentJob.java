package com.employee.loan_system.businessloan.job;

import com.employee.loan_system.businessloan.entity.InstallmentStatus;
import com.employee.loan_system.businessloan.entity.RepaymentInstallment;
import com.employee.loan_system.businessloan.repository.RepaymentInstallmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scheduled job that detects overdue repayment installments.
 *
 * Interview answer for "How do you mark overdue EMIs?"
 * → Pre-generated schedule at disbursement time + nightly @Scheduled job.
 *   The job scans PENDING installments where dueDate < today and flips them to OVERDUE.
 *   This avoids computing overdue status on every read request and allows proactive
 *   borrower notifications and NPA classification downstream.
 */
@Component
public class OverdueInstallmentJob {

    private static final Logger log = LoggerFactory.getLogger(OverdueInstallmentJob.class);

    private final RepaymentInstallmentRepository installmentRepository;

    public OverdueInstallmentJob(RepaymentInstallmentRepository installmentRepository) {
        this.installmentRepository = installmentRepository;
    }

    /**
     * Runs every night at 00:30 AM IST.
     * Marks PENDING installments past their due date as OVERDUE.
     */
    @Scheduled(cron = "${app.jobs.overdue-detection.cron:0 30 0 * * *}")
    @Transactional
    public void markOverdueInstallments() {
        LocalDate today = LocalDate.now();
        log.info("OverdueInstallmentJob started for date={}", today);

        List<RepaymentInstallment> allInstallments = installmentRepository.findAll();
        List<RepaymentInstallment> toMark = allInstallments.stream()
                .filter(i -> i.getStatus() == InstallmentStatus.PENDING
                        && i.getDueDate() != null
                        && i.getDueDate().isBefore(today))
                .collect(Collectors.toList());

        if (toMark.isEmpty()) {
            log.info("OverdueInstallmentJob: no installments to mark as overdue");
            return;
        }

        toMark.forEach(installment -> {
            installment.setStatus(InstallmentStatus.OVERDUE);
            log.info(
                "OverdueInstallmentJob: marked installmentId={} loanAccountId={} dueDate={} as OVERDUE",
                installment.getId(),
                installment.getLoanAccount() != null ? installment.getLoanAccount().getId() : "unknown",
                installment.getDueDate()
            );
        });

        installmentRepository.saveAll(toMark);
        log.info("OverdueInstallmentJob completed: marked {} installments as OVERDUE", toMark.size());
    }
}
