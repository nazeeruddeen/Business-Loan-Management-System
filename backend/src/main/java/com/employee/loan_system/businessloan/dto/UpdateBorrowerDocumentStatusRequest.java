package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.BorrowerDocumentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBorrowerDocumentStatusRequest {

    @NotNull(message = "Document status is required")
    private BorrowerDocumentStatus documentStatus;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}
