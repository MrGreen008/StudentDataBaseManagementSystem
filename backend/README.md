# Release 1 backend

The Spring Boot application serves the public website and its API from the same origin. PostgreSQL stores the public profile, announcements, and admission enquiries. Redis caches public read endpoints for two minutes. Flyway creates and seeds the database schema on first startup.

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

Student records are intentionally out of Release 1 and should be added with the authenticated student/staff portal work in a later release.

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

The public homepage reads announcements, metrics, and department names from the API. Submitting the admission form validates consent and stores the enquiry in PostgreSQL. This release does not expose an API for reading enquiries publicly; inspect them through an authenticated database connection in IntelliJ. Student and staff authentication, notice editing, and enquiry triage belong in a later portal release.

Database rows carry `created_by`, `created_at`, `updated_by`, and `updated_at` audit metadata. Existing seed data is attributed to `SYSTEM`; public admission enquiries are attributed to `PUBLIC`. Once staff and student sign-in is added, write operations must take the actor from Spring Security's authenticated principal, never from a name supplied by the browser. PostgreSQL updates `updated_at` automatically; the application sets `updated_by` when authenticated update flows are introduced.

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
