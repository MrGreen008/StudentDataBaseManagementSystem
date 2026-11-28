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

### Registration and notice workflow extensions

- The selected Student, Faculty or Administration portal is checked by the backend during sign-in. Username or email is accepted.
- Student and faculty demo registration uses a CAPTCHA and unique username, email and normalized name/mobile combination. Administrators provision additional administrators. All administrators have the same authority.
- Registration emails a generated temporary password. Initial sign-in requires a password change. OTP recovery uses a ten-minute, single-use code with attempt and resend limits. Existing accounts can set a recovery email with current-password confirmation.
- Administrators see all portal notices. Faculty read published student and faculty notices and submit notices for assigned student cohorts. Each faculty submission requires administrator approval. Students read only their applicable published notices.
- Optional PDFs are stored with portal notices and downloaded with backend access checks. A faculty edit or attachment change requires renewed approval.
- Management tables and Overview refresh when revisited. Form-local feedback and expiring page messages explain failed saves. Light/dark themes persist in the browser.
- Local verification covers account creation, both self-registration roles, email sign-in, forced password changes, OTP recovery, approval and downloads. SMTP delivery was verified with a loopback test mailbox. Live Gmail delivery requires the local SMTP settings documented in the backend guide.
- CAPTCHA layout and feedback refinements place the image beside its entry field, prevent overlapping refreshes, and show action-specific rate limits with a countdown. Mail authentication failures preserve their original form message and produce a safe diagnostic category.
- Registration success now handles an empty HTTP 201 response correctly. Four frontend regression cases cover empty success bodies, normal JSON, malformed responses and server error messages.

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

Local release review covered the populated V3-to-V6 upgrade, identity lifecycle, scoped profile and assignment queries, assignment retirement and announcement publication. Chrome verification used an administrator, two faculty and two students in separate sessions. Checks covered account provisioning, college setup, publication and withdrawal, loss of publisher access, sign-out, and dashboards at a 390-pixel viewport. College forms use linked-record dropdowns and searchable tables. Latest verification also covered table search, dropdown-based college setup and an individually targeted notice visible only to its intended student. Three automated tests passed with no skips, including faculty notice creation and denial after teaching assignment removal. QA promotion is a separate release step.

The portal UI refinement adds photographic role-specific sign-in screens, a separate Overview dashboard, account settings, searchable tables, linked-record selectors and local date/time pickers. Existing record codes are read-only during editing. Targeted portal notices extend the R2 publication tools to role, cohort and individual audiences, with faculty publishing scoped to assigned cohorts. Exam-form and graduation templates create unpublished drafts. Results, attendance and student-service workflows retain their release scope below.

1. **Identity.** Deliver bootstrap, sign-in, required password change, logout, and revocation together. Verify repeated bootstrap preserves credentials, resets invalidate old sessions, a disabled account loses access on its next request, and concurrent admin changes preserve an active administrator.
2. **College setup and scope.** Deliver catalog migrations, admin workflows and scoped queries. Exercise fresh installation and upgrade from populated V3 data. Verify two students see only their own records, and two faculty with different assignments see only their assigned offerings. Removing an assignment must affect the next request.
3. **Portal and publication workflows.** Use each homepage portal link to sign in and reach the appropriate dashboard. Create a catalog entry, assign faculty, and publish then withdraw a notice through the UI. Verify the public homepage handles empty results, scheduling and expiration, and still accepts anonymous enquiries.
4. **Release review.** Exercise allowed and denied API calls directly as well as through the UI. Cover missing or invalid CSRF tokens, logged-out sessions, role changes, publisher permission removal, cross-account reads and failed writes. Check that persisted audit actors match the authenticated account and that logs and responses omit secrets. Review the running application before promotion.

## Release 3 — Academic operations

- Student course enrollment, timetable views, and role-scoped student/faculty bulletin boards for timetables, examination schedules, and campus holidays.
- Faculty attendance-taking for assigned class sessions, with prompt student visibility.
- Attendance history and a discrepancy-report workflow.
- Faculty planned leave and day-of availability, with clear timestamps and responsible updates.

### Academic workflows and permissions

| Portal | Workflow |
| --- | --- |
| Administration | Link students to offerings for their cohort and term, schedule classes with an assigned instructor and room, review attendance discrepancies, and maintain faculty availability. All administrators share these permissions. |
| Faculty | View a weekly teaching timetable, mark attendance for their own scheduled classes, review discrepancies for those classes, and enter planned leave or update today's availability. |
| Student | View enrolled courses, a weekly timetable, and personal attendance history. Report a discrepancy against a specific published mark and follow its outcome. A demo account awaiting a linked college profile sees an explanation in its academic views. |

**Enrollment and timetables.** Reuse the R2 student and faculty profiles, cohorts, terms, offerings and teaching assignments. Administrators manage enrollment with effective dates within the term. Ending an enrollment preserves the student's past classes and marks. For each dated class, store the offering, designated instructor, room, start and end. Show the week in campus time (`Asia/Kolkata`). Require an active teaching assignment and dates within the term. Prevent overlapping classes for a room, instructor or cohort, including simultaneous scheduling requests. One class may start when the preceding class ends.

