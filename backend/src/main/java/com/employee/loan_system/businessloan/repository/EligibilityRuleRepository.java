package com.employee.loan_system.businessloan.repository;

import com.employee.loan_system.businessloan.entity.EligibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EligibilityRuleRepository extends JpaRepository<EligibilityRule, Long> {
    boolean existsByRuleCodeIgnoreCase(String ruleCode);
    List<EligibilityRule> findByActiveTrueOrderByRuleCodeAsc();
}
