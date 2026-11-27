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

### Identity and permissions design

Use one same-origin Spring Security sign-in flow for all three portals. Resolve the dashboard from the account's role after authentication. Store an immutable account ID, unique normalized login, adaptive password hash, role, enabled flag, password-change requirement, and security version in PostgreSQL. Keep the authenticated account ID and security version in a server-side session. Rotate the session ID on sign-in, expire idle sessions, use an HttpOnly and SameSite=Lax cookie, and require Secure cookies over HTTPS in QA and production.

Allow an operator to provision the first administrator once with credentials supplied outside Git. Serialize bootstrap and make restarts idempotent. Administrators provision subsequent accounts and issue temporary passwords through a private channel. Require a password change before granting portal access. Use a generic sign-in failure and throttle repeated attempts. On reset, deactivation, or permission change, increment the security version in the same transaction. Check the version and enabled flag on each protected request. Serialize changes that could remove the last active administrator.

| Actor | Allowed actions and backend scope |
| --- | --- |
| Public visitor | Read the published public site and submit an admission enquiry. Keep the existing anonymous JSON enquiry contract and its fixed PUBLIC audit actor independent of a portal session. |
| Student | Read their own profile, program and cohort through a self-service endpoint. Resolve the student ID from the principal. |
| Faculty | Read their own profile and assigned offerings. Scope both list and detail queries by faculty account ID. |
| Authorized publisher | Administrators may manage announcements. A faculty member needs an explicit administrator-granted publishing permission for the same tools. |
| Administrator | Provision, reset and deactivate accounts, maintain college setup and assign faculty. Attribute each authenticated write to the immutable account ID. |

Allow public routes by HTTP method and path. Require explicit authority for protected APIs and deny unmatched routes. Keep CSRF protection for sign-in, password changes, logout and authenticated mutations. Supply a same-origin CSRF token to the portal frontend and refresh it after authentication. Limit any public-write exception to the existing JSON enquiry POST. Send private responses with `Cache-Control: no-store`. Return 401 for unauthenticated private API requests, 403 for prohibited operations, and 404 for records outside a user's authorized query scope. Preserve these responses through the existing API exception handler.

### College setup and publication

Append Flyway migrations after V3. Reuse `academic_department` and `campus_announcement` with their existing keys and audit metadata. Link programs to departments, cohorts to programs, and honors/minors to the programs offering them. Give students a unique account-linked profile and a cohort. Give faculty a unique account-linked profile. Represent an offering as a course taught to a cohort in a term, with a unique faculty-to-offering assignment. Enforce references and uniqueness in PostgreSQL. Reject removal of referenced catalog records and support retirement where records must survive for historical use.

Build the admin dashboard around account and catalog management, the faculty dashboard around assignments, and the student dashboard around the signed-in student's college record. Show publishing controls to authorized staff. Use the existing announcement publication flag and publication and expiry timestamps. Make publish, unpublish and catalog visibility changes effective on the public site after commit. Account for scheduled changes when caching announcements. Render the API's published collection even when it is empty, and render added or hidden departments correctly. Keep the standalone offline preview distinct from live data.

### Implementation order and acceptance checks

Local release review covered the populated V3-to-V5 upgrade, identity lifecycle, scoped profile and assignment queries, assignment retirement and announcement publication. Chrome verification used an administrator, two faculty and two students in separate sessions. Checks covered account provisioning, college setup, publication and withdrawal, loss of publisher access, sign-out, and dashboards at a 390-pixel viewport. College forms use manually entered reference codes and account IDs. QA promotion is a separate release step.

The portal UI refinement adds separate administration sections, an account menu for password changes and sign-out, explicit create and edit forms, role selectors and labeled record cards. Existing record codes are read-only during editing. Further UI work can replace manual reference codes with record selectors and add search to long lists.

1. **Identity.** Deliver bootstrap, sign-in, required password change, logout, and revocation together. Verify repeated bootstrap preserves credentials, resets invalidate old sessions, a disabled account loses access on its next request, and concurrent admin changes preserve an active administrator.
2. **College setup and scope.** Deliver catalog migrations, admin workflows and scoped queries. Exercise fresh installation and upgrade from populated V3 data. Verify two students see only their own records, and two faculty with different assignments see only their assigned offerings. Removing an assignment must affect the next request.
3. **Portal and publication workflows.** Use each homepage portal link to sign in and reach the appropriate dashboard. Create a catalog entry, assign faculty, and publish then withdraw a notice through the UI. Verify the public homepage handles empty results, scheduling and expiration, and still accepts anonymous enquiries.
4. **Release review.** Exercise allowed and denied API calls directly as well as through the UI. Cover missing or invalid CSRF tokens, logged-out sessions, role changes, publisher permission removal, cross-account reads and failed writes. Check that persisted audit actors match the authenticated account and that logs and responses omit secrets. Review the running application before promotion.

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
