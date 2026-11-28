CREATE TABLE college_exam (
    id UUID PRIMARY KEY,
    offering_code VARCHAR(20) NOT NULL REFERENCES course_offering(code),
    title VARCHAR(140) NOT NULL,
    kind VARCHAR(8) NOT NULL CHECK (kind IN ('REGULAR','BACKLOG')),
    room VARCHAR(80) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL CHECK (ends_at > starts_at),
    max_score NUMERIC(7,2) NOT NULL CHECK (max_score > 0),
    pass_score NUMERIC(7,2) NOT NULL CHECK (pass_score > 0 AND pass_score <= max_score),
    state VARCHAR(12) NOT NULL DEFAULT 'DRAFT' CHECK (state IN ('DRAFT','SCHEDULED','CANCELLED','RELEASED')),
    released_at TIMESTAMPTZ,
    revision BIGINT NOT NULL DEFAULT 0,
    CHECK ((state='RELEASED') = (released_at IS NOT NULL))
);
CREATE INDEX college_exam_offering ON college_exam (offering_code, starts_at);

CREATE TABLE exam_result (
    exam_id UUID REFERENCES college_exam(id),
    student_id UUID REFERENCES portal_account(id),
    score NUMERIC(7,2) NOT NULL CHECK (score >= 0),
    revision BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (exam_id, student_id)
);
CREATE INDEX exam_result_student ON exam_result(student_id);

CREATE TABLE student_service_route (
    kind VARCHAR(20) PRIMARY KEY CHECK (kind IN ('BACKLOG','REVALUATION','RESULT_CORRECTION','BONA_FIDE','SCHOLARSHIP','ACCOUNT_SUPPORT')),
    office VARCHAR(120) NOT NULL,
    instructions VARCHAR(1000) NOT NULL,
    email VARCHAR(254),
    revision BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE student_service_request (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES portal_account(id),
    kind VARCHAR(20) NOT NULL REFERENCES student_service_route(kind),
    exam_id UUID REFERENCES college_exam(id),
    details VARCHAR(1000) NOT NULL,
    state VARCHAR(12) NOT NULL DEFAULT 'SUBMITTED' CHECK (state IN ('SUBMITTED','IN_REVIEW','APPROVED','REJECTED','COMPLETED')),
    response VARCHAR(1000),
    reviewed_by UUID REFERENCES portal_account(id),
    reviewed_at TIMESTAMPTZ,
    result_revision BIGINT,
    old_score NUMERIC(7,2),
    new_score NUMERIC(7,2),
    revision BIGINT NOT NULL DEFAULT 0,
    CHECK ((kind IN ('BACKLOG','REVALUATION','RESULT_CORRECTION')) = (exam_id IS NOT NULL)),
    CHECK ((kind IN ('REVALUATION','RESULT_CORRECTION')) = (result_revision IS NOT NULL))
);
CREATE INDEX student_service_student ON student_service_request (student_id);
CREATE INDEX student_service_exam ON student_service_request (exam_id);
CREATE UNIQUE INDEX student_service_open_exam ON student_service_request (student_id, exam_id)
    WHERE exam_id IS NOT NULL AND state IN ('SUBMITTED','IN_REVIEW');
CREATE UNIQUE INDEX student_service_backlog_approved ON student_service_request (student_id, exam_id)
    WHERE kind='BACKLOG' AND state='APPROVED';

DO $$
DECLARE table_name TEXT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY['college_exam','exam_result','student_service_route','student_service_request']
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

INSERT INTO student_service_route (kind,office,instructions,created_by,updated_by) VALUES
    ('BACKLOG','Examination Office','Open Exam schedules to find an eligible backlog sitting. Track the registration decision under Student requests.','SYSTEM','SYSTEM'),
    ('REVALUATION','Examination Office','Open Results and request a revaluation for the exam. Check Student requests for the decision.','SYSTEM','SYSTEM'),
    ('RESULT_CORRECTION','Examination Office','Open Results and describe the discrepancy against the affected exam.','SYSTEM','SYSTEM'),
    ('BONA_FIDE','Student Records Office','Submit the purpose for your certificate. Check the request response for collection instructions before visiting the office.','SYSTEM','SYSTEM'),
    ('SCHOLARSHIP','Scholarship Office','Submit a request for guidance on eligibility, documents or deadlines. Check the response for the next step.','SYSTEM','SYSTEM'),
    ('ACCOUNT_SUPPORT','Portal Support','Describe the issue and where it occurs. Keep passwords and verification codes private.','SYSTEM','SYSTEM');
