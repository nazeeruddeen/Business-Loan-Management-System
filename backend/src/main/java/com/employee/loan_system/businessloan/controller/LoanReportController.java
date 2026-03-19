package com.employee.loan_system.businessloan.controller;

import com.employee.loan_system.businessloan.dto.DisbursementReportResponse;
import com.employee.loan_system.businessloan.service.LoanReportService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping("/api/v1/reports")
public class LoanReportController {

    private final LoanReportService loanReportService;

    public LoanReportController(LoanReportService loanReportService) {
        this.loanReportService = loanReportService;
    }

    @GetMapping("/disbursements")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<DisbursementReportResponse> disbursements(
            @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "disbursedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(loanReportService.getDisbursementReport(from, to, pageable));
    }

    @GetMapping(value = "/disbursements/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public ResponseEntity<StreamingResponseBody> exportDisbursements(
            @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate to) {
        StreamingResponseBody body = loanReportService.exportDisbursementCsv(from, to);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("disbursement-report.csv").build());
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
