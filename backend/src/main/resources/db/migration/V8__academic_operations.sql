CREATE TABLE academic_enrollment (
    offering_code VARCHAR(20) REFERENCES course_offering(code),
    student_id UUID REFERENCES portal_account(id),
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL CHECK (ends_on >= starts_on),
    revision BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (offering_code, student_id)
);
CREATE INDEX academic_enrollment_student ON academic_enrollment (student_id);

CREATE TABLE academic_session (
    id UUID PRIMARY KEY,
    offering_code VARCHAR(20) NOT NULL REFERENCES course_offering(code),
    instructor_id UUID NOT NULL REFERENCES portal_account(id),
    cohort_code VARCHAR(20) NOT NULL REFERENCES college_cohort(code),
    room VARCHAR(80) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL CHECK (ends_at > starts_at),
    state VARCHAR(12) NOT NULL DEFAULT 'SCHEDULED' CHECK (state IN ('SCHEDULED','CANCELLED','SUBMITTED')),
    revision BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX academic_session_schedule ON academic_session (starts_at);
CREATE INDEX academic_session_offering ON academic_session (offering_code);
CREATE INDEX academic_session_instructor ON academic_session (instructor_id);

CREATE TABLE attendance_mark (
    session_id UUID REFERENCES academic_session(id),
    student_id UUID REFERENCES portal_account(id),
    mark VARCHAR(8) NOT NULL CHECK (mark IN ('PRESENT','ABSENT')),
    submitted_mark VARCHAR(8) NOT NULL CHECK (submitted_mark IN ('PRESENT','ABSENT')),
    revision BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (session_id, student_id)
);
CREATE INDEX attendance_mark_student ON attendance_mark (student_id);

CREATE TABLE attendance_report (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    student_id UUID NOT NULL,
    explanation VARCHAR(1000) NOT NULL,
    original_mark VARCHAR(8) NOT NULL CHECK (original_mark IN ('PRESENT','ABSENT')),
    mark_revision BIGINT NOT NULL,
    decision VARCHAR(8) NOT NULL DEFAULT 'OPEN' CHECK (decision IN ('OPEN','ACCEPTED','REJECTED')),
    corrected_mark VARCHAR(8) CHECK (corrected_mark IN ('PRESENT','ABSENT')),
    review_reason VARCHAR(1000),
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES portal_account(id),
    revision BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (session_id, student_id) REFERENCES attendance_mark(session_id, student_id)
);
CREATE UNIQUE INDEX attendance_report_open ON attendance_report (session_id, student_id) WHERE decision='OPEN';

CREATE TABLE faculty_availability (
    id UUID PRIMARY KEY,
    instructor_id UUID NOT NULL REFERENCES portal_account(id),
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL CHECK (ends_on >= starts_on),
    state VARCHAR(12) NOT NULL CHECK (state IN ('LEAVE','AVAILABLE','UNAVAILABLE','WITHDRAWN')),
    daily BOOLEAN NOT NULL,
    notes VARCHAR(500),
    revision BIGINT NOT NULL DEFAULT 0,
    CHECK (NOT daily OR starts_on=ends_on)
);
CREATE INDEX faculty_availability_instructor ON faculty_availability (instructor_id, starts_on);

ALTER TABLE portal_notice ADD COLUMN topic VARCHAR(20) NOT NULL DEFAULT 'GENERAL'
    CHECK (topic IN ('GENERAL','TIMETABLE','EXAM_DATES','HOLIDAY'));

DO $$
DECLARE table_name TEXT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY['academic_enrollment','academic_session',
        'attendance_mark','attendance_report','faculty_availability']
    LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN created_by VARCHAR(100) NOT NULL,
            ADD COLUMN updated_by VARCHAR(100) NOT NULL,
            ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP', table_name);
        EXECUTE format('CREATE TRIGGER updated_at BEFORE UPDATE ON %I
            FOR EACH ROW EXECUTE FUNCTION set_row_updated_at()', table_name);
    END LOOP;
END;
$$;
