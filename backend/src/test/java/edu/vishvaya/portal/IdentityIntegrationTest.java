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
    @Autowired edu.vishvaya.portal.service.PortalNoticeService portalNotices;
    @Autowired edu.vishvaya.portal.service.PortalDocumentService documents;
    @Autowired edu.vishvaya.portal.service.RegistrationService registration;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    edu.vishvaya.portal.service.PortalMailService mail;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) throws java.sql.SQLException {
        // Exercise each upgrade over the data already present at that release.
        org.flywaydb.core.Flyway.configure()
                .dataSource(System.getenv("R2_TEST_DATABASE_URL"), "postgres",
                        System.getenv("R2_TEST_DATABASE_PASSWORD"))
                .locations("classpath:db/migration").target("3").load().migrate();
        org.flywaydb.core.Flyway.configure()
                .dataSource(System.getenv("R2_TEST_DATABASE_URL"), "postgres",
                        System.getenv("R2_TEST_DATABASE_PASSWORD"))
                .locations("classpath:db/migration").target("7").load().migrate();
        try (var connection = java.sql.DriverManager.getConnection(System.getenv("R2_TEST_DATABASE_URL"),
                "postgres", System.getenv("R2_TEST_DATABASE_PASSWORD"));
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO portal_notice (id,title,summary,audience,published_at,created_by,updated_by)
                    VALUES ('aa9790db-0e5d-4d1f-8487-ff35fb33ad57','Existing notice','Pre-upgrade draft',
                        'ADMIN',CURRENT_TIMESTAMP,'SYSTEM','SYSTEM')
                    """);
        }
        org.flywaydb.core.Flyway.configure()
                .dataSource(System.getenv("R2_TEST_DATABASE_URL"), "postgres",
                        System.getenv("R2_TEST_DATABASE_PASSWORD"))
                .locations("classpath:db/migration").target("8").load().migrate();
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
        return call(browser, "POST", "/api/auth/login", "username=" + username + "&password=" + password
                + "&portalRole=" + (username.toLowerCase().startsWith("student")?"STUDENT":"ADMIN"),
                "application/x-www-form-urlencoded", token()).statusCode();
    }

    @Test
    void identityLifecycleAndAuthorization() throws Exception {
        assertTrue(jdbc.queryForObject("SELECT count(*) FROM campus_announcement", Long.class) > 0);
        assertEquals("GENERAL", jdbc.queryForObject("""
                SELECT topic FROM portal_notice WHERE id='aa9790db-0e5d-4d1f-8487-ff35fb33ad57'
                """, String.class));
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
        var results = call(browser, "GET", "/api/student/results", "", null, null);
        assertEquals(200, results.statusCode());
        assertTrue(results.body().contains("\"outcome\":\"PASS\""));
        assertTrue(results.body().contains("Synthetic exam"));
        assertEquals(403, call(browser, "GET", "/api/admin/exams", "", null, null).statusCode());
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
        registrationAndRecovery();
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
        academicScheduling(student, teacher.id());
        examPublication(student, teacher.id());
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
        UUID internal = UUID.randomUUID();
        var target = new edu.vishvaya.portal.api.PortalNoticeRequest("Synthetic semester form",
                "Synthetic notice for cohort 2026", "STUDENT", "2026", student,
                true, now.minusMinutes(1), now.plusDays(1));
        portalNotices.save(internal, target, admin);
        assertTrue(portalNotices.inbox(student).stream().anyMatch(row -> row.get("id").equals(internal)));
        assertFalse(portalNotices.inbox(teacher.id()).isEmpty());
        assertFalse(portalNotices.inbox(admin).isEmpty());
        assertFalse(call(browser, "GET", "/api/public/announcements", "", null, null).body().contains(internal.toString()));
        accounts.permissions(teacher.id(), new AccountRequests.Permissions("FACULTY", true, true), admin);
        assertThrows(ResponseStatusException.class, () -> portalNotices.save(internal, target, teacher.id()));
        portalNotices.save(internal, new edu.vishvaya.portal.api.PortalNoticeRequest("Unpublished",
                "Synthetic withdrawn notice", "STUDENT", "2026", student,
                false, now.minusMinutes(1), null), admin);
        assertTrue(portalNotices.inbox(student).isEmpty());
        assignment.put("active", true);
        college.save("assignments", assignment, admin);
        UUID facultyNotice = UUID.randomUUID();
        var cohortNotice = new edu.vishvaya.portal.api.PortalNoticeRequest("Synthetic faculty notice",
                "Students in the assigned cohort only", "STUDENT", "2026", null,
                true, now.minusMinutes(1), now.plusDays(1));
        portalNotices.save(facultyNotice, cohortNotice, teacher.id());
        byte[] pdf="%PDF-1.4\nSynthetic test document".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        documents.upload(facultyNotice,new org.springframework.mock.web.MockMultipartFile(
                "file","official.pdf","application/pdf",pdf),teacher.id());
        portalNotices.save(facultyNotice, cohortNotice, teacher.id());
        assertFalse(portalNotices.inbox(student).stream().anyMatch(row -> row.get("id").equals(facultyNotice)));
        long revision=((Number)portalNotices.managed(admin).stream().filter(row->row.get("id").equals(facultyNotice))
                .findFirst().orElseThrow().get("revision")).longValue();
        documents.approve(facultyNotice,revision,true,admin);
        assertArrayEquals(pdf,documents.download(facultyNotice,student).bytes());
        assertThrows(ResponseStatusException.class,()->documents.approve(facultyNotice,revision,true,admin));
        assertTrue(portalNotices.inbox(student).stream().anyMatch(row -> row.get("id").equals(facultyNotice)));
        assignment.put("active", false);
        college.save("assignments", assignment, admin);
        assertThrows(ResponseStatusException.class, () -> portalNotices.save(facultyNotice, cohortNotice, teacher.id()));
    }

    private void academicScheduling(UUID student, UUID teacher) throws Exception {
        String csrf = token();
        String academic = "/api/admin/academic";
        String dates = """
                {"startsOn":"2026-01-01","endsOn":"2026-06-01"}
                """;
        assertTrue(call(browser, "GET", "/api/admin/setup/terms", "", null, null)
                .body().contains("\"starts_on\":\"2026-01-01\""));
        assertEquals(204, call(browser, "PUT", academic + "/enrollments/o1/" + student,
                dates, "application/json", csrf).statusCode());
        String lesson = """
                {"offeringCode":"o1","instructorId":"%s","room":"Lab A",
                    "startsAt":"2026-02-02T10:00:00+05:30","endsAt":"2026-02-02T11:00:00+05:30"}
                """.formatted(teacher);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> schedule = () -> call(browser, "PUT",
                    academic + "/sessions/" + UUID.randomUUID(), lesson, "application/json", csrf).statusCode();
            var outcomes = executor.invokeAll(List.of(schedule, schedule));
            assertEquals(List.of(204, 409), java.util.stream.Stream.of(outcomes.get(0).get(), outcomes.get(1).get())
                    .sorted().toList());
        }
        var timetable = call(browser, "GET", academic + "/timetable?week=2026-02-02", "", null, null);
        assertEquals(200, timetable.statusCode());
        assertTrue(timetable.body().contains("Lab A"));
    }

    private void examPublication(UUID student, UUID teacher) throws Exception {
        UUID id = UUID.randomUUID();
        String exam = "/api/admin/exams/" + id;
        String csrf = token();
        String schedule = """
                {"offeringCode":"o1","title":"Synthetic exam","kind":"REGULAR","room":"Lab A",
                    "startsAt":"2026-02-02T10:00:00+05:30","endsAt":"2026-02-02T11:00:00+05:30",
                    "maxScore":100,"passScore":40,"state":"SCHEDULED"}
                """;
        assertEquals(204, call(browser, "PUT", exam, schedule, "application/json", csrf).statusCode());
        assertEquals(409, call(browser, "POST", exam + "/release", "{\"revision\":0}",
                "application/json", csrf).statusCode());
        String score = "{\"examRevision\":0,\"score\":75.25}";
        assertEquals(404, call(browser, "PUT", exam + "/scores/" + teacher, score,
                "application/json", csrf).statusCode());
        assertEquals(204, call(browser, "PUT", exam + "/scores/" + student, score,
                "application/json", csrf).statusCode());
        assertEquals(409, call(browser, "POST", exam + "/release", "{\"revision\":0}",
                "application/json", csrf).statusCode());
        assertEquals(204, call(browser, "POST", exam + "/release", "{\"revision\":1}",
                "application/json", csrf).statusCode());
        assertEquals(204, call(browser, "POST", exam + "/release", "{\"revision\":1}",
                "application/json", csrf).statusCode());
    }

    private void registrationAndRecovery() throws Exception {
        registration.register(new edu.vishvaya.portal.api.RegistrationRequest("demo.user","Demo visitor",
                "9876543210","demo@example.test","STUDENT","unused"));
        assertThrows(ResponseStatusException.class,()->registration.register(
                new edu.vishvaya.portal.api.RegistrationRequest("bad.admin","Synthetic admin",
                "9876543219","admin@example.test","ADMIN","unused")));
        UUID registeredId=jdbc.queryForObject("SELECT id FROM portal_account WHERE login='demo.user'",UUID.class);
        assertEquals("demo@example.test",registration.profile(registeredId).get("email"));
        assertThrows(ResponseStatusException.class,()->registration.register(
                new edu.vishvaya.portal.api.RegistrationRequest("different.user","  DEMO   visitor ",
                "9876543210","other@example.test","FACULTY","unused")));
        var message=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(mail).send(org.mockito.ArgumentMatchers.eq("demo@example.test"),
                org.mockito.ArgumentMatchers.eq("Vishvaya demo registration"),message.capture());
        String temporary=message.getValue().split("Temporary password: ")[1].split("\\s")[0];
        assertEquals(401,call(browser,"POST","/api/auth/login",
                "username=demo.user&password="+temporary+"&portalRole=FACULTY",
                "application/x-www-form-urlencoded",token()).statusCode());
        assertEquals(204,call(browser,"POST","/api/auth/login",
                "username=demo%40example.test&password="+temporary+"&portalRole=STUDENT",
                "application/x-www-form-urlencoded",token()).statusCode());
        registration.recover("demo.user","STUDENT");
        var codeMessage=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(mail).send(org.mockito.ArgumentMatchers.eq("demo@example.test"),
                org.mockito.ArgumentMatchers.eq("Vishvaya password reset code"),codeMessage.capture());
        String code=codeMessage.getValue().split("code is ")[1].substring(0,6);
        assertFalse(registration.reset("demo.user","STUDENT",code.equals("000000")?"111111":"000000",temporary+"X"));
        assertTrue(registration.reset("demo.user","STUDENT",code,temporary+"X"));
        assertFalse(registration.reset("demo.user","STUDENT",code,temporary+"Y"));
        assertEquals(401,call(browser,"GET","/api/auth/me","",null,null).statusCode());
        assertEquals(204,call(browser,"POST","/api/auth/login",
                "username=demo.user&password="+temporary+"X&portalRole=STUDENT",
                "application/x-www-form-urlencoded",token()).statusCode());
        var me=call(browser,"GET","/api/auth/me","",null,null);
        assertTrue(me.body().contains("\"passwordChangeRequired\":false"));
    }
}
