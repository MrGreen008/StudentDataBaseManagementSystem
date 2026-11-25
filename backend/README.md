# Release 1 backend

The Spring Boot application serves the public website and its API from the same origin. PostgreSQL stores the public profile, announcements, and admission enquiries. Redis caches public read endpoints for two minutes. Flyway creates and seeds the database schema on first startup.

## Open in IntelliJ IDEA

1. Open the repository in IntelliJ IDEA and choose **Load Maven Changes** when prompted for `backend/pom.xml`.
2. Set the project SDK to JDK 21. This computer has a JDK at `C:\Program Files\Java\jdk-21`.
3. Keep the run configuration working directory at the repository root so Spring Boot can load the ignored root `.env` file.
4. Start the existing PostgreSQL and Redis containers with `docker compose up -d` from the repository root.
5. Run `edu.vishvaya.portal.VishvayaPortalApplication` from the editor.

The root `.env` file must contain the same database and Redis passwords used by Compose. Do not commit `.env`.

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

After connecting IntelliJ's Database tool to the local PostgreSQL container, enquiries can be reviewed with:

```sql
SELECT id, full_name, phone_number, email, programme_of_interest, status, created_at
FROM admission_enquiry
ORDER BY created_at DESC;
```

## Optional all-in-Docker preview

From the repository root, run `docker compose --profile demo up --build -d`. This builds and starts the web application alongside PostgreSQL and Redis. The default Compose profile remains database-only for IntelliJ development.
