package com.employee.loan_system.businessloan.controller;

import com.employee.loan_system.businessloan.dto.BorrowerDocumentResponse;
import com.employee.loan_system.businessloan.dto.CreateBorrowerDocumentRequest;
import com.employee.loan_system.businessloan.entity.BorrowerDocumentStatus;
import com.employee.loan_system.businessloan.entity.BorrowerDocumentType;
import com.employee.loan_system.businessloan.service.BorrowerDocumentService;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = BorrowerDocumentController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BorrowerDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BorrowerDocumentService borrowerDocumentService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createDocumentShouldReturnCreated() throws Exception {
        CreateBorrowerDocumentRequest request = new CreateBorrowerDocumentRequest();
        request.setDocumentType(BorrowerDocumentType.PAN_CARD);
        request.setFileName("pan-card.pdf");
        request.setFileReference("kyc/borrower-101/pan-card.pdf");
        request.setRemarks("Primary PAN proof");

        when(borrowerDocumentService.createDocument(any(Long.class), any(CreateBorrowerDocumentRequest.class)))
                .thenReturn(BorrowerDocumentResponse.builder()
                        .id(11L)
                        .documentType(BorrowerDocumentType.PAN_CARD)
                        .documentStatus(BorrowerDocumentStatus.UPLOADED)
                        .fileName("pan-card.pdf")
                        .fileReference("kyc/borrower-101/pan-card.pdf")
                        .uploadedBy("officer")
                        .uploadedAt(LocalDateTime.now())
                        .remarks("Primary PAN proof")
                        .requiredDocument(true)
                        .build());

        mockMvc.perform(post("/api/v1/borrowers/101/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.documentType").value("PAN_CARD"))
                .andExpect(jsonPath("$.documentStatus").value("UPLOADED"));
    }
}
