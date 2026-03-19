package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BorrowerAddressRequest {

    @NotNull(message = "Address type is required")
    private AddressType addressType;

    @NotBlank(message = "Address line one is required")
    @Size(max = 160, message = "Address line one must not exceed 160 characters")
    private String lineOne;

    @Size(max = 160, message = "Address line two must not exceed 160 characters")
    private String lineTwo;

    @NotBlank(message = "City is required")
    @Size(max = 80, message = "City must not exceed 80 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 80, message = "State must not exceed 80 characters")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Size(max = 15, message = "Postal code must not exceed 15 characters")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(max = 80, message = "Country must not exceed 80 characters")
    private String country;
}
