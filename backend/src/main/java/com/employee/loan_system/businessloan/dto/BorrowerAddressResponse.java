package com.employee.loan_system.businessloan.dto;

import com.employee.loan_system.businessloan.entity.AddressType;
import lombok.Builder;

@Builder
public record BorrowerAddressResponse(
        Long id,
        AddressType addressType,
        String lineOne,
        String lineTwo,
        String city,
        String state,
        String postalCode,
        String country
) {
}
