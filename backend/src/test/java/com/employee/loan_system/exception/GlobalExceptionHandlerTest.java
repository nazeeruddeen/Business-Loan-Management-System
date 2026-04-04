package com.employee.loan_system.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessRuleExceptionShouldReturnUnprocessableEntity() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/loan-applications/42/submit");

        var response = handler.handleBusinessRuleException(
                new BusinessRuleException("KYC is incomplete. Missing verified documents: [PAN_CARD]"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("status", 422);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("message", "KYC is incomplete. Missing verified documents: [PAN_CARD]");
    }

    @Test
    void duplicateResourceExceptionShouldReturnConflict() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/eligibility-rules");

        var response = handler.handleDuplicateResourceException(
                new DuplicateResourceException("Eligibility rule already exists with code: INCOME_MIN"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("status", 409);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("message", "Eligibility rule already exists with code: INCOME_MIN");
    }

    @Test
    void optimisticLockFailureShouldReturnConflict() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/loan-accounts/42/repayments");

        var response = handler.handleOptimisticLockingFailure(
                new ObjectOptimisticLockingFailureException("LoanAccount", 42L),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("status", 409);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("message", "The resource was updated by another request. Reload the latest state and retry.");
    }
}
