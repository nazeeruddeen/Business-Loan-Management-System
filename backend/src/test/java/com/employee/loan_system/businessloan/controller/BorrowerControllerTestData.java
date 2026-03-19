package com.employee.loan_system.businessloan.controller;

import com.employee.loan_system.businessloan.dto.BorrowerAddressRequest;
import com.employee.loan_system.businessloan.dto.CreateBorrowerRequest;
import com.employee.loan_system.businessloan.entity.AddressType;

import java.math.BigDecimal;
import java.util.List;

final class BorrowerControllerTestData {

    private BorrowerControllerTestData() {
    }

    static CreateBorrowerRequest validBorrowerRequest() {
        CreateBorrowerRequest request = new CreateBorrowerRequest();
        request.setLegalBusinessName("Atlas Foods Private Limited");
        request.setContactPersonName("Ravi Kumar");
        request.setBusinessPan("ABCDE1234F");
        request.setGstin("29ABCDE1234F1Z5");
        request.setEmail("ops@atlasfoods.com");
        request.setPhoneNumber("9876543210");
        request.setIndustryType("Manufacturing");
        request.setAnnualTurnover(new BigDecimal("15000000"));
        request.setMonthlyIncome(new BigDecimal("650000"));

        BorrowerAddressRequest address = new BorrowerAddressRequest();
        address.setAddressType(AddressType.REGISTERED);
        address.setLineOne("12 Industrial Estate");
        address.setCity("Bengaluru");
        address.setState("Karnataka");
        address.setPostalCode("560001");
        address.setCountry("India");

        request.setAddresses(List.of(address));
        return request;
    }
}
