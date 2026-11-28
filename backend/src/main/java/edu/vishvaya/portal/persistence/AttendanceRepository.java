package edu.vishvaya.portal.persistence;

import edu.vishvaya.portal.api.AcademicRequests.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AttendanceRepository {
    private final JdbcTemplate jdbc;

    public AttendanceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> roster(UUID session) {
        return jdbc.queryForList("""
                SELECT p.account_id AS student_id,p.name AS student, NULL AS mark
                FROM academic_session s JOIN academic_enrollment e ON e.offering_code=s.offering_code
                JOIN student_profile p ON p.account_id=e.student_id
                JOIN portal_account a ON a.id=p.account_id
                WHERE s.id=? AND s.state='SCHEDULED' AND p.active AND a.enabled AND a.role='STUDENT'
                    AND (s.starts_at AT TIME ZONE 'Asia/Kolkata')::date BETWEEN e.starts_on AND e.ends_on
                UNION ALL
                SELECT m.student_id,a.display_name AS student,m.mark
                FROM attendance_mark m JOIN portal_account a ON a.id=m.student_id WHERE m.session_id=?
                ORDER BY student,student_id
                """, session, session);
    }

    public void submit(UUID session, Register input, String actor) {
        jdbc.batchUpdate("""
                INSERT INTO attendance_mark (session_id,student_id,mark,submitted_mark,created_by,updated_by)
                VALUES (?,?,?,?,?,?)
                """, input.marks().entrySet().stream()
                .map(entry -> new Object[]{session, entry.getKey(), entry.getValue().name(), entry.getValue().name(),
                        actor, actor}).toList());
        jdbc.update("""
                UPDATE academic_session SET state='SUBMITTED',revision=revision+1,updated_by=? WHERE id=?
                """, actor, session);
    }

    public boolean matchesSubmission(UUID session, Map<UUID, Mark> marks) {
        var submitted = jdbc.queryForList("""
                SELECT student_id,submitted_mark FROM attendance_mark WHERE session_id=?
                """, session);
        return submitted.size() == marks.size() && submitted.stream().allMatch(row ->
                row.get("submitted_mark").equals(java.util.Objects.toString(marks.get(row.get("student_id")), null)));
    }

    public List<Map<String, Object>> history(UUID student) {
        return jdbc.queryForList("""
                SELECT s.id AS session_id,s.starts_at,o.name AS course,m.mark,m.revision
                FROM attendance_mark m JOIN academic_session s ON s.id=m.session_id
                JOIN course_offering o ON o.code=s.offering_code
                WHERE m.student_id=? ORDER BY s.starts_at DESC,s.id
                """, student);
    }

    public Optional<Map<String, Object>> mark(UUID session, UUID student) {
        return jdbc.queryForList("""
                SELECT mark,revision FROM attendance_mark WHERE session_id=? AND student_id=?
                """, session, student).stream().findFirst();
    }

    public Optional<Map<String, Object>> report(UUID id) {
        return jdbc.queryForList("""
                SELECT * FROM attendance_report WHERE id=? FOR UPDATE
                """, id).stream().findFirst();
    }

    public boolean openReport(UUID id, UUID session, UUID student, Report input, String original) {
        return jdbc.update("""
                INSERT INTO attendance_report (id,session_id,student_id,explanation,original_mark,mark_revision,
                    created_by,updated_by) VALUES (?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING
                """, id, session, student, input.explanation(), original, input.markRevision(),
                student.toString(), student.toString()) == 1;
    }

    public void decide(UUID id, UUID session, UUID student, Decision input, String mark, UUID actor) {
        if (input.accepted()) {
            jdbc.update("""
                    UPDATE attendance_mark SET mark=?,revision=revision+1,updated_by=?
                    WHERE session_id=? AND student_id=?
                    """, mark, actor.toString(), session, student);
        }
        jdbc.update("""
                UPDATE attendance_report SET decision=?,corrected_mark=?,review_reason=?,reviewed_by=?,
                    reviewed_at=CURRENT_TIMESTAMP,updated_by=?,revision=revision+1 WHERE id=?
                """, input.accepted() ? "ACCEPTED" : "REJECTED", mark, input.reason(),
                actor, actor.toString(), id);
    }

    public List<Map<String, Object>> reports(String role, UUID actor) {
        String query = """
                SELECT r.id,r.session_id,r.student_id,a.display_name AS student,s.starts_at,o.name AS course,
                    r.explanation,r.original_mark,r.decision,r.corrected_mark,r.review_reason,
                    r.reviewed_at,r.revision,r.created_at
                FROM attendance_report r JOIN academic_session s ON s.id=r.session_id
                JOIN course_offering o ON o.code=s.offering_code JOIN portal_account a ON a.id=r.student_id
                """;
        String order = " ORDER BY (r.decision='OPEN') DESC,r.created_at DESC,r.id";
        return switch (role) {
            case "FACULTY" -> jdbc.queryForList(query + " WHERE s.instructor_id=? AND " + AcademicRepository.TEACHES
                    + order, actor);
            case "STUDENT" -> jdbc.queryForList(query + " WHERE r.student_id=?" + order, actor);
            default -> jdbc.queryForList(query + order);
        };
    }
}
