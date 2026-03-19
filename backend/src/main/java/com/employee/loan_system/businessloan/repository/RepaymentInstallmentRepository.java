package com.employee.loan_system.businessloan.repository;

import com.employee.loan_system.businessloan.entity.InstallmentStatus;
import com.employee.loan_system.businessloan.entity.RepaymentInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RepaymentInstallmentRepository extends JpaRepository<RepaymentInstallment, Long> {

    List<RepaymentInstallment> findByLoanAccount_IdOrderByInstallmentNumberAsc(Long loanAccountId);

    @Query("select count(i) from RepaymentInstallment i where i.status = :status")
    long countByStatus(@Param("status") InstallmentStatus status);

    @Query("select count(i) from RepaymentInstallment i where i.status <> com.employee.loan_system.businessloan.entity.InstallmentStatus.PAID and i.dueDate < :today")
    long countOverdue(@Param("today") LocalDate today);
}
