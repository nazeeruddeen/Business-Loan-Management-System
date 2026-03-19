package com.employee.loan_system.businessloan.repository;

import com.employee.loan_system.businessloan.entity.LoanRepaymentTransaction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface LoanRepaymentTransactionRepository extends JpaRepository<LoanRepaymentTransaction, Long> {

    List<LoanRepaymentTransaction> findByLoanAccount_IdOrderByRecordedAtDesc(Long loanAccountId);

    @Query("select coalesce(sum(t.amount), 0) from LoanRepaymentTransaction t")
    BigDecimal sumAmount();
}
