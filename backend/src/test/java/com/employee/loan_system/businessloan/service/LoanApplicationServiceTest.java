package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.ApplicationDecisionRequest;
import com.employee.loan_system.businessloan.dto.AssignReviewerRequest;
import com.employee.loan_system.businessloan.dto.CreateLoanApplicationRequest;
import com.employee.loan_system.businessloan.dto.EligibilityEvaluationResponse;
import com.employee.loan_system.businessloan.entity.ApplicationStatus;
import com.employee.loan_system.businessloan.entity.Borrower;
import com.employee.loan_system.businessloan.entity.LoanApplication;
import com.employee.loan_system.businessloan.entity.LoanProduct;
import com.employee.loan_system.businessloan.repository.ApplicationStatusHistoryRepository;
import com.employee.loan_system.businessloan.repository.BorrowerRepository;
import com.employee.loan_system.businessloan.repository.LoanAccountRepository;
import com.employee.loan_system.businessloan.repository.LoanApplicationRepository;
import com.employee.loan_system.businessloan.repository.LoanProductRepository;
import com.employee.loan_system.entity.AppUser;
import com.employee.loan_system.entity.UserRole;
import com.employee.loan_system.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private LoanProductRepository loanProductRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ApplicationStatusHistoryRepository statusHistoryRepository;

    @Mock
    private LoanAccountRepository loanAccountRepository;

    @Mock
    private EligibilityService eligibilityService;

    @InjectMocks
    private LoanApplicationService loanApplicationService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDraftShouldStoreEligibilitySnapshot() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("officer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_LOAN_OFFICER"))));

        Borrower borrower = new Borrower();
        borrower.setId(1L);
        borrower.setLegalBusinessName("Atlas Foods");

        LoanProduct product = new LoanProduct();
        product.setId(2L);
        product.setProductCode("BL-TERM");

        when(borrowerRepository.findById(1L)).thenReturn(Optional.of(borrower));
        when(loanProductRepository.findById(2L)).thenReturn(Optional.of(product));
        when(eligibilityService.evaluate(any(Borrower.class), any(LoanProduct.class), any(BigDecimal.class), any(Integer.class)))
                .thenReturn(EligibilityEvaluationResponse.builder().eligible(true).summary("Eligibility checks passed").ruleResults(List.of()).build());
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> {
            LoanApplication application = invocation.getArgument(0);
            application.setId(11L);
            return application;
        });
        when(statusHistoryRepository.findByLoanApplication_IdOrderByChangedAtAsc(11L)).thenReturn(List.of());

        CreateLoanApplicationRequest request = new CreateLoanApplicationRequest();
        request.setBorrowerId(1L);
        request.setLoanProductId(2L);
        request.setRequestedAmount(new BigDecimal("250000"));
        request.setRequestedTenureMonths(24);
        request.setPurpose("Working capital");

        var response = loanApplicationService.createDraft(request);

        ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(loanApplicationRepository).save(captor.capture());
        assertThat(captor.getValue().isEligibilityPassed()).isTrue();
        assertThat(captor.getValue().getStatus()).isEqualTo(ApplicationStatus.DRAFT);
        assertThat(response.id()).isEqualTo(11L);
    }

    @Test
    void assignReviewerShouldMoveApplicationToUnderReview() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        LoanApplication application = new LoanApplication();
        application.setId(11L);
        application.setStatus(ApplicationStatus.SUBMITTED);
        Borrower borrower = new Borrower();
        borrower.setId(1L);
        borrower.setLegalBusinessName("Atlas Foods");
        application.setBorrower(borrower);
        LoanProduct product = new LoanProduct();
        product.setId(2L);
        product.setProductCode("BL-TERM");
        application.setLoanProduct(product);

        AppUser reviewer = new AppUser();
        reviewer.setId(99L);
        reviewer.setUsername("reviewer");
        reviewer.setRole(UserRole.REVIEWER);
        reviewer.setActive(true);

        when(loanApplicationRepository.findById(11L)).thenReturn(Optional.of(application));
        when(appUserRepository.findById(99L)).thenReturn(Optional.of(reviewer));
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statusHistoryRepository.findByLoanApplication_IdOrderByChangedAtAsc(11L)).thenReturn(List.of());

        AssignReviewerRequest request = new AssignReviewerRequest();
        request.setReviewerUserId(99L);

        var response = loanApplicationService.assignReviewer(11L, request);

        assertThat(response.status()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(response.reviewerUsername()).isEqualTo("reviewer");
    }

    @Test
    void decideShouldRejectInvalidTransition() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("reviewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_REVIEWER"))));

        LoanApplication application = new LoanApplication();
        application.setId(11L);
        application.setStatus(ApplicationStatus.SUBMITTED);

        when(loanApplicationRepository.findById(11L)).thenReturn(Optional.of(application));

        ApplicationDecisionRequest request = new ApplicationDecisionRequest();
        request.setDecisionStatus(ApplicationStatus.APPROVED);
        request.setRemarks("Looks good");

        assertThatThrownBy(() -> loanApplicationService.decide(11L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("under review");
    }
}
