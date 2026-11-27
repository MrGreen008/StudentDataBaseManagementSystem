# Vishvaya portal backend

## Release 2 identity

Release 2 adds session-based sign-in at `/portal.html`, administrator account and college setup forms, faculty assignment and student profile dashboards, and public announcement publishing for authorized staff.

Administrators maintain departments, programs, cohorts, terms, courses, honors/minors, offerings, student and faculty profiles, and assignments. The college form shows the required fields for each resource. Use existing codes for references and the account UUID from the Accounts list when linking profiles. Save an existing code to update it. Retire records with the active flag and hide departments with the visible flag. The backend preserves referenced records and attributes writes to the authenticated account.

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

Run `mvn test` with JDK 21 for security unit checks. The HTTP/PostgreSQL integration test runs when `R2_TEST_DATABASE_URL` and `R2_TEST_DATABASE_PASSWORD` point to a fresh, disposable PostgreSQL database owned by `postgres`. It creates and updates synthetic accounts and admission enquiries. The test first applies V1–V3, then starts the application to apply V4 and V5 and exercise identity, college setup and publication workflows. Use a new disposable database on each run.

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
