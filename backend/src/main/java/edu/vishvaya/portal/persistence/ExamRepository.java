package edu.vishvaya.portal.persistence;

import edu.vishvaya.portal.api.StudentServicesRequests.Exam;
import edu.vishvaya.portal.api.StudentServicesRequests.Score;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ExamRepository {
    private static final String FRESH_BACKLOG = """
            x.kind='BACKLOG' AND x.state='SCHEDULED' AND x.starts_at>CURRENT_TIMESTAMP
            """;
    private static final String OUTSTANDING_FAILURE = """
            EXISTS (SELECT 1 FROM exam_result m JOIN college_exam prior ON prior.id=m.exam_id
                WHERE m.student_id=? AND prior.offering_code=x.offering_code AND prior.state='RELEASED'
                    AND m.score<prior.pass_score)
            AND NOT EXISTS (SELECT 1 FROM exam_result m JOIN college_exam prior ON prior.id=m.exam_id
                WHERE m.student_id=? AND prior.offering_code=x.offering_code AND prior.state='RELEASED'
                    AND m.score>=prior.pass_score)
            """;
    private static final String NO_UPCOMING_REGISTRATION = """
            NOT EXISTS (SELECT 1 FROM student_service_request r
                JOIN college_exam other ON other.id=r.exam_id
                WHERE r.student_id=? AND r.kind='BACKLOG' AND r.state='APPROVED'
                    AND other.offering_code=x.offering_code AND other.state='SCHEDULED'
                    AND other.starts_at > CURRENT_TIMESTAMP)
            """;
    private static final String REGULAR_ROSTER = """
            SELECT e.student_id FROM academic_enrollment e
            JOIN student_profile p ON p.account_id=e.student_id
            JOIN portal_account a ON a.id=e.student_id
            WHERE e.offering_code=x.offering_code AND p.active AND a.enabled AND a.role='STUDENT'
                AND (x.starts_at AT TIME ZONE 'Asia/Kolkata')::date BETWEEN e.starts_on AND e.ends_on
            """;
    private static final String BACKLOG_ROSTER = """
            SELECT r.student_id FROM student_service_request r
            WHERE r.exam_id=x.id AND r.kind='BACKLOG' AND r.state='APPROVED'
            """;
    private final JdbcTemplate jdbc;

    public ExamRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Map<String, Object>> exam(UUID id) {
        return jdbc.queryForList("SELECT * FROM college_exam WHERE id=? FOR UPDATE", id).stream().findFirst();
    }

    public boolean validOffering(String offering, boolean regular, LocalDate date) {
        return jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM course_offering o JOIN academic_term t ON t.code=o.term_code
                    WHERE o.code=? AND (NOT ? OR (o.active AND ? BETWEEN t.starts_on AND t.ends_on)))
                """, Boolean.class, offering, regular, date);
    }

    public boolean overlaps(UUID id, Exam input) {
        return jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM college_exam x
                    JOIN course_offering existing ON existing.code=x.offering_code
                    JOIN course_offering proposed ON proposed.code=?
                    WHERE x.id<>? AND x.state IN ('SCHEDULED','RELEASED')
                        AND x.starts_at<? AND x.ends_at>?
                        AND (lower(x.room)=lower(?) OR existing.cohort_code=proposed.cohort_code))
                """, Boolean.class, input.offeringCode(), id, input.endsAt(), input.startsAt(), input.room().strip());
    }

    public boolean save(UUID id, Exam input, String actor) {
        if (input.revision() == null) {
            return jdbc.update("""
                    INSERT INTO college_exam (id,offering_code,title,kind,room,starts_at,ends_at,max_score,
                        pass_score,state,created_by,updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT DO NOTHING
                    """, id, input.offeringCode(), input.title().strip(), input.kind().name(), input.room().strip(),
                    input.startsAt(), input.endsAt(), input.maxScore(), input.passScore(), input.state().name(),
                    actor, actor) == 1;
        }
        return jdbc.update("""
                UPDATE college_exam SET offering_code=?,title=?,kind=?,room=?,starts_at=?,ends_at=?,max_score=?,
                    pass_score=?,state=?,revision=revision+1,updated_by=?
                WHERE id=? AND revision=? AND state IN ('DRAFT','SCHEDULED')
                """, input.offeringCode(), input.title().strip(), input.kind().name(), input.room().strip(),
                input.startsAt(), input.endsAt(), input.maxScore(), input.passScore(), input.state().name(),
                actor, id, input.revision()) == 1;
    }

    public List<Map<String, Object>> schedules(String role, UUID viewer) {
        String query = """
                SELECT x.id,x.offering_code,o.name AS course,o.cohort_code,o.term_code,x.title,x.kind,x.room,
                    x.starts_at,x.ends_at,x.max_score,x.pass_score,x.state,x.released_at,x.revision
                FROM college_exam x JOIN course_offering o ON o.code=x.offering_code
                """;
        String order = " ORDER BY x.starts_at DESC,x.title,x.id";
        return switch (role) {
            case "STUDENT" -> jdbc.queryForList(query + """
                    WHERE x.state<>'DRAFT' AND (
                        (x.kind='REGULAR' AND ? IN (
                    """ + REGULAR_ROSTER + """
                        ))
                        OR EXISTS (SELECT 1 FROM exam_result m WHERE m.exam_id=x.id AND m.student_id=? AND x.state='RELEASED')
                        OR EXISTS (SELECT 1 FROM student_service_request r WHERE r.exam_id=x.id AND r.student_id=? AND r.kind='BACKLOG')
                        OR (
                    """ + FRESH_BACKLOG + " AND " + OUTSTANDING_FAILURE + "))" + order, viewer, viewer, viewer, viewer, viewer);
            case "FACULTY" -> jdbc.queryForList(query + """
                    WHERE x.state<>'DRAFT' AND EXISTS (
                        SELECT 1 FROM faculty_assignment a JOIN faculty_profile f ON f.code=a.faculty_code
                        WHERE a.offering_code=x.offering_code AND a.active AND f.active AND f.account_id=?
                    )
                    """ + order, viewer);
            default -> jdbc.queryForList(query + order);
        };
    }

    public List<Map<String, Object>> backlogOptions(UUID student) {
        return jdbc.queryForList("""
                SELECT x.id,x.title,o.name AS course,x.starts_at
                FROM college_exam x JOIN course_offering o ON o.code=x.offering_code WHERE
                """ + FRESH_BACKLOG + " AND " + OUTSTANDING_FAILURE + " AND " + NO_UPCOMING_REGISTRATION + """
                 AND NOT EXISTS (SELECT 1 FROM student_service_request r WHERE r.exam_id=x.id AND r.student_id=?
                    AND r.state IN ('SUBMITTED','IN_REVIEW','APPROVED'))
                ORDER BY x.starts_at,x.id
                """, student, student, student, student);
    }

    public boolean backlogEligible(UUID id, UUID student) {
        return jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM college_exam x WHERE x.id=? AND "
                + FRESH_BACKLOG + " AND " + OUTSTANDING_FAILURE + " AND " + NO_UPCOMING_REGISTRATION + ")",
                Boolean.class, id, student, student, student);
    }

    public List<Map<String, Object>> roster(UUID id) {
        return jdbc.queryForList("""
                WITH eligible AS (
                    SELECT student_id FROM college_exam x CROSS JOIN LATERAL (
                """ + REGULAR_ROSTER + """
                    ) regular WHERE x.id=? AND x.kind='REGULAR' AND x.state<>'RELEASED'
                    UNION ALL
                    SELECT student_id FROM college_exam x CROSS JOIN LATERAL (
                """ + BACKLOG_ROSTER + """
                    ) backlog WHERE x.id=? AND x.kind='BACKLOG' AND x.state<>'RELEASED'
                    UNION ALL
                    SELECT m.student_id FROM exam_result m JOIN college_exam x ON x.id=m.exam_id
                    WHERE x.id=? AND x.state='RELEASED'
                )
                SELECT a.id AS student_id,a.display_name AS student,m.score,m.revision,(e.student_id IS NOT NULL) AS eligible
                FROM eligible e FULL JOIN (SELECT * FROM exam_result WHERE exam_id=?) m USING (student_id)
                JOIN portal_account a ON a.id=COALESCE(e.student_id,m.student_id)
                ORDER BY a.display_name,a.id
                """, id, id, id, id);
    }

    public Optional<Map<String, Object>> result(UUID id, UUID student) {
        return jdbc.queryForList("""
                SELECT m.score,m.revision,x.state,x.max_score FROM exam_result m JOIN college_exam x ON x.id=m.exam_id
                WHERE m.exam_id=? AND m.student_id=?
                """, id, student).stream().findFirst();
    }

    public List<Map<String, Object>> results(UUID student) {
        return jdbc.queryForList("""
                SELECT x.id AS exam_id,x.title,o.name AS course,x.kind,x.starts_at,x.released_at,
                    m.score,x.max_score,x.pass_score,
                    CASE WHEN m.score>=x.pass_score THEN 'PASS' ELSE 'FAIL' END AS outcome,m.updated_at
                FROM exam_result m JOIN college_exam x ON x.id=m.exam_id JOIN course_offering o ON o.code=x.offering_code
                WHERE m.student_id=? AND x.state='RELEASED' ORDER BY x.starts_at DESC,x.id
                """, student);
    }

    public boolean saveScore(UUID exam, UUID student, Score input, UUID actor) {
        if (input.revision() == null) {
            return jdbc.update("""
                    INSERT INTO exam_result (exam_id,student_id,score,created_by,updated_by)
                    VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING
                    """, exam, student, input.score(), actor.toString(), actor.toString()) == 1;
        }
        return updateScore(exam, student, input.score(), input.revision(), actor);
    }

    public boolean updateScore(UUID exam, UUID student, BigDecimal score, long revision, UUID actor) {
        return jdbc.update("""
                UPDATE exam_result SET score=?,revision=revision+1,updated_by=?
                WHERE exam_id=? AND student_id=? AND revision=?
                """, score, actor.toString(), exam, student, revision) == 1;
    }

    public boolean discardDraft(UUID exam, UUID student, long revision) {
        return jdbc.update("DELETE FROM exam_result WHERE exam_id=? AND student_id=? AND revision=?",
                exam, student, revision) == 1;
    }

    public void touchExam(UUID exam, UUID actor) {
        jdbc.update("UPDATE college_exam SET revision=revision+1,updated_by=? WHERE id=?", actor.toString(), exam);
    }

    public void release(UUID exam, UUID actor) {
        jdbc.update("""
                UPDATE college_exam SET state='RELEASED',released_at=CURRENT_TIMESTAMP,
                    revision=revision+1,updated_by=? WHERE id=?
                """, actor.toString(), exam);
    }
}
