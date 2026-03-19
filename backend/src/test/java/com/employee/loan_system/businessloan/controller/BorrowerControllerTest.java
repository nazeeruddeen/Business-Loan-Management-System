package com.employee.loan_system.businessloan.controller;

import com.employee.loan_system.businessloan.dto.BorrowerAddressResponse;
import com.employee.loan_system.businessloan.dto.BorrowerResponse;
import com.employee.loan_system.businessloan.dto.CreateBorrowerRequest;
import com.employee.loan_system.businessloan.service.BorrowerService;
import com.employee.loan_system.exception.GlobalExceptionHandler;
import com.employee.loan_system.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = BorrowerController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BorrowerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BorrowerService borrowerService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createBorrowerShouldReturnCreated() throws Exception {
        CreateBorrowerRequest request = BorrowerControllerTestData.validBorrowerRequest();
        when(borrowerService.createBorrower(any(CreateBorrowerRequest.class))).thenReturn(
                BorrowerResponse.builder()
                        .id(55L)
                        .legalBusinessName("Atlas Foods Private Limited")
                        .contactPersonName("Ravi Kumar")
                        .businessPan("ABCDE1234F")
                        .gstin("29ABCDE1234F1Z5")
                        .email("ops@atlasfoods.com")
                        .phoneNumber("9876543210")
                        .industryType("Manufacturing")
                        .annualTurnover(new BigDecimal("15000000"))
                        .monthlyIncome(new BigDecimal("650000"))
                        .createdAt(LocalDateTime.now())
                        .addresses(List.of(BorrowerAddressResponse.builder().id(1L).lineOne("12 Industrial Estate").build()))
                        .build()
        );

        mockMvc.perform(post("/api/v1/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.businessPan").value("ABCDE1234F"));
    }

    @Test
    void createBorrowerShouldReturnBadRequestForInvalidPayload() throws Exception {
        CreateBorrowerRequest request = BorrowerControllerTestData.validBorrowerRequest();
        request.setBusinessPan("BADPAN");

        mockMvc.perform(post("/api/v1/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.businessPan").exists());
    }
}
