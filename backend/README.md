# Vishvaya portal backend

## Release 4 exams and student services

Restart Spring Boot to apply Flyway V9. In **Exam schedules**, administrators create a regular or backlog exam for a course offering and configure the maximum and passing scores. Use Draft while setting up the exam, then Scheduled to publish its schedule. Regular exam rosters come from enrollment on the exam date. Faculty can view schedules for their assigned offerings.

After entering every eligible student's score, use **Publish results** once the exam ends. Students see only their own published scores, passing thresholds, and outcomes. An eligible student with an outstanding failed result can request an upcoming backlog exam for the same offering. Approved registrations form the backlog roster.

Use **Student requests** to review backlog registrations, result corrections, revaluations, certificate requests, and scholarship or account-support questions. A result correction requires a new score. Revaluation can confirm the existing score or record a change. The request preserves the decision and the previous score. Use the office response for the next step, including certificate pickup instructions. Update the office descriptions, instructions, or verified contact email in **Office guidance**. Students see the same information in **Help & offices**.

Exam and service requests use the existing role-scoped API prefixes. The student endpoints are `/api/student/exams`, `/api/student/results`, `/api/student/backlog-exams`, and `/api/student/services/**`. Administrators manage exams at `/api/admin/exams/**` and the request queue and office guidance at `/api/admin/student-services/**`. The faculty schedule is at `/api/faculty/exams`. Edits use CSRF and record revisions.

## Release 3 academic operations

Restart Spring Boot to apply Flyway V8. To try the academic workflows, create a program, cohort, term, course and offering in **College setup**. Link synthetic student and faculty accounts to profiles and assign the faculty member to the offering. In **Enrollments**, assign students from that cohort to the offering and set the effective date range. In **Timetable**, schedule a class using the teaching assignment, campus time (IST), and a room. The timetable flags unavailable faculty so an administrator can edit or cancel the affected class.

- Faculty open an assigned class in **Timetable**, select a mark for everyone on the roster, and submit after the start time. Students can then see their own marks in **Attendance**.
- Students file discrepancies from their attendance history. The instructor or an administrator can accept with a corrected mark or reject with a reason. A submitted register retains its original marks and remains attached to the scheduled class.
- **Faculty availability** supports planned leave and a day-of status in campus time. A day-of update takes precedence over planned leave. Timetables display the status and update time. Staff notes stay within the administrator and owning faculty member's views.
- In **Portal notices**, choose Timetable, Exam dates, or Holiday to publish to **Academic bulletins** using the existing audience, approval, and document permissions.

The new APIs live under `/api/admin/academic`, `/api/faculty/academic`, and `/api/student/academic`. Timetable reads take `week=YYYY-MM-DD` as the first campus date of a seven-day window. Schedule writes accept timestamps with offsets. Writes use the portal CSRF token and carry the last-read revision on edits. A 409 means a scheduling/business conflict or an outdated record. Reload after an outdated-record response.

## Demo registration and Gmail email

Students and faculty can register from their portal sign-in page. Registration asks for a preferred username, full name, ten-digit mobile number, email and a session-bound CAPTCHA. Username and email are unique, and the normalized full-name/mobile pair must also be unique. The email contains a generated temporary password. The account must change it before accessing the dashboard. Administrators are created by another administrator or first-start bootstrap. All administrators have the same permissions.

Configure these settings in the ignored root `.env` for Gmail SMTP, then restart Spring Boot.

```dotenv
PORTAL_SMTP_HOST=smtp.gmail.com
PORTAL_SMTP_PORT=587
PORTAL_SMTP_USERNAME=your-sender@gmail.com
PORTAL_SMTP_PASSWORD=your-google-app-password
PORTAL_MAIL_FROM=your-sender@gmail.com
PORTAL_SMTP_TLS=true
```

