ALTER TABLE loan_repayment_transactions
    ADD COLUMN prepayment_principal_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00
    AFTER applied_principal_amount;
