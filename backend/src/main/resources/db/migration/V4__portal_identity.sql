CREATE TABLE portal_account (
    id UUID PRIMARY KEY,
    login VARCHAR(100) NOT NULL UNIQUE CHECK (login = lower(trim(login))),
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'FACULTY', 'STUDENT')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
    can_publish BOOLEAN NOT NULL DEFAULT FALSE,
    security_version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_portal_account_updated_at
    BEFORE UPDATE ON portal_account
    FOR EACH ROW EXECUTE FUNCTION set_row_updated_at();

-- A permanent lock row serializes bootstrap and last-administrator changes.
CREATE TABLE identity_lock (id INTEGER PRIMARY KEY CHECK (id = 1));
INSERT INTO identity_lock VALUES (1);
