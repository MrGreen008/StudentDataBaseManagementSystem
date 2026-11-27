CREATE TABLE college_program (
    code VARCHAR(20) PRIMARY KEY, name VARCHAR(140) NOT NULL,
    department_code VARCHAR(20) NOT NULL REFERENCES academic_department(code),
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE college_cohort (
    code VARCHAR(20) PRIMARY KEY, name VARCHAR(140) NOT NULL,
    program_code VARCHAR(20) NOT NULL REFERENCES college_program(code),
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE academic_term (
    code VARCHAR(20) PRIMARY KEY, name VARCHAR(140) NOT NULL,
    starts_on DATE NOT NULL, ends_on DATE NOT NULL CHECK (ends_on >= starts_on),
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE college_course (
    code VARCHAR(20) PRIMARY KEY, name VARCHAR(140) NOT NULL,
    department_code VARCHAR(20) NOT NULL REFERENCES academic_department(code),
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE program_specialization (
    code VARCHAR(20) PRIMARY KEY, name VARCHAR(140) NOT NULL,
    program_code VARCHAR(20) NOT NULL REFERENCES college_program(code),
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('HONORS', 'MINOR')),
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE course_offering (
    code VARCHAR(20) PRIMARY KEY, name VARCHAR(140) NOT NULL,
    course_code VARCHAR(20) NOT NULL REFERENCES college_course(code),
    cohort_code VARCHAR(20) NOT NULL REFERENCES college_cohort(code),
    term_code VARCHAR(20) NOT NULL REFERENCES academic_term(code),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (course_code, cohort_code, term_code)
);
CREATE TABLE student_profile (
    code VARCHAR(20) PRIMARY KEY, name VARCHAR(140) NOT NULL,
    account_id UUID NOT NULL UNIQUE REFERENCES portal_account(id),
    cohort_code VARCHAR(20) NOT NULL REFERENCES college_cohort(code),
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE faculty_profile (
    code VARCHAR(20) PRIMARY KEY, name VARCHAR(140) NOT NULL,
    account_id UUID NOT NULL UNIQUE REFERENCES portal_account(id),
    department_code VARCHAR(20) NOT NULL REFERENCES academic_department(code),
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE faculty_assignment (
    code VARCHAR(20) PRIMARY KEY, name VARCHAR(140) NOT NULL,
    faculty_code VARCHAR(20) NOT NULL REFERENCES faculty_profile(code),
    offering_code VARCHAR(20) NOT NULL REFERENCES course_offering(code),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (faculty_code, offering_code)
);
DO $$
DECLARE table_name TEXT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY['college_program', 'college_cohort', 'academic_term',
        'college_course', 'program_specialization', 'course_offering', 'student_profile',
        'faculty_profile', 'faculty_assignment']
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
