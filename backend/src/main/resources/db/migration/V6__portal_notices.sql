CREATE TABLE portal_notice (
    id UUID PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    audience VARCHAR(20) NOT NULL CHECK (audience IN ('STUDENT', 'FACULTY', 'ADMIN')),
    cohort_code VARCHAR(20) REFERENCES college_cohort(code),
    account_id UUID REFERENCES portal_account(id),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (cohort_code IS NULL OR audience = 'STUDENT'),
    CHECK (expires_at IS NULL OR expires_at > published_at)
);
CREATE INDEX idx_portal_notice_audience ON portal_notice (audience, published_at DESC);
CREATE TRIGGER trg_portal_notice_updated_at BEFORE UPDATE ON portal_notice
    FOR EACH ROW EXECUTE FUNCTION set_row_updated_at();
