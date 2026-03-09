package com.employee.loan_system.controller;

import com.employee.loan_system.dto.EmployeeDTO;
import com.employee.loan_system.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employee")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202"})
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST','VIEWER')")
    @GetMapping("/getAll")
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        return new ResponseEntity<>(employeeService.getAllEmployees(), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @PostMapping("/saveEmp")
    public ResponseEntity<EmployeeDTO> saveEmployee(@RequestBody EmployeeDTO dto) {
        return new ResponseEntity<>(employeeService.saveEmployee(dto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @PutMapping("/updateEmp")
    public ResponseEntity<EmployeeDTO> updateEmployee(@RequestBody EmployeeDTO dto) {
        return new ResponseEntity<>(employeeService.updateEmployee(dto), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @DeleteMapping("/deleteEmp/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST','VIEWER')")
    @GetMapping("/getByEmpId/{id}")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable Long id) {
        return new ResponseEntity<>(employeeService.getEmployeeById(id), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST','VIEWER')")
    @GetMapping("/search/{searchTerm}")
    public ResponseEntity<List<EmployeeDTO>> searchEmployees(@PathVariable String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty() ||
                searchTerm.equalsIgnoreCase("null") || searchTerm.equalsIgnoreCase("undefined")) {
            return new ResponseEntity<>(employeeService.getAllEmployees(), HttpStatus.OK);
        }
        return new ResponseEntity<>(employeeService.searchEmployees(searchTerm.trim()), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST','VIEWER')")
    @GetMapping("/dataSorting")
    public ResponseEntity<List<EmployeeDTO>> sortEmployees(
            @RequestParam String property,
            @RequestParam String orderType) {
        return new ResponseEntity<>(employeeService.sortEmployees(property, orderType), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST','VIEWER')")
    @GetMapping("/searchFilters")
    public ResponseEntity<List<EmployeeDTO>> filterEmployees(
            @RequestParam String filterType,
            @RequestParam String empCode) {
        return new ResponseEntity<>(employeeService.filterEmployees(filterType, empCode), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','ANALYST','VIEWER')")
    @GetMapping("/factoryDesign/{type}")
    public ResponseEntity<byte[]> exportEmployees(@PathVariable String type) {
        byte[] data = employeeService.exportEmployees(type);

        String extension = switch (type.toLowerCase()) {
            case "pdf" -> "pdf";
            case "docx" -> "docx";
            case "xlsx" -> "xlsx";
            case "txt" -> "txt";
            default -> "dat";
        };

        String contentType = switch (type.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", "employees." + extension);
        headers.setContentLength(data.length);

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
