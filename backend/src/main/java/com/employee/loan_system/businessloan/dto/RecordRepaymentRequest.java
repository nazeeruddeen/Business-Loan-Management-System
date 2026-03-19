package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecordRepaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @NotBlank(message = "Transaction reference is required")
    private String transactionReference;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
