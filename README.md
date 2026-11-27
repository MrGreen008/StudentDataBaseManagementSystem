# Vishvaya Institute of Technology — Student System

This repository is developed on `dev`. `qa` and `prod` are promotion branches: review and test a release on QA before promoting it to production. Do not develop directly on `qa` or `prod`.

## Release 1

Release 1 serves the public college website from Spring Boot, loads announcements and college metrics from PostgreSQL, reads the BTech department catalog, and saves admission enquiries in PostgreSQL. Redis caches the public read APIs. Flyway creates and seeds the schema automatically on first application startup. Swagger UI documents the REST APIs.

See [the backend setup guide](backend/README.md) for IntelliJ steps, API URLs, and the optional Docker-based app preview. The public site source is in [frontend/dist](frontend/dist/).

## Release 2

Sign-in, account management, college setup, student and faculty dashboards, and staff announcement management are available at `/portal.html`. The backend setup guide documents first-administrator provisioning and API usage. The roadmap records local release verification.

## Local services

- PostgreSQL and Redis run in Docker.
- Spring Boot runs from IntelliJ during development and connects to those local containers.
- The root `.env` file contains local-only passwords; it is ignored by Git. Start from `.env.example` if setting up another machine.
- The all-in-Docker preview is optional and uses the Compose `demo` profile.

## Data notice

The college metrics, credential examples, announcements, and student stories are project demo content. Verify institutional facts and obtain permission for attributed stories before publishing. Never use real student records in a public demo.
