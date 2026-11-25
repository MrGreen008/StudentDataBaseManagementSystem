package edu.vishvaya.portal.persistence;

import edu.vishvaya.portal.api.AnnouncementResponse;
import edu.vishvaya.portal.api.CollegeOverviewResponse;
import edu.vishvaya.portal.api.DepartmentResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class PublicContentRepository {

    private final JdbcTemplate jdbc;

    public PublicContentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AnnouncementResponse> findPublishedAnnouncements() {
        return jdbc.query("""
                SELECT id, category, title, summary, action_label, action_url, published_at
                FROM campus_announcement
                WHERE published = TRUE
                  AND published_at <= CURRENT_TIMESTAMP
                  AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
                ORDER BY priority DESC, published_at DESC
                LIMIT 12
                """, (rs, row) -> new AnnouncementResponse(
                rs.getObject("id", UUID.class),
                rs.getString("category"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("action_label"),
                rs.getString("action_url"),
                rs.getObject("published_at", java.time.OffsetDateTime.class)
        ));
    }

    public CollegeOverviewResponse findCollegeOverview() {
        return jdbc.queryForObject("""
                SELECT college_name, city, student_count, faculty_count,
                       publication_count, patent_count, project_count
                FROM college_profile
                WHERE profile_key = 'public'
                """, (rs, row) -> new CollegeOverviewResponse(
                rs.getString("college_name"),
                rs.getString("city"),
                rs.getLong("student_count"),
                rs.getLong("faculty_count"),
                rs.getLong("publication_count"),
                rs.getLong("patent_count"),
                rs.getLong("project_count")
        ));
    }

    public List<DepartmentResponse> findDepartments() {
        return jdbc.query("""
                SELECT code, name
                FROM academic_department
                WHERE visible = TRUE
                ORDER BY display_order, name
                """, (rs, row) -> new DepartmentResponse(
                rs.getString("code"), rs.getString("name")
        ));
    }
}
