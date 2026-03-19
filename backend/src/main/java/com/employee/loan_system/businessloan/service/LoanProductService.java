package com.employee.loan_system.businessloan.service;

import com.employee.loan_system.businessloan.dto.CreateLoanProductRequest;
import com.employee.loan_system.businessloan.dto.LoanProductResponse;
import com.employee.loan_system.businessloan.entity.LoanProduct;
import com.employee.loan_system.businessloan.repository.LoanProductRepository;
import com.employee.loan_system.exception.DuplicateResourceException;
import com.employee.loan_system.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanProductService {

    private final LoanProductRepository loanProductRepository;

    public LoanProductService(LoanProductRepository loanProductRepository) {
        this.loanProductRepository = loanProductRepository;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
    @CacheEvict(value = "loanProducts", allEntries = true)
    public LoanProductResponse createProduct(CreateLoanProductRequest request) {
        if (request.getMinAmount().compareTo(request.getMaxAmount()) > 0) {
            throw new IllegalArgumentException("Minimum amount cannot be greater than maximum amount");
        }

        String normalizedCode = request.getProductCode().trim().toUpperCase();
        if (loanProductRepository.existsByProductCodeIgnoreCase(normalizedCode)) {
            throw new DuplicateResourceException("Loan product already exists with code: " + normalizedCode);
        }

        LoanProduct product = new LoanProduct();
        product.setProductCode(normalizedCode);
        product.setName(request.getName().trim());
        product.setMinAmount(request.getMinAmount());
        product.setMaxAmount(request.getMaxAmount());
        product.setInterestRate(request.getInterestRate());
        product.setTenureMonths(request.getTenureMonths());
        product.setEligibilityCriteria(request.getEligibilityCriteria());
        product.setActive(request.isActive());

        return toResponse(loanProductRepository.save(product));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER','BORROWER')")
    public LoanProductResponse getProduct(Long productId) {
        return toResponse(loanProductRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found with id: " + productId)));
    }

    /**
     * Cache key includes all filter params. TTL-based expiry configured in Redis (10 min).
     * Interview answer: "We cache product listings since loan products are configured infrequently
     * but read on every application creation. Cache is evicted on any write to prevent stale data."
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','REVIEWER','BORROWER')")
    @Cacheable(value = "loanProducts", unless = "#result.isEmpty()")
    public List<LoanProductResponse> searchProducts(String name, Boolean active, BigDecimal amount, Integer maxTenureMonths) {
        Specification<LoanProduct> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%"));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (amount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minAmount"), amount));
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxAmount"), amount));
            }
            if (maxTenureMonths != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("tenureMonths"), maxTenureMonths));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return loanProductRepository.findAll(specification).stream()
                .map(this::toResponse)
                .toList();
    }

    private LoanProductResponse toResponse(LoanProduct product) {
        return LoanProductResponse.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .name(product.getName())
                .minAmount(product.getMinAmount())
                .maxAmount(product.getMaxAmount())
                .interestRate(product.getInterestRate())
                .tenureMonths(product.getTenureMonths())
                .eligibilityCriteria(product.getEligibilityCriteria())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