**Attendance and corrections.** Use the class date to determine its enrolled roster. Allow the assigned instructor to submit a complete set of Present or Absent marks once the class starts. Until submission the portal displays “Awaiting attendance.” Save the roster and marks in one transaction. Each student can see their own marks on the next read. Preserve the published roster when enrollments change. A student may open one unresolved discrepancy per mark. The assigned instructor or an administrator records a decision and reason. An accepted correction updates the mark and closes the report together, retaining the previous mark, reviewer and timestamp. Check revisions on writes so a stale tab cannot overwrite another decision. Removing a teaching assignment ends that instructor's access on the next request.

**Availability and bulletins.** Faculty can record a dated leave interval and a day-of availability update. Show the effective status and last update to affected students alongside their instructor's timetable. Keep private leave details within staff scope. An absence flags affected sessions for the administrator to reschedule or cancel explicitly. For timetable changes, examination dates and holidays, reuse the R2 portal notice, audience, approval and PDF workflows. Older notices retain their existing audiences. Structured exam administration, grades and results belong to Release 4.

### Persistence and implementation order

1. **Enrollments and sessions.** Append migrations after V7. Reference the existing offering and store the student's immutable account ID on the enrollment. Store the scheduled instructor's immutable account ID and cohort on the session. Recheck the active profile and teaching assignment when authorizing faculty access. These links keep historical marks associated with the right person if an administrator edits a profile. Store session instants in PostgreSQL and interpret the term and enrollment date boundaries in campus time. Serialize checks and writes for competing timetable edits within the database. Retain cancelled sessions with their status. A submitted register fixes the date, offering and instructor for that session.
2. **Registers and reports.** Identify a mark by session and student account, with a database uniqueness constraint. Lock the session when submitting its register and recheck the eligible roster before commit. Record corrections against that mark. Enforce at most one open report per mark in PostgreSQL and check the report and mark revisions together when resolving it. An identical retry should preserve the first outcome. Conflicting edits should invite the user to reload. Include actor IDs and timestamps on academic writes.
3. **Portal views.** Use the existing role-prefixed API paths. Derive the student or faculty identity from the signed-in session. Add weekly navigation, a class roster for the instructor, a personal attendance history for the student and an administrator's review queue. On the academic bulletin board, filter academic topics within the existing notice audience query and retain the attachment checks. Show day-specific availability ahead of the broader leave interval for the same day, with its update time.

### Acceptance before release

1. Upgrade a populated R2 database and exercise a fresh installation. Existing accounts and notices still work. Academic records start empty until entered for the demo.
2. Enroll a synthetic student and schedule their class. Verify the student and assigned instructor see it in the correct week. Try an overlapping room, instructor and cohort, adjacent classes and concurrent bookings. Check that a rejected write leaves the timetable intact.
3. Submit a class register, refresh as two different students, and verify each sees only their own mark. Exercise an incomplete roster, a duplicate submission, a stale edit and a removed assignment. A later enrollment change must preserve published history.
4. Have a student report an incorrect mark. Exercise acceptance and rejection, verify the student's updated history and decision, and confirm a second reviewer cannot overwrite the first decision.
5. Change faculty availability and verify the affected timetable, update time and privacy of staff details. Publish and withdraw an academic bulletin through the existing approval flow and check its audience and attachment access.
6. Use separate administrator, faculty and student browser sessions on desktop and mobile widths. Exercise the same permissions through HTTP, including access to another student's attendance and an unassigned class.

Local verification passed on PostgreSQL. The integration suite upgraded an existing R2 notice and checked term dates and concurrent scheduling. Chrome exercised the three portals, attendance submission and correction, rejected discrepancies, enrollment changes after submission, leave and day-of availability, and targeted bulletin publication and withdrawal. Separate sessions verified student and instructor scope. QA promotion remains a separate release step.

## Release 4 — Exams, results, and student services

- Exam schedules and result publishing.
- Backlog registration, revaluation, and result-correction requests with status tracking.
- Online requests for certificates such as bona fide, with offline process details where needed.
- Scholarship and account-support routing to the relevant office/contact.
- Student-facing help panel using approved college information and support contacts.

Administrators set an exam's maximum score and passing threshold, publish its schedule, enter draft marks, and release results for the eligible roster together. Students see only their own published results. Faculty see published exam schedules for assigned offerings. Regular exams use the offering's enrollment dates. Backlog registration is available for a published upcoming backlog exam on the same offering when a student has an outstanding failed result. The administration reviews those requests before the exam starts.

Students can track backlog, revaluation, result correction, bona fide certificate, scholarship and account-support requests. A revaluation or correction decision can update a published result and records the old score, new score, reviewer and reason in the same transaction. Office guidance and contact details are maintained by administrators. Certificates support an online request and offline collection instructions.

Verify a V8 upgrade, draft and published visibility, complete exam rosters, score limits, student ownership, backlog eligibility, correction history, certificate status changes, and office guidance in the running portals before promoting the release.

Local Chrome verification covered a fresh database, draft and published exams, course-scoped faculty schedules, private student results, backlog approval, score corrections and revaluation, certificate collection, and updated office guidance. The PostgreSQL integration suite covers the V8 upgrade and revision checks at result release.

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
