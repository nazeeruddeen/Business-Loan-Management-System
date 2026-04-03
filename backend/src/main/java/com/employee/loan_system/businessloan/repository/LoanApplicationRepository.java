package com.employee.loan_system.businessloan.repository;

import com.employee.loan_system.businessloan.entity.ApplicationStatus;
import com.employee.loan_system.businessloan.entity.LoanApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    @EntityGraph(attributePaths = {"borrower", "loanProduct", "reviewer"})
    List<LoanApplication> findByStatusOrderByCreatedAtDesc(ApplicationStatus status);

    @EntityGraph(attributePaths = {"borrower", "loanProduct", "reviewer"})
    List<LoanApplication> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"borrower", "loanProduct", "reviewer"})
    Page<LoanApplication> findByStatusOrderByCreatedAtDesc(ApplicationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"borrower", "loanProduct", "reviewer"})
    Page<LoanApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ApplicationStatus status);
}
