package com.employee.loan_system.businessloan.repository;

import com.employee.loan_system.businessloan.entity.Borrower;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BorrowerRepository extends JpaRepository<Borrower, Long> {
    boolean existsByBusinessPanIgnoreCase(String businessPan);
    Optional<Borrower> findByBusinessPanIgnoreCase(String businessPan);
    List<Borrower> findByLegalBusinessNameContainingIgnoreCase(String legalBusinessName);
    Page<Borrower> findByLegalBusinessNameContainingIgnoreCase(String legalBusinessName, Pageable pageable);
}
