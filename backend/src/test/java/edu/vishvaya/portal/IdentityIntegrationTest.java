package edu.vishvaya.portal;

import edu.vishvaya.portal.api.AccountRequests;
import edu.vishvaya.portal.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "R2_TEST_DATABASE_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IdentityIntegrationTest {
    @LocalServerPort int port;
    @Autowired AccountService accounts;
    @Autowired JdbcTemplate jdbc;
    @Autowired edu.vishvaya.portal.service.CollegeService college;
    @Autowired edu.vishvaya.portal.service.NoticeService notices;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        // Exercise V4 as an upgrade over populated Release 1 data.
        org.flywaydb.core.Flyway.configure()
                .dataSource(System.getenv("R2_TEST_DATABASE_URL"), "postgres",
                        System.getenv("R2_TEST_DATABASE_PASSWORD"))
                .locations("classpath:db/migration").target("3").load().migrate();
        properties.add("spring.datasource.url", () -> System.getenv("R2_TEST_DATABASE_URL"));
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> System.getenv("R2_TEST_DATABASE_PASSWORD"));
        properties.add("spring.cache.type", () -> "none");
        properties.add("PORTAL_BOOTSTRAP_LOGIN", () -> "");
    }

    private final CookieManager cookies = new CookieManager();
    private final HttpClient browser = HttpClient.newBuilder().cookieHandler(cookies).build();

    private HttpResponse<String> call(HttpClient client, String method, String path,
                                      String body, String contentType, String csrf) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.ofString(body));
        if (contentType != null) builder.header("Content-Type", contentType);
        if (csrf != null) builder.header("X-CSRF-TOKEN", csrf);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String token() throws Exception {
        var response = call(browser, "GET", "/api/auth/csrf", "", null, null);
        assertEquals(200, response.statusCode());
        return response.body().split("\"token\":\"")[1].split("\"")[0];
    }

    private int login(String username, String password) throws Exception {
        return call(browser, "POST", "/api/auth/login", "username=" + username + "&password=" + password,
                "application/x-www-form-urlencoded", token()).statusCode();
    }

    @Test
    void identityLifecycleAndAuthorization() throws Exception {
        assertTrue(jdbc.queryForObject("SELECT count(*) FROM campus_announcement", Long.class) > 0);
        String temporary = UUID.randomUUID().toString();
        String permanent = UUID.randomUUID().toString();
        accounts.bootstrap("first.admin", temporary);
        var admin = accounts.list().getFirst();
        accounts.bootstrap("ignored.admin", UUID.randomUUID().toString());
        assertEquals(1, accounts.list().size());
        assertEquals(401, call(browser, "GET", "/api/admin/accounts", "", null, null).statusCode());
        assertEquals(403, call(browser, "POST", "/api/auth/login", "", null, null).statusCode());
        token();
        String anonymousSession = cookies.getCookieStore().getCookies().getFirst().getValue();
        assertEquals(204, login("FIRST.ADMIN", temporary));
        assertNotEquals(anonymousSession, cookies.getCookieStore().getCookies().getFirst().getValue());
        assertEquals(403, call(browser, "GET", "/api/admin/accounts", "", null, null).statusCode());
        assertEquals(200, call(browser, "GET", "/api/auth/me", "", null, null).statusCode());
        assertEquals(200, call(browser, "GET", "/", "", null, null).statusCode());

        String change = "{\"currentPassword\":\"" + temporary + "\",\"newPassword\":\"" + permanent + "\"}";
        assertEquals(403, call(browser, "POST", "/api/auth/password", change, "application/json", null).statusCode());
        assertEquals(204, call(browser, "POST", "/api/auth/password", change, "application/json", token()).statusCode());
        assertEquals(401, call(browser, "GET", "/api/auth/me", "", null, null).statusCode());
        assertEquals(204, login("first.admin", permanent));
        var list = call(browser, "GET", "/api/admin/accounts", "", null, null);
        assertEquals(200, list.statusCode());
        assertFalse(list.body().contains("passwordHash"));
        assertTrue(list.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        assertThrows(ResponseStatusException.class, () -> accounts.permissions(admin.id(),
                new AccountRequests.Permissions("STUDENT", true, false), admin.id()));

        String studentPassword = UUID.randomUUID().toString();
        String create = "{\"login\":\"student.one\",\"displayName\":\"Synthetic student\","
                + "\"role\":\"STUDENT\",\"temporaryPassword\":\"" + studentPassword + "\"}";
        assertEquals(201, call(browser, "POST", "/api/admin/accounts", create, "application/json", token()).statusCode());
        assertEquals(409, call(browser, "POST", "/api/admin/accounts", create, "application/json", token()).statusCode());
        var student = accounts.list().stream().filter(account -> account.role().equals("STUDENT")).findFirst().orElseThrow();
        String actor = jdbc.queryForObject("SELECT created_by FROM portal_account WHERE id = ?",
                String.class, student.id());
        assertEquals(admin.id().toString(), actor);
        collegeAndPublication(admin.id(), student.id());

        // Reset revokes an already authenticated administrator session.
        accounts.reset(admin.id(), temporary, admin.id());
        assertEquals(401, call(browser, "GET", "/api/admin/accounts", "", null, null).statusCode());
        assertEquals(204, login("student.one", studentPassword));
        String studentPermanent = UUID.randomUUID().toString();
        accounts.changePassword(student.id(), 0, studentPassword, studentPermanent);
        assertEquals(401, call(browser, "GET", "/api/auth/me", "", null, null).statusCode());
        assertEquals(204, login("student.one", studentPermanent));
        assertEquals(403, call(browser, "GET", "/api/admin/accounts", "", null, null).statusCode());
        accounts.permissions(student.id(), new AccountRequests.Permissions("STUDENT", false, false), admin.id());
        assertEquals(401, call(browser, "GET", "/api/auth/me", "", null, null).statusCode());

        // PostgreSQL serializes competing changes that would remove all administrators.
        var second = accounts.create(new AccountRequests.Create("second.admin", "Synthetic administrator",
                temporary, "ADMIN"), admin.id());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var outcomes = executor.invokeAll(List.<java.util.concurrent.Callable<Boolean>>of(
                    () -> disable(admin.id(), second.id()), () -> disable(second.id(), admin.id())));
            assertEquals(1, outcomes.stream().filter(result -> {
                try { return result.get(); } catch (Exception exception) { throw new RuntimeException(exception); }
            }).count());
        }
        assertEquals(1L, jdbc.queryForObject(
                "SELECT count(*) FROM portal_account WHERE role = 'ADMIN' AND enabled", Long.class));

        // Logout clears a valid session even while a temporary password is required.
        if (!accounts.get(admin.id()).enabled()) {
            accounts.permissions(admin.id(), new AccountRequests.Permissions("ADMIN", true, false), second.id());
        }
        assertEquals(204, login("first.admin", temporary));
        String csrf = token();
        assertEquals(204, call(browser, "POST", "/api/auth/logout", "", null, csrf).statusCode());
        assertEquals(401, call(browser, "GET", "/api/auth/me", "", null, null).statusCode());

        var publicClient = HttpClient.newHttpClient();
        String enquiry = "{\"fullName\":\"Synthetic visitor\",\"phoneNumber\":\"9999999999\","
                + "\"message\":\"Synthetic test enquiry\",\"privacyConsent\":true}";
        assertEquals(201, call(publicClient, "POST", "/api/public/enquiries", enquiry, "application/json", null).statusCode());
        assertEquals("PUBLIC", jdbc.queryForObject(
                "SELECT created_by FROM admission_enquiry ORDER BY created_at DESC LIMIT 1", String.class));
    }

    private boolean disable(UUID target, UUID actor) {
        try {
            accounts.permissions(target, new AccountRequests.Permissions("ADMIN", false, false), actor);
            return true;
        } catch (ResponseStatusException exception) {
            assertEquals(409, exception.getStatusCode().value());
            return false;
        }
    }

    private void collegeAndPublication(UUID admin, UUID student) throws Exception {
        college.save("programs", java.util.Map.of("code", "btech", "name", "Synthetic BTech",
                "department_code", "cse", "active", true), admin);
        college.save("cohorts", java.util.Map.of("code", "2026", "name", "Synthetic cohort",
                "program_code", "btech", "active", true), admin);
        college.save("students", java.util.Map.of("code", "s1", "name", "Synthetic student",
                "account_id", student.toString(), "cohort_code", "2026", "active", true), admin);
        assertEquals(1, college.student(student).size());
        assertTrue(college.student(admin).isEmpty());
        var teacher = accounts.create(new AccountRequests.Create("faculty.one", "Synthetic faculty",
                UUID.randomUUID().toString(), "FACULTY"), admin);
        college.save("faculty", java.util.Map.of("code", "f1", "name", "Synthetic faculty",
                "account_id", teacher.id().toString(), "department_code", "cse", "active", true), admin);
        college.save("courses", java.util.Map.of("code", "cs101", "name", "Synthetic computing",
                "department_code", "cse", "active", true), admin);
        college.save("terms", java.util.Map.of("code", "t1", "name", "Synthetic term",
                "starts_on", "2026-01-01", "ends_on", "2026-06-01", "active", true), admin);
        college.save("offerings", java.util.Map.of("code", "o1", "name", "Synthetic offering",
                "course_code", "cs101", "cohort_code", "2026", "term_code", "t1", "active", true), admin);
        var assignment = new java.util.HashMap<String, Object>(java.util.Map.of("code", "a1",
                "name", "Synthetic assignment", "faculty_code", "f1", "offering_code", "o1", "active", true));
        college.save("assignments", assignment, admin);
        assertEquals(1, college.faculty(teacher.id()).size());
        assertTrue(college.faculty(student).isEmpty());
        assignment.put("active", false);
        college.save("assignments", assignment, admin);
        assertTrue(college.faculty(teacher.id()).isEmpty());
        UUID notice = UUID.randomUUID();
        var now = java.time.OffsetDateTime.now();
        notices.save(notice, new edu.vishvaya.portal.api.NoticeRequest("DEMO", "Synthetic notice",
                "Synthetic summary", 100, true, now.minusMinutes(1), now.plusMinutes(1)), admin);
        assertTrue(call(browser, "GET", "/api/public/announcements", "", null, null).body().contains(notice.toString()));
        notices.save(notice, new edu.vishvaya.portal.api.NoticeRequest("DEMO", "Scheduled notice",
                "Synthetic summary", 100, true, now.plusDays(1), null), admin);
        assertFalse(call(browser, "GET", "/api/public/announcements", "", null, null).body().contains(notice.toString()));
        notices.save(notice, new edu.vishvaya.portal.api.NoticeRequest("DEMO", "Expired notice",
                "Synthetic summary", 100, true, now.minusDays(2), now.minusDays(1)), admin);
        assertFalse(call(browser, "GET", "/api/public/announcements", "", null, null).body().contains(notice.toString()));
        notices.save(notice, new edu.vishvaya.portal.api.NoticeRequest("DEMO", "Synthetic notice",
                "Synthetic summary", 100, false, now.minusMinutes(1), null), admin);
        assertFalse(call(browser, "GET", "/api/public/announcements", "", null, null).body().contains(notice.toString()));
        assertEquals(200, call(browser, "GET", "/api/admin/setup/programs", "", null, null).statusCode());
    }
}
