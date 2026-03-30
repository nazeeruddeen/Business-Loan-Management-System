package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.ApplicationDecisionRequest;
import com.employee.loan_system.businessloan.dto.ApplicationStatusHistoryResponse;
import com.employee.loan_system.businessloan.dto.AssignReviewerRequest;
import com.employee.loan_system.businessloan.dto.BorrowerKycSummaryResponse;
import com.employee.loan_system.businessloan.dto.CreateLoanApplicationRequest;
import com.employee.loan_system.businessloan.dto.DisburseLoanRequest;
import com.employee.loan_system.businessloan.dto.EligibilityEvaluationResponse;
import com.employee.loan_system.businessloan.dto.LoanApplicationResponse;
import com.employee.loan_system.businessloan.entity.ApplicationStatus;
import com.employee.loan_system.businessloan.entity.ApplicationStatusHistory;
import com.employee.loan_system.businessloan.entity.Borrower;
import com.employee.loan_system.businessloan.entity.LoanAccount;
import com.employee.loan_system.businessloan.entity.RepaymentInstallment;
import com.employee.loan_system.businessloan.entity.LoanApplication;
import com.employee.loan_system.businessloan.entity.LoanProduct;
import com.employee.loan_system.businessloan.repository.ApplicationStatusHistoryRepository;
import com.employee.loan_system.businessloan.repository.BorrowerRepository;
import com.employee.loan_system.businessloan.repository.LoanAccountRepository;
import com.employee.loan_system.businessloan.repository.LoanApplicationRepository;
import com.employee.loan_system.businessloan.repository.LoanProductRepository;
import com.employee.loan_system.entity.AppUser;
import com.employee.loan_system.entity.UserRole;
import com.employee.loan_system.exception.BusinessRuleException;
import com.employee.loan_system.exception.ResourceNotFoundException;
import com.employee.loan_system.repository.AppUserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final BorrowerRepository borrowerRepository;
    private final LoanProductRepository loanProductRepository;
    private final AppUserRepository appUserRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final EligibilityService eligibilityService;
    private final BorrowerDocumentService borrowerDocumentService;

    public LoanApplicationService(
            LoanApplicationRepository loanApplicationRepository,
            BorrowerRepository borrowerRepository,
            LoanProductRepository loanProductRepository,
            AppUserRepository appUserRepository,
            ApplicationStatusHistoryRepository statusHistoryRepository,
            LoanAccountRepository loanAccountRepository,
            EligibilityService eligibilityService,
            BorrowerDocumentService borrowerDocumentService) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.borrowerRepository = borrowerRepository;
        this.loanProductRepository = loanProductRepository;
        this.appUserRepository = appUserRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.eligibilityService = eligibilityService;
        this.borrowerDocumentService = borrowerDocumentService;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BORROWER')")
    public LoanApplicationResponse createDraft(CreateLoanApplicationRequest request) {
        Borrower borrower = borrowerRepository.findById(request.getBorrowerId())
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with id: " + request.getBorrowerId()));
        LoanProduct product = loanProductRepository.findById(request.getLoanProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found with id: " + request.getLoanProductId()));

        EligibilityEvaluationResponse evaluation = eligibilityService.evaluate(
                borrower,
                product,
                request.getRequestedAmount(),
                request.getRequestedTenureMonths());

        LoanApplication application = new LoanApplication();
        application.setBorrower(borrower);
        application.setLoanProduct(product);
        application.setRequestedAmount(request.getRequestedAmount());
        application.setRequestedTenureMonths(request.getRequestedTenureMonths());
        application.setPurpose(request.getPurpose().trim());
        application.setStatus(ApplicationStatus.DRAFT);
        application.setEligibilityPassed(evaluation.eligible());
        application.setEligibilitySummary(evaluation.summary());

        LoanApplication saved = loanApplicationRepository.save(application);
        recordHistory(saved, null, ApplicationStatus.DRAFT, "Draft created");
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BORROWER')")
    public LoanApplicationResponse submit(Long applicationId) {
        LoanApplication application = findApplication(applicationId);
        ensureStatus(application, ApplicationStatus.DRAFT, "Only draft applications can be submitted");
        if (!application.isEligibilityPassed()) {
            throw new BusinessRuleException("Application cannot be submitted because eligibility checks failed");
        }
        borrowerDocumentService.assertKycComplete(application.getBorrower());

        ApplicationStatus previousStatus = application.getStatus();
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmittedAt(LocalDateTime.now());
        LoanApplication saved = loanApplicationRepository.save(application);
        recordHistory(saved, previousStatus, ApplicationStatus.SUBMITTED, "Application submitted");
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public LoanApplicationResponse assignReviewer(Long applicationId, AssignReviewerRequest request) {
        LoanApplication application = findApplication(applicationId);
        ensureStatus(application, ApplicationStatus.SUBMITTED, "Reviewer can only be assigned to submitted applications");
        borrowerDocumentService.assertKycComplete(application.getBorrower());

        AppUser reviewer = appUserRepository.findById(request.getReviewerUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found with id: " + request.getReviewerUserId()));
        if (reviewer.getRole() != UserRole.REVIEWER) {
            throw new BusinessRuleException("Assigned user must have REVIEWER role");
        }
        if (!reviewer.isActive()) {
            throw new BusinessRuleException("Assigned reviewer is inactive");
        }

        ApplicationStatus previousStatus = application.getStatus();
        application.setReviewer(reviewer);
        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        LoanApplication saved = loanApplicationRepository.save(application);
        recordHistory(saved, previousStatus, ApplicationStatus.UNDER_REVIEW, "Reviewer assigned: " + reviewer.getUsername());
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public LoanApplicationResponse decide(Long applicationId, ApplicationDecisionRequest request) {
        LoanApplication application = findApplication(applicationId);
        ensureStatus(application, ApplicationStatus.UNDER_REVIEW, "Only applications under review can be decided");

        if (request.getDecisionStatus() != ApplicationStatus.APPROVED && request.getDecisionStatus() != ApplicationStatus.REJECTED) {
            throw new BusinessRuleException("Decision must be APPROVED or REJECTED");
        }

        String actor = currentActor();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!admin && application.getReviewer() != null && !application.getReviewer().getUsername().equalsIgnoreCase(actor)) {
            throw new BusinessRuleException("Only the assigned reviewer can decide this application");
        }

        ApplicationStatus previousStatus = application.getStatus();
        application.setStatus(request.getDecisionStatus());
        application.setDecisionedAt(LocalDateTime.now());
        application.setDecisionRemarks(request.getRemarks().trim());
        LoanApplication saved = loanApplicationRepository.save(application);
        recordHistory(saved, previousStatus, request.getDecisionStatus(), request.getRemarks().trim());
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    public LoanApplicationResponse disburse(Long applicationId, DisburseLoanRequest request) {
        LoanApplication application = findApplication(applicationId);
        ensureStatus(application, ApplicationStatus.APPROVED, "Only approved applications can be disbursed");

        if (loanAccountRepository.findByLoanApplication_Id(applicationId).isPresent()) {
            throw new BusinessRuleException("Loan account already exists for application id: " + applicationId);
        }

        ApplicationStatus previousStatus = application.getStatus();
        application.setStatus(ApplicationStatus.DISBURSED);
        application.setDisbursedAt(request.getDisbursementDate().atStartOfDay());
        LoanApplication saved = loanApplicationRepository.save(application);

        LoanAccount account = new LoanAccount();
        account.setLoanApplication(saved);
        account.setAccountNumber(String.format("BLA-%06d", saved.getId()));
        account.setDisbursementReference(request.getDisbursementReference().trim());
        account.setPrincipalAmount(saved.getRequestedAmount());
        account.setAnnualInterestRate(saved.getLoanProduct().getInterestRate());
        account.setTenureMonths(saved.getRequestedTenureMonths());
        account.setOutstandingPrincipal(saved.getRequestedAmount());
        account.setMonthlyInstallmentAmount(calculateEmi(
                saved.getRequestedAmount(),
                saved.getLoanProduct().getInterestRate(),
                saved.getRequestedTenureMonths()));
        account.setDisbursedAt(request.getDisbursementDate().atStartOfDay());
        account.setNextDueDate(request.getDisbursementDate().plusMonths(1));

        for (RepaymentInstallment installment : buildSchedule(account)) {
            account.addInstallment(installment);
        }

        LoanAccount savedAccount = loanAccountRepository.save(account);
        recordHistory(saved, previousStatus, ApplicationStatus.DISBURSED, "Disbursed as account " + savedAccount.getAccountNumber());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER','BORROWER')")
    public LoanApplicationResponse getApplication(Long applicationId) {
        return toResponse(findApplication(applicationId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER','BORROWER')")
    public List<LoanApplicationResponse> listApplications(ApplicationStatus status) {
        List<LoanApplication> applications = status == null
                ? loanApplicationRepository.findAllByOrderByCreatedAtDesc()
                : loanApplicationRepository.findByStatusOrderByCreatedAtDesc(status);
        return applications.stream().map(this::toResponse).toList();
    }

    private LoanApplication findApplication(Long applicationId) {
        return loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with id: " + applicationId));
    }

    private void ensureStatus(LoanApplication application, ApplicationStatus expected, String message) {
        if (application.getStatus() != expected) {
            throw new BusinessRuleException(message);
        }
    }

    private void recordHistory(LoanApplication application, ApplicationStatus from, ApplicationStatus to, String remarks) {
        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setLoanApplication(application);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setRemarks(remarks);
        history.setChangedBy(currentActor());
        statusHistoryRepository.save(history);
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || !authentication.isAuthenticated()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private LoanApplicationResponse toResponse(LoanApplication application) {
        BorrowerKycSummaryResponse kycSummary = borrowerDocumentService.getKycSummary(application.getBorrower());
        List<ApplicationStatusHistoryResponse> history = statusHistoryRepository
                .findByLoanApplication_IdOrderByChangedAtAsc(application.getId()).stream()
                .map(entry -> ApplicationStatusHistoryResponse.builder()
                        .fromStatus(entry.getFromStatus())
                        .toStatus(entry.getToStatus())
                        .remarks(entry.getRemarks())
                        .changedBy(entry.getChangedBy())
                        .changedAt(entry.getChangedAt())
                        .build())
                .toList();

        return LoanApplicationResponse.builder()
                .id(application.getId())
                .borrowerId(application.getBorrower().getId())
                .borrowerName(application.getBorrower().getLegalBusinessName())
                .loanProductId(application.getLoanProduct().getId())
                .loanProductCode(application.getLoanProduct().getProductCode())
                .requestedAmount(application.getRequestedAmount())
                .requestedTenureMonths(application.getRequestedTenureMonths())
                .purpose(application.getPurpose())
                .status(application.getStatus())
                .eligibilityPassed(application.isEligibilityPassed())
                .eligibilitySummary(application.getEligibilitySummary())
                .reviewerUsername(application.getReviewer() == null ? null : application.getReviewer().getUsername())
                .borrowerKycComplete(kycSummary.kycComplete())
                .missingRequiredDocuments(kycSummary.missingRequiredDocuments())
                .submittedAt(application.getSubmittedAt())
                .decisionedAt(application.getDecisionedAt())
                .disbursedAt(application.getDisbursedAt())
                .decisionRemarks(application.getDecisionRemarks())
                .history(history)
                .build();
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

    private List<RepaymentInstallment> buildSchedule(LoanAccount account) {
        List<RepaymentInstallment> installments = new ArrayList<>();
        BigDecimal remainingPrincipal = account.getPrincipalAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = account.getAnnualInterestRate().divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal emi = account.getMonthlyInstallmentAmount().setScale(2, RoundingMode.HALF_UP);
        LocalDate dueDate = account.getDisbursedAt().toLocalDate().plusMonths(1);

        for (int installmentNumber = 1; installmentNumber <= account.getTenureMonths(); installmentNumber++) {
            BigDecimal interestDue = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalDue = emi.subtract(interestDue).setScale(2, RoundingMode.HALF_UP);

            if (installmentNumber == account.getTenureMonths()) {
                principalDue = remainingPrincipal.setScale(2, RoundingMode.HALF_UP);
                emi = principalDue.add(interestDue).setScale(2, RoundingMode.HALF_UP);
            }

            if (principalDue.compareTo(BigDecimal.ZERO) < 0) {
                principalDue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            RepaymentInstallment installment = new RepaymentInstallment();
            installment.setInstallmentNumber(installmentNumber);
            installment.setDueDate(dueDate);
            installment.setOpeningPrincipal(remainingPrincipal);
            installment.setPrincipalDue(principalDue);
            installment.setInterestDue(interestDue);
            installment.setPrincipalPaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            installment.setInterestPaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            installment.setStatus(com.employee.loan_system.businessloan.entity.InstallmentStatus.PENDING);
            installments.add(installment);

            remainingPrincipal = remainingPrincipal.subtract(principalDue).max(BigDecimal.ZERO);
            dueDate = dueDate.plusMonths(1);
        }
        return installments;
    }
}
