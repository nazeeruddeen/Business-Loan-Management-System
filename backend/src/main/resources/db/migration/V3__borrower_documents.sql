CREATE TABLE borrower_documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    borrower_id BIGINT NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    document_status VARCHAR(30) NOT NULL,
    file_name VARCHAR(180) NOT NULL,
    file_reference VARCHAR(255) NOT NULL,
    uploaded_by VARCHAR(120) NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by VARCHAR(120) NULL,
    reviewed_at DATETIME NULL,
    remarks VARCHAR(500) NULL,
    CONSTRAINT fk_borrower_documents_borrower FOREIGN KEY (borrower_id) REFERENCES borrowers (id)
);

CREATE INDEX idx_borrower_documents_borrower ON borrower_documents (borrower_id);
CREATE INDEX idx_borrower_documents_type ON borrower_documents (document_type);
CREATE INDEX idx_borrower_documents_status ON borrower_documents (document_status);
