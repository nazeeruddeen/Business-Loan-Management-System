package com.employee.loan_system.businessloan.config;

import com.employee.loan_system.businessloan.entity.ApplicationStatus;
import com.employee.loan_system.businessloan.entity.LoanAccountStatus;
import com.employee.loan_system.businessloan.repository.LoanAccountRepository;
import com.employee.loan_system.businessloan.repository.LoanApplicationRepository;
import com.employee.loan_system.businessloan.repository.LoanRepaymentTransactionRepository;
import com.employee.loan_system.businessloan.repository.RepaymentInstallmentRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class BusinessLoanMetricsConfiguration {

    @Bean
    public MeterBinder businessLoanWorkflowMetricsBinder(
            LoanApplicationRepository loanApplicationRepository,
            LoanAccountRepository loanAccountRepository,
            RepaymentInstallmentRepository repaymentInstallmentRepository,
            LoanRepaymentTransactionRepository loanRepaymentTransactionRepository) {
        return registry -> {
            Gauge.builder("business.loan.applications.count", loanApplicationRepository::count)
                    .description("Total business loan applications")
                    .register(registry);

            for (ApplicationStatus status : ApplicationStatus.values()) {
                Gauge.builder("business.loan.applications.status.count",
                                () -> loanApplicationRepository.countByStatus(status))
                        .description("Business loan applications by workflow status")
                        .tag("status", status.name().toLowerCase())
                        .register(registry);
            }

            for (LoanAccountStatus status : LoanAccountStatus.values()) {
                Gauge.builder("business.loan.accounts.status.count",
                                () -> loanAccountRepository.countByStatus(status))
                        .description("Business loan accounts by lifecycle status")
                        .tag("status", status.name().toLowerCase())
                        .register(registry);
            }

            Gauge.builder("business.loan.review.backlog.count",
                            () -> loanApplicationRepository.countByStatus(ApplicationStatus.SUBMITTED)
                                    + loanApplicationRepository.countByStatus(ApplicationStatus.UNDER_REVIEW))
                    .description("Submitted and under-review applications waiting in the decision queue")
                    .register(registry);

            Gauge.builder("business.loan.installments.overdue.count",
                            () -> repaymentInstallmentRepository.countOverdue(LocalDate.now()))
                    .description("Installments currently overdue")
                    .register(registry);

            Gauge.builder("business.loan.portfolio.amount",
                            () -> decimalValue(loanAccountRepository.sumPrincipalDisbursed()))
                    .description("Business loan portfolio amount snapshots")
                    .tag("kind", "principal_disbursed")
                    .baseUnit("currency")
                    .register(registry);

            Gauge.builder("business.loan.portfolio.amount",
                            () -> decimalValue(loanAccountRepository.sumOutstandingPrincipalByStatus(LoanAccountStatus.ACTIVE)))
                    .description("Business loan portfolio amount snapshots")
                    .tag("kind", "outstanding_principal")
                    .baseUnit("currency")
                    .register(registry);

            Gauge.builder("business.loan.portfolio.amount",
                            () -> decimalValue(loanRepaymentTransactionRepository.sumAmount()))
                    .description("Business loan portfolio amount snapshots")
                    .tag("kind", "repaid_amount")
                    .baseUnit("currency")
                    .register(registry);
        };
    }

    private double decimalValue(BigDecimal value) {
        return value == null ? 0.0d : value.doubleValue();
    }
}
