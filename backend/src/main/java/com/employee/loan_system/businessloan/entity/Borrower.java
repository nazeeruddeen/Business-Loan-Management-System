package com.employee.loan_system.businessloan.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(
        name = "borrowers",
        uniqueConstraints = @UniqueConstraint(name = "uk_borrowers_business_pan", columnNames = "business_pan")
)
public class Borrower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legal_business_name", nullable = false, length = 150)
    private String legalBusinessName;

    @Column(name = "contact_person_name", nullable = false, length = 120)
    private String contactPersonName;

    @Column(name = "business_pan", nullable = false, length = 10)
    private String businessPan;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "email", nullable = false, length = 180)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "industry_type", nullable = false, length = 80)
    private String industryType;

    @Column(name = "annual_turnover", nullable = false, precision = 15, scale = 2)
    private BigDecimal annualTurnover;

    @Column(name = "monthly_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @OneToMany(mappedBy = "borrower", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BorrowerAddress> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "borrower", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BorrowerDocument> documents = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addAddress(BorrowerAddress address) {
        address.setBorrower(this);
        addresses.add(address);
    }

    public void addDocument(BorrowerDocument document) {
        document.setBorrower(this);
        documents.add(document);
    }
}
