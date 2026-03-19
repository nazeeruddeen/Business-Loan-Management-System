package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.DisbursementReportItem;
import com.employee.loan_system.businessloan.dto.DisbursementReportResponse;
import com.employee.loan_system.businessloan.entity.LoanAccount;
import com.employee.loan_system.businessloan.repository.LoanAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class LoanReportService {

    private static final DateTimeFormatter CSV_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LoanAccountRepository loanAccountRepository;

    public LoanReportService(LoanAccountRepository loanAccountRepository) {
        this.loanAccountRepository = loanAccountRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public DisbursementReportResponse getDisbursementReport(LocalDate from, LocalDate to, Pageable pageable) {
        LocalDateTime start = startOfDayOrDefault(from);
        LocalDateTime end = endOfDayOrDefault(to);

        Page<LoanAccount> page = loanAccountRepository.findByDisbursedAtBetweenOrderByDisbursedAtDesc(start, end, pageable);
        List<DisbursementReportItem> items = page.stream().map(this::toItem).toList();

        long disbursedCount = loanAccountRepository.countByDisbursedAtBetween(start, end);
        BigDecimal totalPrincipalDisbursed = loanAccountRepository.sumPrincipalDisbursedBetween(start, end).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalOutstanding = loanAccountRepository.sumOutstandingPrincipalBetween(start, end).setScale(2, RoundingMode.HALF_UP);

        return DisbursementReportResponse.builder()
                .fromDate(from)
                .toDate(to)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .disbursedCount(disbursedCount)
                .totalPrincipalDisbursed(totalPrincipalDisbursed)
                .totalOutstandingPrincipal(totalOutstanding)
                .items(items)
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER')")
    public StreamingResponseBody exportDisbursementCsv(LocalDate from, LocalDate to) {
        LocalDateTime start = startOfDayOrDefault(from);
        LocalDateTime end = endOfDayOrDefault(to);
        List<DisbursementReportItem> items = loanAccountRepository.findByDisbursedAtBetweenOrderByDisbursedAtDesc(start, end)
                .stream()
                .map(this::toItem)
                .toList();

        return outputStream -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                writer.write("accountNumber,applicationId,borrowerName,productCode,principalAmount,outstandingPrincipal,status,disbursedAt,nextDueDate");
                writer.newLine();
                for (DisbursementReportItem item : items) {
                    writer.write(csv(item.accountNumber()));
                    writer.write(',');
                    writer.write(String.valueOf(item.applicationId()));
                    writer.write(',');
                    writer.write(csv(item.borrowerName()));
                    writer.write(',');
                    writer.write(csv(item.productCode()));
                    writer.write(',');
                    writer.write(csv(item.principalAmount()));
                    writer.write(',');
                    writer.write(csv(item.outstandingPrincipal()));
                    writer.write(',');
                    writer.write(csv(item.status().name()));
                    writer.write(',');
                    writer.write(csv(item.disbursedAt().format(CSV_TIMESTAMP_FORMATTER)));
                    writer.write(',');
                    writer.write(csv(item.nextDueDate().toString()));
                    writer.newLine();
                }
                writer.flush();
            }
        };
    }

    private DisbursementReportItem toItem(LoanAccount account) {
        return DisbursementReportItem.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .applicationId(account.getLoanApplication().getId())
                .borrowerName(account.getLoanApplication().getBorrower().getLegalBusinessName())
                .productCode(account.getLoanApplication().getLoanProduct().getProductCode())
                .principalAmount(account.getPrincipalAmount())
                .outstandingPrincipal(account.getOutstandingPrincipal())
                .status(account.getStatus())
                .disbursedAt(account.getDisbursedAt())
                .nextDueDate(account.getNextDueDate())
                .build();
    }

    private LocalDateTime startOfDayOrDefault(LocalDate from) {
        return (from == null ? LocalDate.of(2000, 1, 1) : from).atStartOfDay();
    }

    private LocalDateTime endOfDayOrDefault(LocalDate to) {
        return (to == null ? LocalDate.now().plusYears(50) : to).atTime(23, 59, 59);
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        boolean quoted = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        return quoted ? "\"" + text + "\"" : text;
    }
}
