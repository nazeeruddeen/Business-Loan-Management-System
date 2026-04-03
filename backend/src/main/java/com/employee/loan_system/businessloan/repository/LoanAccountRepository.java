package com.employee.loan_system.businessloan.repository;

import com.employee.loan_system.businessloan.entity.LoanAccount;
import com.employee.loan_system.businessloan.entity.LoanAccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {

    @EntityGraph(attributePaths = {"loanApplication", "loanApplication.borrower", "loanApplication.loanProduct"})
    Optional<LoanAccount> findByLoanApplication_Id(Long applicationId);

    @EntityGraph(attributePaths = {"loanApplication", "loanApplication.borrower", "loanApplication.loanProduct"})
    Optional<LoanAccount> findByAccountNumber(String accountNumber);

    @EntityGraph(attributePaths = {"loanApplication", "loanApplication.borrower", "loanApplication.loanProduct"})
    List<LoanAccount> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"loanApplication", "loanApplication.borrower", "loanApplication.loanProduct"})
    Page<LoanAccount> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"loanApplication", "loanApplication.borrower", "loanApplication.loanProduct"})
    Page<LoanAccount> findByDisbursedAtBetweenOrderByDisbursedAtDesc(LocalDateTime from, LocalDateTime to, Pageable pageable);

    @EntityGraph(attributePaths = {"loanApplication", "loanApplication.borrower", "loanApplication.loanProduct"})
    List<LoanAccount> findByDisbursedAtBetweenOrderByDisbursedAtDesc(LocalDateTime from, LocalDateTime to);

    long countByStatus(LoanAccountStatus status);

    long countByDisbursedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(a.principalAmount), 0) from LoanAccount a")
    BigDecimal sumPrincipalDisbursed();

    @Query("select coalesce(sum(a.principalAmount), 0) from LoanAccount a where a.disbursedAt between :from and :to")
    BigDecimal sumPrincipalDisbursedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(a.outstandingPrincipal), 0) from LoanAccount a where a.status = :status")
    BigDecimal sumOutstandingPrincipalByStatus(@Param("status") LoanAccountStatus status);

    @Query("select coalesce(sum(a.outstandingPrincipal), 0) from LoanAccount a where a.disbursedAt between :from and :to")
    BigDecimal sumOutstandingPrincipalBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
