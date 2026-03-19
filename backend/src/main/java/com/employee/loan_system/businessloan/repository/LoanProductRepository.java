package com.employee.loan_system.businessloan.repository;

import com.employee.loan_system.businessloan.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long>, JpaSpecificationExecutor<LoanProduct> {
    boolean existsByProductCodeIgnoreCase(String productCode);
}
