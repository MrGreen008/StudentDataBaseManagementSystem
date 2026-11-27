package edu.vishvaya.portal.persistence;

import edu.vishvaya.portal.api.NoticeRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class NoticeRepository {
    private final JdbcTemplate jdbc;
    public NoticeRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<Map<String, Object>> list() {
        return jdbc.queryForList("SELECT * FROM campus_announcement ORDER BY published_at DESC");
    }
    public void save(UUID id, NoticeRequest notice, UUID actor) {
        jdbc.update("""
                INSERT INTO campus_announcement
                (id, category, title, summary, priority, published, published_at, expires_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET category = EXCLUDED.category, title = EXCLUDED.title,
                    summary = EXCLUDED.summary, priority = EXCLUDED.priority, published = EXCLUDED.published,
                    published_at = EXCLUDED.published_at, expires_at = EXCLUDED.expires_at,
                    updated_by = EXCLUDED.updated_by
                """, id, notice.category(), notice.title(), notice.summary(), notice.priority(),
                notice.published(), notice.publishedAt(), notice.expiresAt(), actor.toString(), actor.toString());
    }
}
