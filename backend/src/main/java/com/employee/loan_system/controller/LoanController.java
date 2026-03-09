package com.employee.loan_system.controller;

import com.employee.loan_system.dto.*;
import com.employee.loan_system.service.FileProcessingService;
import com.employee.loan_system.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/loans")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202"})
public class LoanController {
    @Autowired
    private LoanService loanService;

    @Autowired
    private FileProcessingService fileProcessingService;

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @PostMapping("/applyLoan")
    public ResponseEntity<LoanApplicationDTO> applyLoan(@RequestBody LoanApplicationDTO dto) {
        return new ResponseEntity<>(loanService.applyLoan(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST','VIEWER')")
    @GetMapping("/loanTaskboard")
    public ResponseEntity<List<LoanApplicationDTO>> getLoanTaskboard() {
        return new ResponseEntity<>(loanService.getLoanTaskboardData(), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST','VIEWER')")
    @GetMapping("/getOverviewDeatils/{appId}")
    public ResponseEntity<OverviewDTO> getOverviewDetails(@PathVariable Long appId) {
        return new ResponseEntity<>(loanService.getOverviewDetails(appId), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST','VIEWER')")
    @GetMapping("/getCompanyDetails/{appId}")
    public ResponseEntity<CompanyDetailsDTO> getCompanyDetails(@PathVariable Long appId) {
        return new ResponseEntity<>(loanService.getCompanyDetails(appId), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @PostMapping("/saveCompanyDetails/{appId}")
    public ResponseEntity<CompanyDetailsDTO> saveCompanyDetails(
            @PathVariable Long appId,
            @RequestBody CompanyDetailsDTO dto) {
        return new ResponseEntity<>(loanService.saveCompanyDetails(appId, dto), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST','VIEWER')")
    @GetMapping("/getCompanyAddress/{appId}")
    public ResponseEntity<CompanyAddressDTO> getCompanyAddress(@PathVariable Long appId) {
        return new ResponseEntity<>(loanService.getCompanyAddress(appId), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @PostMapping("/saveCompanyAddress/{appId}")
    public ResponseEntity<CompanyAddressDTO> saveCompanyAddress(
            @PathVariable Long appId,
            @RequestBody CompanyAddressDTO dto) {
        return new ResponseEntity<>(loanService.saveCompanyAddress(appId, dto), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST','VIEWER')")
    @GetMapping("/getProductDetails/{appId}")
    public ResponseEntity<BusinessProductDTO> getProductDetails(@PathVariable Long appId) {
        return new ResponseEntity<>(loanService.getProductDetails(appId), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @PostMapping("/saveProductDetails/{appId}")
    public ResponseEntity<BusinessProductDTO> saveProductDetails(
            @PathVariable Long appId,
            @RequestBody BusinessProductDTO dto) {
        return new ResponseEntity<>(loanService.saveProductDetails(appId, dto), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @PostMapping("/readJson")
    public ResponseEntity<List<PersonDetailsDTO>> readJsonFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            List<PersonDetailsDTO> persons = fileProcessingService.processJsonFile(file);
            return new ResponseEntity<>(persons, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @PostMapping("/saveJsonfileData/{appId}")
    public ResponseEntity<?> savePersonDetails(
            @PathVariable Long appId,
            @RequestBody List<PersonDetailsDTO> dtos) {
        try {
            List<PersonDetailsDTO> result = loanService.savePersonDetails(appId, dtos);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @GetMapping("/getPersonDetails/{appId}")
    public ResponseEntity<List<PersonDetailsDTO>> getPersonDetails(@PathVariable Long appId) {
        return new ResponseEntity<>(loanService.getPersonDetails(appId), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @PostMapping("/readExcel")
    public ResponseEntity<List<SalesReportDTO>> readExcelFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            List<SalesReportDTO> reports = fileProcessingService.processExcelFile(file);
            return new ResponseEntity<>(reports, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @PostMapping("/saveSalesReport/{appId}")
    public ResponseEntity<?> saveSalesReport(
            @PathVariable Long appId,
            @RequestBody List<SalesReportDTO> dtos) {
        try {
            List<SalesReportDTO> result = loanService.saveSalesReport(appId, dtos);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST')")
    @GetMapping("/getSalesReportDetails/{appId}")
    public ResponseEntity<List<SalesReportDTO>> getSalesReportDetails(@PathVariable Long appId) {
        return new ResponseEntity<>(loanService.getSalesReportDetails(appId), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST')")
    @GetMapping("/fetchtransactions/{appId}")
    public ResponseEntity<List<TransactionDTO>> getTransactions(
            @PathVariable Long appId,
            @RequestParam(required = false) String duration,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return new ResponseEntity<>(loanService.getTransactions(appId, duration, startDate, endDate), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST')")
    @GetMapping("/filtertransactions/{appId}")
    public ResponseEntity<List<TransactionDTO>> filterTransactions(
            @PathVariable Long appId,
            @RequestParam(required = false) List<String> statusList,
            @RequestParam(required = false) List<String> instrumentList,
            @RequestParam(required = false) String statusOrInstrument,
            @RequestParam(required = false, name = "statusOrInstrumentTypesList") List<String> statusOrInstrumentTypesList) {

        boolean hasNewFilterParams = (statusList != null && !statusList.isEmpty())
                || (instrumentList != null && !instrumentList.isEmpty());

        if (hasNewFilterParams) {
            return new ResponseEntity<>(loanService.filterTransactions(appId, statusList, instrumentList), HttpStatus.OK);
        }

        List<String> legacyTypes = statusOrInstrumentTypesList == null ? List.of() : statusOrInstrumentTypesList;
        String legacyMode = (statusOrInstrument == null || statusOrInstrument.isBlank()) ? "status" : statusOrInstrument;

        return new ResponseEntity<>(
                loanService.filterTransactions(appId, legacyMode, legacyTypes), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST')")
    @GetMapping("/getTxnsData/{appId}")
    public ResponseEntity<List<TransactionDTO>> getTxnsData(@PathVariable Long appId) {
        return new ResponseEntity<>(loanService.getTransactions(appId, null, null, null), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @PostMapping("/updateTransaction")
    public ResponseEntity<TransactionDTO> updateTransaction(@RequestBody TransactionDTO dto) {
        try {
            TransactionDTO updated = loanService.updateTransaction(dto);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @PostMapping("/sales/saveTxnsData/{appId}")
    public ResponseEntity<List<TransactionDTO>> saveTransactions(
            @PathVariable Long appId,
            @RequestBody List<TransactionDTO> dtos) {
        return new ResponseEntity<>(loanService.saveTransactions(appId, dtos), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @PostMapping("/sales/readTransactionsCsv")
    public ResponseEntity<?> readTransactionsCsv(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return new ResponseEntity<>("Please upload a file", HttpStatus.BAD_REQUEST);
            }
            List<TransactionDTO> transactions = fileProcessingService.processCsvFile(file);
            return new ResponseEntity<>(transactions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
