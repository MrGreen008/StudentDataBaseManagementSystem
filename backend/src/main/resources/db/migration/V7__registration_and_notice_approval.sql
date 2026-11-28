ALTER TABLE portal_account ADD COLUMN email VARCHAR(254),
    ADD COLUMN mobile_number VARCHAR(20), ADD COLUMN full_name_key VARCHAR(120);
CREATE UNIQUE INDEX uq_account_email ON portal_account (lower(email)) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uq_account_person ON portal_account (full_name_key,mobile_number)
    WHERE full_name_key IS NOT NULL AND mobile_number IS NOT NULL;
ALTER TABLE portal_notice ADD COLUMN approval_status VARCHAR(16) NOT NULL DEFAULT 'APPROVED'
    CHECK (approval_status IN ('DRAFT','PENDING','APPROVED','REJECTED')),
    ADD COLUMN approved_by VARCHAR(100),
    ADD COLUMN attachment_name VARCHAR(180), ADD COLUMN attachment_data BYTEA,
    ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;
CREATE TABLE password_recovery (
    account_id UUID PRIMARY KEY REFERENCES portal_account(id),
    code_hash VARCHAR(255) NOT NULL, security_version BIGINT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL, sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempts INTEGER NOT NULL DEFAULT 0
);
