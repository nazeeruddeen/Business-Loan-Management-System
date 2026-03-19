# Business Loan Management System

A robust, production-ready monolithic application for managing business loans. This project demonstrates strong foundational skills in end-to-end delivery using Spring Boot, covering the entire lifecycle from borrower onboarding to loan disbursement and servicing.

## 🎯 Core Interview Story: End-to-End Monolith Delivery
This project serves as a "Strong Foundation" resume story. It proves the ability to deliver a complete, production-grade Spring Boot application from scratch, handling complex data relationships, security, and operational readiness.

### Key Architectural Decisions & Features
*   **Caching Strategy (Redis):** Implemented `@Cacheable` and `@CacheEvict` for `LoanProduct` lookups to handle high-volume read traffic (e.g., frontend dropdowns) while ensuring data freshness on updates. Features a 10-minute TTL fallback.
*   **Scheduled Batch Jobs:** Offloaded overdue installment detection to a nightly `@Scheduled` batch process to prevent read-path latency and maintain transactional integrity over large datasets.
*   **Observability & Production Readiness:**
    *   **Actuator:** Exposed health, metrics, and info endpoints for Kubernetes readiness/liveness probes.
    *   **Structured Logging:** Configured `logstash-logback-encoder` to output pure JSON logs in staging/production, complete with MDC `correlationId` tracking for seamless ELK stack integration.
*   **API Design & Documentation:** Fully documented RESTful APIs adhering to maturity model level 2, auto-generated using Springdoc OpenAPI (Swagger UI).
*   **CI/CD Pipeline:** A complete Jenkinsfile defining stages for Build, Test, Docker Image Push (with credential management), and Kubernetes deployment rollouts.

## 🛠 Tech Stack
*   **Java 17** & **Spring Boot 3.2**
*   **Spring Data JPA / Hibernate** (MySQL)
*   **Spring Security & JWT** (Role-based access control)
*   **Spring Cache & Redis**
*   **Flyway** (Database migrations)
*   **Swagger/OpenAPI** (API Documentation)
*   **Docker & Jenkins**

## 🚀 Run Locally

**Backend:**
```bash
cd backend
mvn clean test
mvn spring-boot:run
```
*(Requires a local MySQL instance running on port 3306 and Redis on 6379, or simply relies on application.properties fallbacks)*

**Ports:**
*   API / Swagger UI: `http://localhost:8010/swagger-ui.html`
*   Actuator Health: `http://localhost:8010/actuator/health`
