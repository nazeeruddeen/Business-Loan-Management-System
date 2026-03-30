package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.BusinessLoanDashboardResponse;
import com.employee.loan_system.businessloan.dto.LoanAccountResponse;
import com.employee.loan_system.businessloan.dto.LoanRepaymentTransactionResponse;
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
    public List<LoanAccountResponse> listAccounts() {
        return loanAccountRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
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
        RepaymentInstallment firstTouchedInstallment = null;

        List<RepaymentInstallment> installments = account.getInstallments().stream()
                .sorted(Comparator.comparing(RepaymentInstallment::getInstallmentNumber))
                .toList();

        for (RepaymentInstallment installment : installments) {
            if (amountRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
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

            if (installment.remainingDue().compareTo(BigDecimal.ZERO) <= 0) {
                installment.setStatus(InstallmentStatus.PAID);
                installment.setPaidAt(LocalDateTime.now());
            } else if (installment.getDueDate().isBefore(LocalDate.now())) {
                installment.setStatus(InstallmentStatus.OVERDUE);
            } else {
                installment.setStatus(InstallmentStatus.PARTIAL);
            }
        }

        if (amountRemaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Repayment amount exceeds the outstanding schedule");
        }

        account.setOutstandingPrincipal(account.getOutstandingPrincipal().subtract(totalAppliedPrincipal)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP));
        if (account.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) <= 0) {
            account.setStatus(LoanAccountStatus.CLOSED);
        }

        LoanRepaymentTransaction transaction = new LoanRepaymentTransaction();
        transaction.setTransactionReference(request.getTransactionReference().trim());
        transaction.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        transaction.setAppliedPrincipalAmount(totalAppliedPrincipal);
        transaction.setAppliedInterestAmount(totalAppliedInterest);
        transaction.setPaymentMode(request.getPaymentMode());
        transaction.setPaymentDate(request.getPaymentDate());
        transaction.setNotes(request.getNotes() == null ? null : request.getNotes().trim());
        transaction.setRecordedBy(currentActor());
        transaction.setInstallment(firstTouchedInstallment);
        account.addTransaction(transaction);

        loanAccountRepository.save(account);
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
