ALTER TABLE eligibility_rules
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
