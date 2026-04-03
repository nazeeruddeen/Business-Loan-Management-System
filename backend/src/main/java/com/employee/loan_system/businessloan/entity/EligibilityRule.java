package com.employee.loan_system.businessloan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "eligibility_rules",
        uniqueConstraints = @UniqueConstraint(name = "uk_eligibility_rules_code", columnNames = "rule_code")
)
public class EligibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "rule_code", nullable = false, length = 60)
    private String ruleCode;

    @Column(name = "rule_expression", nullable = false, length = 120)
    private String ruleExpression;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "rule_type", nullable = false, length = 30)
    private EligibilityRuleType ruleType;

    @Column(name = "min_value", precision = 15, scale = 2)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 15, scale = 2)
    private BigDecimal maxValue;

    @Column(name = "rule_value_text", length = 120)
    private String ruleValueText;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

