-- Preserve existing timestamps and identify historical seed rows as system-managed.
ALTER TABLE college_profile
    ADD COLUMN created_at TIMESTAMPTZ,
    ADD COLUMN created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM';

UPDATE college_profile SET created_at = updated_at;
ALTER TABLE college_profile
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE campus_announcement
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD COLUMN created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM';

UPDATE campus_announcement SET updated_at = created_at;
ALTER TABLE campus_announcement
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE academic_department
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM';

ALTER TABLE admission_enquiry
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD COLUMN created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM';

UPDATE admission_enquiry SET updated_at = created_at;
ALTER TABLE admission_enquiry
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET NOT NULL;

CREATE FUNCTION set_row_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_college_profile_updated_at
    BEFORE UPDATE ON college_profile
    FOR EACH ROW EXECUTE FUNCTION set_row_updated_at();

CREATE TRIGGER trg_campus_announcement_updated_at
    BEFORE UPDATE ON campus_announcement
    FOR EACH ROW EXECUTE FUNCTION set_row_updated_at();

CREATE TRIGGER trg_academic_department_updated_at
    BEFORE UPDATE ON academic_department
    FOR EACH ROW EXECUTE FUNCTION set_row_updated_at();

CREATE TRIGGER trg_admission_enquiry_updated_at
    BEFORE UPDATE ON admission_enquiry
    FOR EACH ROW EXECUTE FUNCTION set_row_updated_at();
