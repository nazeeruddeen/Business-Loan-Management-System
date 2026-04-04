package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.BusinessLoanDashboardResponse;
import com.employee.loan_system.businessloan.dto.LoanAccountResponse;
import com.employee.loan_system.businessloan.dto.LoanRepaymentTransactionResponse;
import com.employee.loan_system.businessloan.dto.PagedResponse;
import com.employee.loan_system.businessloan.dto.RecordRepaymentRequest;
import com.employee.loan_system.businessloan.dto.RepaymentInstallmentResponse;
import com.employee.loan_system.businessloan.entity.InstallmentStatus;
import com.employee.loan_system.businessloan.entity.LoanAccount;
import com.employee.loan_system.businessloan.entity.LoanAccountStatus;
import com.employee.loan_system.businessloan.entity.LoanApplication;
import com.employee.loan_system.businessloan.entity.LoanRepaymentTransaction;
import com.employee.loan_system.businessloan.entity.RepaymentInstallment;
import com.employee.loan_system.businessloan.repository.LoanAccountRepository;
import com.employee.loan_system.businessloan.repository.LoanApplicationRepository;
import com.employee.loan_system.businessloan.repository.LoanRepaymentTransactionRepository;
import com.employee.loan_system.businessloan.repository.RepaymentInstallmentRepository;
import com.employee.loan_system.exception.BusinessRuleException;
import com.employee.loan_system.exception.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class LoanServicingService {

    private final LoanAccountRepository loanAccountRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final RepaymentInstallmentRepository repaymentInstallmentRepository;
    private final LoanRepaymentTransactionRepository loanRepaymentTransactionRepository;

    public LoanServicingService(
            LoanAccountRepository loanAccountRepository,
            LoanApplicationRepository loanApplicationRepository,
            RepaymentInstallmentRepository repaymentInstallmentRepository,
            LoanRepaymentTransactionRepository loanRepaymentTransactionRepository) {
        this.loanAccountRepository = loanAccountRepository;
        this.loanApplicationRepository = loanApplicationRepository;
        this.repaymentInstallmentRepository = repaymentInstallmentRepository;
        this.loanRepaymentTransactionRepository = loanRepaymentTransactionRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER','BORROWER')")
    public LoanAccountResponse getByAccountNumber(String accountNumber) {
        return toResponse(findAccountByNumber(accountNumber));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER','BORROWER')")
    public LoanAccountResponse getByApplicationId(Long applicationId) {
        LoanAccount account = loanAccountRepository.findByLoanApplication_Id(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan account not found for application id: " + applicationId));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public PagedResponse<LoanAccountResponse> listAccounts(Pageable pageable) {
        return PagedResponse.from(loanAccountRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public LoanRepaymentTransactionResponse recordRepayment(Long accountId, RecordRepaymentRequest request) {
        LoanAccount account = loanAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan account not found with id: " + accountId));
        if (account.getStatus() != LoanAccountStatus.ACTIVE) {
            throw new BusinessRuleException("Repayments can only be recorded for active accounts");
        }

        BigDecimal amountRemaining = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amountRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Repayment amount must be greater than zero");
        }

        BigDecimal totalAppliedPrincipal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAppliedInterest = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal prepaymentPrincipalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        RepaymentInstallment firstTouchedInstallment = null;
        LocalDate paymentDate = request.getPaymentDate();

        List<RepaymentInstallment> installments = account.getInstallments().stream()
                .sorted(Comparator.comparing(RepaymentInstallment::getInstallmentNumber))
                .toList();

        boolean hasDueInstallments = installments.stream()
                .filter(this::hasRemainingDue)
                .anyMatch(installment -> !installment.getDueDate().isAfter(paymentDate));
        boolean advancedToNextInstallment = false;

        for (RepaymentInstallment installment : installments) {
            if (amountRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            if (!isEligibleForScheduledPayment(installment, paymentDate, hasDueInstallments, advancedToNextInstallment)) {
                continue;
            }

            BigDecimal interestRemaining = installment.getInterestDue().subtract(installment.getInterestPaid());
            BigDecimal principalRemaining = installment.getPrincipalDue().subtract(installment.getPrincipalPaid());
            if (interestRemaining.add(principalRemaining).compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            if (firstTouchedInstallment == null) {
                firstTouchedInstallment = installment;
            }

            BigDecimal interestApplied = amountRemaining.min(interestRemaining.max(BigDecimal.ZERO));
            installment.setInterestPaid(installment.getInterestPaid().add(interestApplied));
            amountRemaining = amountRemaining.subtract(interestApplied);
            totalAppliedInterest = totalAppliedInterest.add(interestApplied);

            BigDecimal principalApplied = amountRemaining.min(principalRemaining.max(BigDecimal.ZERO));
            installment.setPrincipalPaid(installment.getPrincipalPaid().add(principalApplied));
            amountRemaining = amountRemaining.subtract(principalApplied);
            totalAppliedPrincipal = totalAppliedPrincipal.add(principalApplied);

            updateInstallmentStatus(installment, paymentDate);
            if (hasDueInstallments || installment.getDueDate().isAfter(paymentDate) || installment.getDueDate().isEqual(paymentDate)) {
                advancedToNextInstallment = true;
            }
        }

        BigDecimal remainingPrincipalCapacity = account.getOutstandingPrincipal().subtract(totalAppliedPrincipal)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        if (amountRemaining.compareTo(BigDecimal.ZERO) > 0 && remainingPrincipalCapacity.compareTo(BigDecimal.ZERO) > 0) {
            prepaymentPrincipalAmount = amountRemaining.min(remainingPrincipalCapacity);
            amountRemaining = amountRemaining.subtract(prepaymentPrincipalAmount);
            totalAppliedPrincipal = totalAppliedPrincipal.add(prepaymentPrincipalAmount);
        }

        if (amountRemaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Repayment amount exceeds the outstanding principal and currently due interest");
        }

        account.setOutstandingPrincipal(account.getOutstandingPrincipal().subtract(totalAppliedPrincipal)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP));
        if (prepaymentPrincipalAmount.compareTo(BigDecimal.ZERO) > 0) {
            recastFutureInstallments(account, paymentDate);
        }
        refreshNextDueDate(account, paymentDate);
        if (account.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) <= 0) {
            account.setStatus(LoanAccountStatus.CLOSED);
        }

        LoanRepaymentTransaction transaction = new LoanRepaymentTransaction();
        transaction.setTransactionReference(request.getTransactionReference().trim());
        transaction.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        transaction.setAppliedPrincipalAmount(totalAppliedPrincipal);
        transaction.setPrepaymentPrincipalAmount(prepaymentPrincipalAmount);
        transaction.setAppliedInterestAmount(totalAppliedInterest);
        transaction.setPaymentMode(request.getPaymentMode());
        transaction.setPaymentDate(paymentDate);
        transaction.setNotes(request.getNotes() == null ? null : request.getNotes().trim());
        transaction.setRecordedBy(currentActor());
        transaction.setInstallment(firstTouchedInstallment);
        account.addTransaction(transaction);

        loanAccountRepository.saveAndFlush(account);
        return toTransactionResponse(transaction);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public BusinessLoanDashboardResponse getDashboard() {
        long totalApplications = loanApplicationRepository.count();
        long approvedApplications = loanApplicationRepository.countByStatus(com.employee.loan_system.businessloan.entity.ApplicationStatus.APPROVED);
        long disbursedAccounts = loanAccountRepository.countByStatus(LoanAccountStatus.ACTIVE) + loanAccountRepository.countByStatus(LoanAccountStatus.CLOSED);
        long activeAccounts = loanAccountRepository.countByStatus(LoanAccountStatus.ACTIVE);
        long overdueInstallments = repaymentInstallmentRepository.countOverdue(LocalDate.now());
        BigDecimal totalPrincipalDisbursed = loanAccountRepository.sumPrincipalDisbursed().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalOutstanding = loanAccountRepository.sumOutstandingPrincipalByStatus(LoanAccountStatus.ACTIVE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalRepaid = loanRepaymentTransactionRepository.sumAmount().setScale(2, RoundingMode.HALF_UP);

        return BusinessLoanDashboardResponse.builder()
                .totalLoanApplications(totalApplications)
                .approvedLoanApplications(approvedApplications)
                .disbursedLoanAccounts(disbursedAccounts)
                .activeLoanAccounts(activeAccounts)
                .overdueInstallments(overdueInstallments)
                .totalPrincipalDisbursed(totalPrincipalDisbursed)
                .totalOutstandingPrincipal(totalOutstanding)
                .totalRepaidAmount(totalRepaid)
                .build();
    }

    private LoanAccount findAccountByNumber(String accountNumber) {
        return loanAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan account not found with account number: " + accountNumber));
    }

    private boolean isEligibleForScheduledPayment(
            RepaymentInstallment installment,
            LocalDate paymentDate,
            boolean hasDueInstallments,
            boolean advancedToNextInstallment) {
        if (!hasRemainingDue(installment)) {
            return false;
        }
        if (!hasDueInstallments) {
            return !advancedToNextInstallment;
        }
        return !installment.getDueDate().isAfter(paymentDate);
    }

    private boolean hasRemainingDue(RepaymentInstallment installment) {
        return installment.remainingDue().compareTo(BigDecimal.ZERO) > 0;
    }

    private void updateInstallmentStatus(RepaymentInstallment installment, LocalDate paymentDate) {
        if (installment.remainingDue().compareTo(BigDecimal.ZERO) <= 0) {
            installment.setStatus(InstallmentStatus.PAID);
            installment.setPaidAt(LocalDateTime.now());
            installment.setRemarks(null);
            return;
        }
        installment.setPaidAt(null);
        installment.setStatus(installment.getDueDate().isBefore(paymentDate)
                ? InstallmentStatus.OVERDUE
                : InstallmentStatus.PARTIAL);
    }

    private void recastFutureInstallments(LoanAccount account, LocalDate paymentDate) {
        List<RepaymentInstallment> futureInstallments = account.getInstallments().stream()
                .filter(installment -> installment.remainingDue().compareTo(BigDecimal.ZERO) > 0)
                .filter(installment -> installment.getPrincipalPaid().compareTo(BigDecimal.ZERO) == 0
                        && installment.getInterestPaid().compareTo(BigDecimal.ZERO) == 0)
                .sorted(Comparator.comparing(RepaymentInstallment::getInstallmentNumber))
                .toList();

        if (futureInstallments.isEmpty()) {
            account.setMonthlyInstallmentAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            return;
        }

        BigDecimal remainingPrincipal = account.getOutstandingPrincipal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = account.getAnnualInterestRate().divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal recalculatedEmi = calculateEmi(remainingPrincipal, account.getAnnualInterestRate(), futureInstallments.size());

        for (int index = 0; index < futureInstallments.size(); index++) {
            RepaymentInstallment installment = futureInstallments.get(index);
            BigDecimal interestDue = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalDue = recalculatedEmi.subtract(interestDue).setScale(2, RoundingMode.HALF_UP);
            if (index == futureInstallments.size() - 1) {
                principalDue = remainingPrincipal;
            }
            if (principalDue.compareTo(BigDecimal.ZERO) < 0) {
                principalDue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            installment.setOpeningPrincipal(remainingPrincipal);
            installment.setInterestDue(interestDue);
            installment.setPrincipalDue(principalDue);
            installment.setPrincipalPaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            installment.setInterestPaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            installment.setPaidAt(null);
            installment.setStatus(installment.getDueDate().isBefore(paymentDate) ? InstallmentStatus.OVERDUE : InstallmentStatus.PENDING);
            installment.setRemarks("Schedule recast after principal prepayment on " + paymentDate);

            remainingPrincipal = remainingPrincipal.subtract(principalDue).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }

        account.setMonthlyInstallmentAmount(recalculatedEmi.setScale(2, RoundingMode.HALF_UP));
    }

    private void refreshNextDueDate(LoanAccount account, LocalDate paymentDate) {
        LocalDate nextDueDate = account.getInstallments().stream()
                .filter(installment -> installment.remainingDue().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(RepaymentInstallment::getDueDate))
                .map(RepaymentInstallment::getDueDate)
                .findFirst()
                .orElse(paymentDate);
        account.setNextDueDate(nextDueDate);
    }

    private BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualInterestRate, Integer tenureMonths) {
        if (tenureMonths == null || tenureMonths <= 0) {
            return principal.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = annualInterestRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal factor = onePlusRate.pow(tenureMonths);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(factor);
        BigDecimal denominator = factor.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private LoanAccountResponse toResponse(LoanAccount account) {
        return LoanAccountResponse.builder()
                .id(account.getId())
                .applicationId(account.getLoanApplication().getId())
                .accountNumber(account.getAccountNumber())
                .borrowerName(account.getLoanApplication().getBorrower().getLegalBusinessName())
                .productCode(account.getLoanApplication().getLoanProduct().getProductCode())
                .principalAmount(account.getPrincipalAmount())
                .annualInterestRate(account.getAnnualInterestRate())
                .tenureMonths(account.getTenureMonths())
                .monthlyInstallmentAmount(account.getMonthlyInstallmentAmount())
                .outstandingPrincipal(account.getOutstandingPrincipal())
                .disbursementReference(account.getDisbursementReference())
                .status(account.getStatus())
                .disbursedAt(account.getDisbursedAt())
                .nextDueDate(account.getNextDueDate())
                .installments(account.getInstallments().stream()
                        .sorted(Comparator.comparing(RepaymentInstallment::getInstallmentNumber))
                        .map(this::toInstallmentResponse)
                        .toList())
                .transactions(account.getTransactions().stream()
                        .sorted(Comparator.comparing(LoanRepaymentTransaction::getRecordedAt).reversed())
                        .map(this::toTransactionResponse)
                        .toList())
                .build();
    }

    private RepaymentInstallmentResponse toInstallmentResponse(RepaymentInstallment installment) {
        InstallmentStatus effectiveStatus = installment.getStatus();
        if (effectiveStatus != InstallmentStatus.PAID && installment.getDueDate().isBefore(LocalDate.now())) {
            effectiveStatus = InstallmentStatus.OVERDUE;
        }

        return RepaymentInstallmentResponse.builder()
                .id(installment.getId())
                .installmentNumber(installment.getInstallmentNumber())
                .dueDate(installment.getDueDate())
                .openingPrincipal(installment.getOpeningPrincipal())
                .principalDue(installment.getPrincipalDue())
                .interestDue(installment.getInterestDue())
                .principalPaid(installment.getPrincipalPaid())
                .interestPaid(installment.getInterestPaid())
                .remainingDue(installment.remainingDue())
                .status(effectiveStatus)
                .paidAt(installment.getPaidAt())
                .remarks(installment.getRemarks())
                .build();
    }

    private LoanRepaymentTransactionResponse toTransactionResponse(LoanRepaymentTransaction transaction) {
        return LoanRepaymentTransactionResponse.builder()
                .id(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .amount(transaction.getAmount())
                .appliedPrincipalAmount(transaction.getAppliedPrincipalAmount())
                .prepaymentPrincipalAmount(transaction.getPrepaymentPrincipalAmount())
                .appliedInterestAmount(transaction.getAppliedInterestAmount())
                .paymentMode(transaction.getPaymentMode())
                .paymentDate(transaction.getPaymentDate())
                .notes(transaction.getNotes())
                .recordedBy(transaction.getRecordedBy())
                .recordedAt(transaction.getRecordedAt())
                .build();
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || !authentication.isAuthenticated()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
