CREATE TABLE college_profile (
    profile_key VARCHAR(32) PRIMARY KEY,
    college_name VARCHAR(180) NOT NULL,
    city VARCHAR(120) NOT NULL,
    student_count BIGINT NOT NULL CHECK (student_count >= 0),
    faculty_count BIGINT NOT NULL CHECK (faculty_count >= 0),
    publication_count BIGINT NOT NULL CHECK (publication_count >= 0),
    patent_count BIGINT NOT NULL CHECK (patent_count >= 0),
    project_count BIGINT NOT NULL CHECK (project_count >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE campus_announcement (
    id UUID PRIMARY KEY,
    category VARCHAR(40) NOT NULL,
    title VARCHAR(180) NOT NULL,
    summary VARCHAR(600) NOT NULL,
    action_label VARCHAR(80),
    action_url VARCHAR(300),
    priority INTEGER NOT NULL DEFAULT 0,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (expires_at IS NULL OR expires_at > published_at)
);

CREATE INDEX idx_announcement_public_order
    ON campus_announcement (published, priority DESC, published_at DESC);

CREATE TABLE academic_department (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(140) NOT NULL UNIQUE,
    display_order SMALLINT NOT NULL UNIQUE,
    visible BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE admission_enquiry (
    id UUID PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    email VARCHAR(254),
    programme_of_interest VARCHAR(120),
    message VARCHAR(1500) NOT NULL,
    privacy_consent BOOLEAN NOT NULL CHECK (privacy_consent = TRUE),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'IN_REVIEW', 'RESOLVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admission_enquiry_status_created
    ON admission_enquiry (status, created_at DESC);

INSERT INTO college_profile
    (profile_key, college_name, city, student_count, faculty_count, publication_count, patent_count, project_count)
VALUES
    ('public', 'Vishvaya Institute of Technology', 'Nagpur', 1248, 86, 142, 19, 74);

INSERT INTO academic_department (code, name, display_order) VALUES
    ('cse', 'Computer Science and Engineering', 1),
    ('ct', 'Computer Technology', 2),
    ('it', 'Information Technology', 3),
    ('aiml', 'Artificial Intelligence and Machine Learning', 4),
    ('aids', 'Artificial Intelligence and Data Science', 5),
    ('ece', 'Electronics and Communication Engineering', 6),
    ('eee', 'Electrical and Electronics Engineering', 7),
    ('me', 'Mechanical Engineering', 8),
    ('ce', 'Civil Engineering', 9),
    ('chem', 'Chemical Engineering', 10),
    ('robotics', 'Robotics and Automation', 11),
    ('design', 'Computer Science and Design', 12);

INSERT INTO campus_announcement
    (id, category, title, summary, action_label, action_url, priority, published)
VALUES
    ('a888d857-5af0-4d21-8956-a4cd3a1c1a01', 'ADMISSIONS', 'Explore engineering at Vishvaya', 'Find programme information, application guidance, and important admission dates.', 'Admission enquiry', '#admissions', 40, TRUE),
    ('a888d857-5af0-4d21-8956-a4cd3a1c1a02', 'SCHOLARSHIPS', 'Scholarship information', 'Scholarship eligibility and deadlines are published by the scholarship office.', 'Scholarship office', '#contact', 30, TRUE),
    ('a888d857-5af0-4d21-8956-a4cd3a1c1a03', 'CAMPUS EVENT', 'Engineering open house', 'Meet our departments, discover student projects, and explore the campus.', 'Explore campus life', '#campus', 20, TRUE),
    ('a888d857-5af0-4d21-8956-a4cd3a1c1a04', 'CONFERENCE', 'Technology and society forum', 'Explore current research and ideas shaping the future of engineering and society.', 'See student activities', '#student-voices', 10, TRUE);
