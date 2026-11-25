# Vishvaya Institute of Technology — Student System Roadmap

This is the proposed release plan for the single-college BTech demo. We will refine each release before implementation. All public demo records, achievements, credentials, and student accounts must be clearly fictional/synthetic.

## Release 1 — Public college website

- Responsive public homepage for Vishvaya Institute of Technology.
- Generated campus, technology festival, field-visit, student-life, and facility concept visuals.
- College overview, expanded vision and mission, BTech departments, honours/minors, and college-at-a-glance figures. Do not show average salary packages or unverified recruiter claims; verify figures and credentials before publication.
- Public announcements for admissions, scholarships, open houses, conferences, and campus events. Keep student/faculty timetables, examination schedules, and holiday bulletins inside authenticated portals.
- Admission enquiry action; connect it to a verified college phone number before launch.
- Department carousel with academic focus areas, a campus gallery, director's message, and student stories. Obtain permission before publishing attributed testimonials.
- Dark public footer with department shortcuts, planned services, and verified contact details when available.
- Navigation entry points for student and staff portals; authentication is not part of this release.
- Lightweight help guide directing visitors to relevant information and office contacts; no general-purpose AI chatbot in this release.

## Release 2 — Identity, roles, and college setup

- Secure sign-in for administrators, faculty, and students.
- Role and scope checks: administrators manage college records; faculty access assigned classes and courses; students access their own records; the public sees published information only.
- Admin-managed accounts and core college data: departments, BTech programs, cohorts, terms, courses, faculty assignments, and honors/minors.
- Staff tools to publish and manage public announcements.
- Basic student, faculty, and admin portal dashboards.

## Release 3 — Academic operations

- Student course enrollment, timetable views, and role-scoped student/faculty bulletin boards for timetables, examination schedules, and campus holidays.
- Faculty attendance-taking for assigned class sessions, with prompt student visibility.
- Attendance history and a discrepancy-report workflow.
- Faculty planned leave and day-of availability, with clear timestamps and responsible updates.

## Release 4 — Exams, results, and student services

- Exam schedules and result publishing.
- Backlog registration, revaluation, and result-correction requests with status tracking.
- Online requests for certificates such as bona fide, with offline process details where needed.
- Scholarship and account-support routing to the relevant office/contact.
- Student-facing help panel using approved college information and support contacts.

## Cross-release requirements

- Use synthetic data for the public portfolio/demo; never publish real student personal data.
- Keep student, faculty, and administrator access separate and enforce authorization in the backend.
- Store database credentials outside tracked source files; keep PostgreSQL data persistent.
- Document local startup and deployment; deploy only with a persistent production database and backups.

## GitHub tracking model

- **GitHub Project:** `Vishvaya Student System` — board view for day-to-day work and roadmap view for release planning.
- **Issues:** one actionable item per feature, bug, or setup task; link pull requests to the issue they complete.
- **Milestones:** `R1 — Public Website`, `R2 — Identity & College Setup`, `R3 — Academic Operations`, `R4 — Exams & Student Services`.
- **Project fields:** Status (`Todo`, `In progress`, `Review`, `Done`), Release (`R1`–`R4`), Area (`Website`, `Backend`, `Database`, `Security`, `Content`), and Priority (`High`, `Medium`, `Low`).
- Move an issue to `Done` only after its acceptance criteria are met and the change is reviewed in the running application.
