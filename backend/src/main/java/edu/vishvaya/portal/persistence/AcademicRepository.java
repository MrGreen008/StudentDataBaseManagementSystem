package edu.vishvaya.portal.persistence;

import edu.vishvaya.portal.api.AcademicRequests.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AcademicRepository {
    static final String TEACHES = """
            EXISTS (SELECT 1 FROM faculty_profile f
            JOIN faculty_assignment a ON a.faculty_code=f.code
            JOIN course_offering o ON o.code=a.offering_code
            WHERE f.account_id=s.instructor_id AND a.offering_code=s.offering_code
                AND f.active AND a.active AND o.active)
            """;
    private static final String CLASS_DATE = "(s.starts_at AT TIME ZONE 'Asia/Kolkata')::date";
    private static final String ENROLLED = """
            EXISTS (SELECT 1 FROM academic_enrollment e JOIN student_profile p ON p.account_id=e.student_id
                WHERE e.student_id=? AND p.active AND e.offering_code=s.offering_code
                AND %s BETWEEN e.starts_on AND e.ends_on)
            """.formatted(CLASS_DATE);

    private final JdbcTemplate jdbc;

    public AcademicRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> enrollments(UUID student) {
        return jdbc.queryForList("""
                SELECT e.offering_code, o.name AS course, o.term_code, p.name AS student,
                    e.student_id, e.starts_on::text, e.ends_on::text, e.revision
                FROM academic_enrollment e JOIN course_offering o ON o.code=e.offering_code
                LEFT JOIN student_profile p ON p.account_id=e.student_id
                WHERE e.student_id=COALESCE(?,e.student_id)
                ORDER BY e.starts_on DESC, o.name, p.name
                """, student);
    }

    public List<Map<String, Object>> teachingChoices() {
        return jdbc.queryForList("""
                SELECT f.account_id AS instructor_id, f.name AS instructor, o.code AS offering_code, o.name AS course
                FROM faculty_assignment a JOIN faculty_profile f ON f.code=a.faculty_code
                JOIN portal_account p ON p.id=f.account_id JOIN course_offering o ON o.code=a.offering_code
                JOIN academic_term t ON t.code=o.term_code
                WHERE f.active AND a.active AND o.active AND t.active AND p.enabled AND p.role='FACULTY'
                ORDER BY o.name,f.name
                """);
    }

    public boolean eligibleEnrollment(String offering, UUID student, Enrollment input) {
        return jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM course_offering o JOIN academic_term t ON t.code=o.term_code
                    JOIN student_profile p ON p.cohort_code=o.cohort_code
                    JOIN portal_account a ON a.id=p.account_id
                    WHERE o.code=? AND p.account_id=? AND p.active AND o.active AND t.active
                        AND a.enabled AND a.role='STUDENT' AND ? >= t.starts_on AND ? <= t.ends_on)
                """, Boolean.class, offering, student, input.startsOn(), input.endsOn());
    }

    public boolean saveEnrollment(String offering, UUID student, Enrollment input, String actor) {
        if (input.revision() == null) {
            return jdbc.update("""
                    INSERT INTO academic_enrollment (offering_code,student_id,starts_on,ends_on,created_by,updated_by)
                    VALUES (?,?,?,?,?,?) ON CONFLICT DO NOTHING
                    """, offering, student, input.startsOn(), input.endsOn(), actor, actor) == 1;
        }
        return jdbc.update("""
                UPDATE academic_enrollment SET starts_on=?,ends_on=?,updated_by=?,revision=revision+1
                WHERE offering_code=? AND student_id=? AND revision=?
                """, input.startsOn(), input.endsOn(), actor, offering, student, input.revision()) == 1;
    }

    public Optional<String> schedulingCohort(Session input, LocalDate firstDate, LocalDate lastDate) {
        return jdbc.queryForList("""
                SELECT o.cohort_code FROM course_offering o JOIN academic_term t ON t.code=o.term_code
                JOIN faculty_assignment a ON a.offering_code=o.code JOIN faculty_profile f ON f.code=a.faculty_code
                JOIN portal_account p ON p.id=f.account_id
                WHERE o.code=? AND f.account_id=? AND a.active AND f.active AND o.active AND t.active
                    AND p.role='FACULTY' AND p.enabled AND ? >= t.starts_on AND ? <= t.ends_on
                """, String.class, input.offeringCode(), input.instructorId(), firstDate, lastDate).stream().findFirst();
    }

    public boolean overlaps(UUID id, Session input, String cohort) {
        return jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM academic_session
                    WHERE id<>? AND state<>'CANCELLED' AND starts_at<? AND ends_at>?
                        AND (lower(room)=lower(?) OR instructor_id=? OR cohort_code=?))
                """, Boolean.class, id, input.endsAt(), input.startsAt(), input.room().strip(), input.instructorId(), cohort);
    }

    public Optional<Map<String, Object>> session(UUID id) {
        return jdbc.queryForList("SELECT * FROM academic_session WHERE id=? FOR UPDATE", id).stream().findFirst();
    }

    public boolean saveSession(UUID id, Session input, String cohort, String actor) {
        if (input.revision() == null) {
            return jdbc.update("""
                    INSERT INTO academic_session
                        (id,offering_code,instructor_id,cohort_code,room,starts_at,ends_at,created_by,updated_by)
                    VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING
                    """, id, input.offeringCode(), input.instructorId(), cohort, input.room().strip(),
                    input.startsAt(), input.endsAt(), actor, actor) == 1;
        }
        return jdbc.update("""
                UPDATE academic_session SET offering_code=?,instructor_id=?,cohort_code=?,room=?,starts_at=?,ends_at=?,
                    state='SCHEDULED',revision=revision+1,updated_by=?
                WHERE id=? AND revision=? AND state<>'SUBMITTED'
                """, input.offeringCode(), input.instructorId(), cohort, input.room().strip(), input.startsAt(),
                input.endsAt(), actor, id, input.revision()) == 1;
    }

    public boolean cancelSession(UUID id, long revision, String actor) {
        return jdbc.update("""
                UPDATE academic_session SET state='CANCELLED',revision=revision+1,updated_by=?
                WHERE id=? AND revision=? AND state='SCHEDULED'
                """, actor, id, revision) == 1;
    }

    public List<Map<String, Object>> timetable(String role, UUID account, OffsetDateTime start, OffsetDateTime end) {
        String query = """
                SELECT s.id,s.offering_code,o.name AS course,s.room,s.cohort_code,
                    s.starts_at,s.ends_at,s.state,s.revision,s.instructor_id,a.display_name AS instructor,
                    v.state AS availability,v.updated_at AS availability_updated_at
                FROM academic_session s JOIN course_offering o ON o.code=s.offering_code
                JOIN portal_account a ON a.id=s.instructor_id
                LEFT JOIN LATERAL (
                    SELECT state,updated_at FROM faculty_availability
                    WHERE instructor_id=s.instructor_id AND state<>'WITHDRAWN'
                        AND %s BETWEEN starts_on AND ends_on
                    ORDER BY daily DESC, updated_at DESC, id LIMIT 1
                ) v ON TRUE
                WHERE s.starts_at>=? AND s.starts_at<?
                """.formatted(CLASS_DATE);
        String order = " ORDER BY s.starts_at,s.offering_code";
        return switch (role) {
            case "FACULTY" -> jdbc.queryForList(query + " AND s.instructor_id=? AND " + TEACHES + order, start, end, account);
            case "STUDENT" -> jdbc.queryForList(query + " AND (" + ENROLLED + """
                     OR EXISTS (SELECT 1 FROM attendance_mark m WHERE m.session_id=s.id AND m.student_id=?))
                    """ + order, start, end, account, account);
            default -> jdbc.queryForList(query + order, start, end);
        };
    }

    public boolean assigned(UUID session, UUID instructor) {
        return jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM academic_session s WHERE s.id=? AND s.instructor_id=? AND "
                + TEACHES + ")", Boolean.class, session, instructor);
    }

    public List<Map<String, Object>> availability(UUID instructor) {
        return jdbc.queryForList("""
                SELECT v.id,v.instructor_id,a.display_name AS instructor,v.starts_on::text,v.ends_on::text,
                    v.state,v.daily,v.notes,v.revision,v.updated_at
                FROM faculty_availability v JOIN portal_account a ON a.id=v.instructor_id
                WHERE v.instructor_id=COALESCE(?,v.instructor_id)
                ORDER BY v.starts_on DESC,v.updated_at DESC
                """, instructor);
    }

    public Optional<Map<String, Object>> availabilityEntry(UUID id) {
        return jdbc.queryForList("SELECT * FROM faculty_availability WHERE id=?", id).stream().findFirst();
    }

    public boolean activeFaculty(UUID instructor) {
        return jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM faculty_profile f JOIN portal_account a ON a.id=f.account_id
                    WHERE f.account_id=? AND f.active AND a.role='FACULTY' AND a.enabled)
                """, Boolean.class, instructor);
    }

    public boolean availabilityOverlap(UUID id, Availability input, boolean daily) {
        return jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM faculty_availability
                    WHERE id<>? AND instructor_id=? AND daily=? AND state<>'WITHDRAWN'
                        AND starts_on<=? AND ends_on>=?)
                """, Boolean.class, id, input.instructorId(), daily, input.endsOn(), input.startsOn());
    }

    public boolean saveAvailability(UUID id, Availability input, boolean daily, String actor) {
        if (input.revision() == null) {
            return jdbc.update("""
                    INSERT INTO faculty_availability
                        (id,instructor_id,starts_on,ends_on,state,daily,notes,created_by,updated_by)
                    VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING
                    """, id, input.instructorId(), input.startsOn(), input.endsOn(), input.state().name(),
                    daily, input.notes(), actor, actor) == 1;
        }
        return jdbc.update("""
                UPDATE faculty_availability SET starts_on=?,ends_on=?,state=?,daily=?,notes=?,
                    updated_by=?,revision=revision+1 WHERE id=? AND instructor_id=? AND revision=?
                """, input.startsOn(), input.endsOn(), input.state().name(), daily, input.notes(),
                actor, id, input.instructorId(), input.revision()) == 1;
    }
}