Create an app password on the sending Google account with 2-Step Verification enabled. Workspace policies can affect app-password availability. Keep it in the local environment file. [Google app-password guidance](https://support.google.com/mail/answer/185833)

Registration fails with a clear email-service error if mail is unavailable, and account creation rolls back when delivery fails. Login accepts username or email and enforces the selected portal on the backend. Use Change password in the account menu to set a recovery email for existing accounts, with current-password confirmation. Forgot password sends a six-digit, ten-minute, single-use code, with a one-minute resend cooldown and five verification attempts. A successful reset invalidates existing sessions. Unknown accounts receive the same request response. Passwords and reset codes are stored as one-way hashes and omitted from logs.

CAPTCHA refresh has a separate allowance of thirty requests per IP per minute. Sign-in, registration and password-recovery routes each have their own ten-request allowance. Rate-limit responses include the remaining wait in `Retry-After`. The refresh control shows a countdown and prevents overlapping requests. CAPTCHA refresh failures leave the original form error visible.

For SMTP failures, the UI distinguishes mailbox authentication, network connection and TLS failures. Logs contain a safe error category without provider responses, addresses or credentials. If Gmail rejects authentication, check the sending address and the Google-generated app password for that same account. After correcting the settings, restart the application and retry registration. Registration sends temporary credentials, while Forgot password sends a code only when an eligible account and recovery email exist.

## Portal notices and documents

Overview refreshes when opened. Administrators see all portal notices, including drafts and pending requests. Faculty see approved, currently published student and faculty notices. Students see only published student notices matching any cohort or individual restriction.

Faculty may save notices for assigned cohorts and submit them for administrator approval. Administrators approve or reject each submission in Portal notices. Any faculty content change requires a new approval. Public website announcements are administered by administrators. Permission to read a notice also controls document downloads.

An optional PDF of up to 5 MB can be attached to a portal notice. Files are stored in PostgreSQL and downloaded as attachments through an authenticated endpoint. Uploads check the PDF signature, size and filename.

Both themes are available through the sun/moon control and the selection persists in the browser. Form errors appear beside the form, clear when edited, and the page-level message expires after eight seconds. Tables refresh on revisiting management sections.

## Release 2 identity

Release 2 adds session-based sign-in at `/portal.html`, administrator account and college setup forms, faculty assignment and student profile dashboards, and public announcement publishing for authorized staff.

Administrators maintain departments, programs, cohorts, terms, courses, honors/minors, offerings, student and faculty profiles, and assignments. Searchable tables show existing records. Linked-record dropdowns select departments, programs, cohorts, courses, terms, offerings, faculty and matching-role accounts. Save an existing code to update it. Retire records with the active flag and hide departments with the visible flag. The backend preserves referenced records and attributes writes to the authenticated account.

The login page accepts a `role` query parameter of `STUDENT`, `FACULTY` or `ADMIN` to choose the photograph and sign-in presentation. The chosen portal must match the authenticated account role. Photography reuses the generated demo assets under `frontend/dist/images`. Account settings hold password changes, while Overview shows role-specific notices and academic information. Student service panels identify the planned releases for results and academic operations.

Portal notices are separate from public announcements. `GET /api/portal/notices` returns published, currently valid notices matching the signed-in role and any cohort or individual restrictions. Authorized publishers use `GET /api/staff/portal-notices` and `PUT /api/staff/portal-notices/{id}`. The write body contains title, summary, audience, optional cohortCode and accountId, published, publishedAt and optional expiresAt. Administrators can target any role, cohort or matching-role account. Faculty can submit their own student notices for assigned cohorts for administrator approval. The backend verifies assignment scope on each write. Templates create unpublished draft examples that require verified details before publication. Date pickers use the browser's local timezone and submit timestamps with an offset.

`GET /api/admin/setup` returns resource field definitions. `GET /api/admin/setup/{resource}` lists records, and `PUT` to the same path saves a complete record. Student and faculty dashboards use `/api/student/dashboard` and `/api/faculty/dashboard` and derive scope from the session. `GET /api/staff/announcements` lists announcements, and `PUT /api/staff/announcements/{uuid}` publishes or updates a notice using category, title, summary, priority, published, publishedAt and optional expiresAt. Timestamps include a timezone. Announcement and department reads query PostgreSQL directly so scheduling and visibility changes take effect on subsequent requests. College overview metrics retain Redis caching.

For the first administrator, set `PORTAL_BOOTSTRAP_LOGIN` and `PORTAL_BOOTSTRAP_PASSWORD` in the ignored root `.env`, then start the application. Use a password with at least 12 characters and at most 72 UTF-8 bytes. Sign in and change the temporary password. Remove the bootstrap settings after provisioning. A restart with bootstrap settings preserves existing accounts and credentials. The Docker preview also reads the bootstrap settings from the root `.env`.

Sessions expire after 30 idle minutes. Password changes require a fresh sign-in. Resets, permission changes and deactivation invalidate existing sessions on their next request. QA and production use Secure session cookies and require HTTPS. Sign-in accepts ten attempts per client IP per minute per application instance. The limiter uses the direct connection address, so proxy deployments must configure a trusted client-address strategy before sharing this limit across users behind a proxy.

| Endpoint | Behavior |
| --- | --- |
| `GET /api/auth/csrf` | Returns a session CSRF token and its header name |
| `POST /api/auth/login` | Accepts URL-encoded `username` and `password`, returns 204 or generic 401 |
| `GET /api/auth/me` | Returns the signed-in account without credentials |
| `POST /api/auth/password` | Accepts `currentPassword` and `newPassword`, invalidates the session |
| `POST /api/auth/logout` | Invalidates the session |
| `GET /api/admin/accounts` | Lists accounts for administrators |
| `POST /api/admin/accounts` | Creates an account from `login`, `displayName`, `role` and `temporaryPassword` |
| `PUT /api/admin/accounts/{id}/permissions` | Sets `role`, `enabled` and `canPublish`, preserves an active administrator |
| `POST /api/admin/accounts/{id}/reset-password` | Accepts `temporaryPassword`, revokes sessions and requires a password change |

Fetch a fresh CSRF token after sign-in and send it with every authenticated mutation, login and logout. Account roles are `ADMIN`, `FACULTY` and `STUDENT`. Temporary-password accounts can inspect their own identity, change their password and log out. Swagger UI and OpenAPI now require an administrator session.

Run `mvn test` with JDK 21. The HTTP/PostgreSQL integration test runs when `R2_TEST_DATABASE_URL` and `R2_TEST_DATABASE_PASSWORD` point to a fresh, disposable PostgreSQL database owned by `postgres`. It migrates through V3 and V7, seeds a portal notice, applies V8, then starts the application to apply V9. It checks identity and notice behavior, concurrent timetable writes, and exam publication. Use a new disposable database on each run.

The Spring Boot application serves the public website and its API from the same origin. PostgreSQL stores the public profile, announcements, and admission enquiries. Redis caches college overview metrics for two minutes. Flyway creates and seeds the database schema on first startup.

## Open in IntelliJ IDEA

1. Open the repository in IntelliJ IDEA and choose **Load Maven Changes** when prompted for `backend/pom.xml`.
2. Set the project SDK to JDK 21. This computer has a JDK at `C:\Program Files\Java\jdk-21`.
3. Keep the run configuration working directory at the repository root so Spring Boot can load the ignored root `.env` file.
4. Start the existing PostgreSQL and Redis containers with `docker compose up -d` from the repository root.
5. Run `edu.vishvaya.portal.VishvayaPortalApplication` from the editor.

The root `.env` file must contain the same database and Redis passwords used by Compose. Do not commit `.env`.

The shared IntelliJ run configuration is a standard Java Application configuration, so it does not require the Spring Boot IDE plugin. It sets `SPRING_PROFILES_ACTIVE=dev`. The `qa` and `prod` profiles require their own database and Redis environment variables; do not use those profiles locally without the matching environment secrets.

## Backend structure and coding conventions

- `api` contains HTTP controllers, request/response DTOs, and API error mapping. Keep database access out of controllers.
- `service` contains service contracts; `service.impl` contains application logic and transaction boundaries.
- `persistence` contains PostgreSQL queries and row mapping. Release 1 uses `JdbcTemplate`, so it does not need JPA entity classes; add domain entities only when a persistence model benefits from them.
- Keep each Java source file below 500 lines. Split cohesive responsibilities into focused types before a file approaches that limit.
- Validate input at the API boundary, keep error responses safe for clients, and log failures server-side. Never log enquiry names, phone numbers, email addresses, passwords, tokens, or other personal data.
- Use constructor injection, descriptive names, small methods, and explicit transactions for write workflows.

The student dashboard resolves the profile from the authenticated account ID.

## Publishing and deployment

GitHub stores the source; it does not run this Spring Boot application. For a portfolio demo, a public repository is appropriate once its contents have been checked for secrets and personal data. A live demo needs a hosting service for the Spring Boot app and persistent PostgreSQL storage. Redis can be added as a hosted cache; the demo can also run without relying on cached data if the application is configured for that. Docker Compose is useful for local development and simple demos, but production data should use persistent storage and backups.

Do not publish the local `.env` file or use real student records on a public demo. Keep credentials in the host's secret/environment configuration. Choose a host and domain only when you are ready to make the demo public; GitHub Pages alone cannot run this backend or database.

## Local endpoints

- Website: `http://localhost:8080/`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`
- Announcements: `GET /api/public/announcements`
- College metrics: `GET /api/public/overview`
- Departments: `GET /api/public/departments`
- Admission form: `POST /api/public/enquiries`

The public homepage reads announcements, metrics, and department names from the API. Submitting the admission form validates consent and stores the enquiry in PostgreSQL. This release does not expose an API for reading enquiries publicly; inspect them through an authenticated database connection in IntelliJ.

Database rows carry `created_by`, `created_at`, `updated_by`, and `updated_at` audit metadata. Existing seed data is attributed to `SYSTEM`; public admission enquiries are attributed to `PUBLIC`. Authenticated writes take the actor ID from the Spring Security principal. PostgreSQL updates `updated_at` automatically; the application sets `updated_by` on authenticated updates.

After connecting IntelliJ's Database tool to the local PostgreSQL container, enquiries can be reviewed with:

```sql
SELECT id, full_name, phone_number, email, programme_of_interest, status,
       created_by,
       TO_CHAR(created_at AT TIME ZONE 'Asia/Kolkata', 'DD Mon YYYY, HH12:MI:SS AM') || ' IST' AS created_at_display,
       updated_by,
       TO_CHAR(updated_at AT TIME ZONE 'Asia/Kolkata', 'DD Mon YYYY, HH12:MI:SS AM') || ' IST' AS updated_at_display
FROM admission_enquiry
ORDER BY created_at DESC;
```

## Optional all-in-Docker preview

From the repository root, run `docker compose --profile demo up --build -d`. This builds and starts the web application alongside PostgreSQL and Redis. The default Compose profile remains database-only for IntelliJ development.
