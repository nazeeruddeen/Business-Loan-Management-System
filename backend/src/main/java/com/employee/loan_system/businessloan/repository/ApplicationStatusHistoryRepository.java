package com.employee.loan_system.businessloan.repository;

import com.employee.loan_system.businessloan.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {
    List<ApplicationStatusHistory> findByLoanApplication_IdOrderByChangedAtAsc(Long loanApplicationId);
}
