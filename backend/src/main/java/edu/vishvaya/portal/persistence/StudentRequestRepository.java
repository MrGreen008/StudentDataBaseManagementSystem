package edu.vishvaya.portal.persistence;

import edu.vishvaya.portal.api.StudentServicesRequests.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StudentRequestRepository {
    private final JdbcTemplate jdbc;

    public StudentRequestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> routes() {
        return jdbc.queryForList("SELECT kind,office,instructions,email,revision FROM student_service_route ORDER BY kind");
    }

    public boolean saveRoute(Kind kind, Route input, UUID actor) {
        String email = input.email() == null || input.email().isBlank() ? null : input.email();
        return jdbc.update("""
                UPDATE student_service_route SET office=?,instructions=?,email=?,revision=revision+1,updated_by=?
                WHERE kind=? AND revision=?
                """, input.office().strip(), input.instructions().strip(), email, actor.toString(),
                kind.name(), input.revision()) == 1;
    }

    public List<Map<String, Object>> requests(UUID owner) {
        return jdbc.queryForList("""
                SELECT r.id,r.student_id,a.display_name AS student,r.kind,r.exam_id,x.title AS exam,
                    r.details,r.state,r.response,r.reviewed_at,r.old_score,r.new_score,r.revision,r.created_at,
                    g.office,g.email,g.instructions,x.max_score
                FROM student_service_request r JOIN portal_account a ON a.id=r.student_id
                JOIN student_service_route g ON g.kind=r.kind LEFT JOIN college_exam x ON x.id=r.exam_id
                WHERE r.student_id=COALESCE(?,r.student_id)
                ORDER BY (r.state IN ('SUBMITTED','IN_REVIEW')) DESC,r.created_at DESC,r.id
                """, owner);
    }

    public Optional<Map<String, Object>> request(UUID id) {
        return jdbc.queryForList("SELECT * FROM student_service_request WHERE id=? FOR UPDATE", id).stream().findFirst();
    }

    public boolean submit(UUID id, Submission input, Long resultRevision, UUID owner) {
        return jdbc.update("""
                INSERT INTO student_service_request (id,student_id,kind,exam_id,details,result_revision,created_by,updated_by)
                VALUES (?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING
                """, id, owner, input.kind().name(), input.examId(), input.details(),
                resultRevision, owner.toString(), owner.toString()) == 1;
    }

    public void review(UUID id, Review input, BigDecimal previous, BigDecimal updated, UUID actor) {
        jdbc.update("""
                UPDATE student_service_request SET state=?,response=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP,
                    old_score=?,new_score=?,updated_by=?,revision=revision+1 WHERE id=?
                """, input.state().name(), input.response(), actor, previous, updated, actor.toString(), id);
    }

    public boolean hasExamRequests(UUID exam) {
        return jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM student_service_request WHERE exam_id=?)
                """, Boolean.class, exam);
    }

    public boolean hasDraftScores(UUID exam) {
        return jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM exam_result WHERE exam_id=?)", Boolean.class, exam);
    }
}
