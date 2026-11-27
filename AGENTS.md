# Repository instructions

This repository is the Vishvaya Institute of Technology student system, a single-college BTech demo. Use [ROADMAP.md](ROADMAP.md) for release scope and the [root](README.md), [backend](backend/README.md), and [frontend](frontend/README.md) guides for existing conventions and setup. Inspect the relevant code and current Git status before editing.

## Development workflow

- Use `dev` as the development base. Reserve `qa` and `prod` for release promotion, with QA verification before production. Preserve unrelated changes in the working tree.
- Agree on a release's design and acceptance criteria before implementing it. Keep changes within the requested release and preserve existing public-site and API behavior unless the release explicitly changes it.
- Use JDK 21 and Maven for the Spring Boot backend. PostgreSQL and Redis run through Docker Compose for local development, with Spring Boot normally running from IntelliJ under the `dev` profile. Use `frontend/dist/` as the source for the current plain HTML, CSS, and JavaScript site served by Spring Boot.
- Follow the existing `api`, `service`, `service.impl`, and `persistence` boundaries. Validate request input at the API boundary, use constructor injection, keep controllers out of persistence, and use explicit transactions for write workflows. Keep Java files under 500 lines.
- Append a new Flyway migration for database changes. Preserve applied migrations and verify upgrades from existing data. Use PostgreSQL for durable records and Redis for caching. Check cache invalidation when a write changes public content.
- Run the relevant Maven checks from `backend/` and verify changed workflows through their normal entry points. Record any checks that could not run. Update setup or API documentation when the change affects it. Mark tracked work done after acceptance checks and review in the running application.

## Identity, authorization, and data

- Enforce permissions in the backend. Check role and record scope for both reads and writes. Administrators manage college records, faculty access assigned classes and courses, students access their own records, and public reads expose published information only. Keep the public admission-enquiry submission available.
- Derive the actor for authenticated writes from the Spring Security principal. Preserve the `created_by`, `created_at`, `updated_by`, and `updated_at` conventions, including the `SYSTEM` and `PUBLIC` actors where appropriate.
- Keep credentials in the ignored local `.env` or the deployment environment. Use `.env.example` for placeholders. Keep passwords, tokens, and personal data out of source, logs, and error responses. Use synthetic students and accounts in demo and test data.
- Verify allowed and denied operations for each affected role, including access to another student's record or an unassigned course where applicable. For identity changes, also review sign-in, session lifecycle, account changes, and the public routes that must stay accessible.
- For schema and authorization changes, run the HTTP integration test against a fresh disposable PostgreSQL database as described in the backend guide. Check the Maven report for skipped tests. For portal changes, exercise each affected role in a separate browser session and check the public page after publishing or withdrawing content.
