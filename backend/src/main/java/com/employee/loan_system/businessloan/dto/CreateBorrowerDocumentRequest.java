package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.BorrowerDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBorrowerDocumentRequest {

    @NotNull(message = "Document type is required")
    private BorrowerDocumentType documentType;

    @NotBlank(message = "File name is required")
    @Size(max = 180, message = "File name must not exceed 180 characters")
    private String fileName;

    @NotBlank(message = "File reference is required")
    @Size(max = 255, message = "File reference must not exceed 255 characters")
    private String fileReference;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}
