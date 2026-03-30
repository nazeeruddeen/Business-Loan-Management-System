package com.employee.loan_system.businessloan.repository;

import com.employee.loan_system.businessloan.entity.BorrowerDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BorrowerDocumentRepository extends JpaRepository<BorrowerDocument, Long> {

    List<BorrowerDocument> findByBorrower_IdOrderByUploadedAtDesc(Long borrowerId);

    Optional<BorrowerDocument> findByIdAndBorrower_Id(Long documentId, Long borrowerId);
}
