package edu.vishvaya.portal.persistence;

import edu.vishvaya.portal.api.PortalNoticeRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class PortalNoticeRepository {
    private final JdbcTemplate jdbc;
    public PortalNoticeRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> inbox(UUID account) {
        return jdbc.queryForList("""
                SELECT n.id, n.title, n.summary, n.published_at, n.audience, n.approval_status,
                    n.attachment_name, n.published, n.revision, n.topic
                FROM portal_notice n JOIN portal_account a ON a.id = ?
                WHERE a.role = 'ADMIN' OR (
                  n.published AND n.approval_status='APPROVED' AND n.published_at <= CURRENT_TIMESTAMP
                  AND (n.expires_at IS NULL OR n.expires_at > CURRENT_TIMESTAMP)
                  AND (a.role='FACULTY' AND n.audience IN ('STUDENT','FACULTY') OR (
                  a.role='STUDENT' AND n.audience='STUDENT'
                  AND (n.account_id IS NULL OR n.account_id = a.id)
                  AND (n.cohort_code IS NULL OR EXISTS (
                    SELECT 1 FROM student_profile s
                    WHERE s.account_id = a.id AND s.active AND s.cohort_code = n.cohort_code)))))
                ORDER BY n.published_at DESC
                """, account);
    }

    public List<Map<String, Object>> managed(UUID author, boolean admin) {
        return jdbc.queryForList("""
                SELECT id,title,summary,audience,cohort_code,account_id,published,published_at,
                    expires_at,created_by,approval_status,attachment_name,revision,topic
                FROM portal_notice WHERE (? OR created_by = ?) ORDER BY updated_at DESC
                """,
                admin, author.toString());
    }

    public void lockIdentity() {
        jdbc.queryForObject("SELECT id FROM identity_lock WHERE id = 1 FOR UPDATE", Integer.class);
    }

    public boolean owns(UUID id, UUID author) {
        return jdbc.queryForObject("SELECT NOT EXISTS (SELECT 1 FROM portal_notice WHERE id = ? AND created_by <> ?)",
                Boolean.class, id, author.toString());
    }

    public boolean teaches(UUID account, String cohort) {
        return jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM faculty_profile f
                JOIN faculty_assignment a ON a.faculty_code = f.code
                JOIN course_offering o ON o.code = a.offering_code
                WHERE f.account_id = ? AND f.active AND a.active AND o.active AND o.cohort_code = ?)
                """, Boolean.class, account, cohort);
    }

    public void save(UUID id, PortalNoticeRequest notice, UUID actor) {
        jdbc.update("""
                INSERT INTO portal_notice (id,title,summary,audience,cohort_code,account_id,published,
                    published_at,expires_at,created_by,updated_by,topic)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,COALESCE(?, 'GENERAL'))
                ON CONFLICT (id) DO UPDATE SET title=EXCLUDED.title,summary=EXCLUDED.summary,
                    audience=EXCLUDED.audience,cohort_code=EXCLUDED.cohort_code,account_id=EXCLUDED.account_id,
                    published=EXCLUDED.published,published_at=EXCLUDED.published_at,
                    expires_at=EXCLUDED.expires_at,updated_by=EXCLUDED.updated_by,topic=COALESCE(?,portal_notice.topic),
                    revision=portal_notice.revision+1
                """,id,notice.title(),notice.summary(),notice.audience(),notice.cohortCode(),notice.accountId(),
                notice.published(),notice.publishedAt(),notice.expiresAt(),actor.toString(),actor.toString(),
                notice.topic(),notice.topic());
    }

    public void reviewState(UUID id, String state, boolean published, UUID actor) {
        jdbc.update("UPDATE portal_notice SET approval_status=?,published=?,approved_by=?,updated_by=? WHERE id=?",
                state,published,state.equals("APPROVED")?actor.toString():null,actor.toString(),id);
    }
    public boolean approve(UUID id,long revision,boolean approve,UUID actor) {
        return jdbc.update("""
                UPDATE portal_notice SET approval_status=?,published=?,approved_by=?,updated_by=?,revision=revision+1
                WHERE id=? AND revision=? AND approval_status='PENDING'
                """,approve?"APPROVED":"REJECTED",approve,actor.toString(),actor.toString(),id,revision)==1;
    }
    public Optional<Map<String,Object>> attachment(UUID id) {
        return jdbc.queryForList("SELECT attachment_name,attachment_data,created_by FROM portal_notice WHERE id=?",id)
            .stream().findFirst();
    }
    public void attach(UUID id,String name,byte[] data,UUID actor,boolean admin) {
        jdbc.update("""
                UPDATE portal_notice SET attachment_name=?,attachment_data=?,revision=revision+1,
                    updated_by=?,approval_status=CASE WHEN ? THEN approval_status ELSE 'DRAFT' END,
                    published=CASE WHEN ? THEN published ELSE FALSE END WHERE id=?
                """,name,data,actor.toString(),admin,admin,id);
    }
}
