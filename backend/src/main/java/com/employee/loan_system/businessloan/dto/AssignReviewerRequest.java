package com.employee.loan_system.businessloan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignReviewerRequest {

    @NotNull(message = "Reviewer user id is required")
    private Long reviewerUserId;
}
