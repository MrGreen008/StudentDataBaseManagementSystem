package edu.vishvaya.portal.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class CollegeRepository {
    private final JdbcTemplate jdbc;
    public CollegeRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> list(CollegeResource resource) {
        return jdbc.queryForList("SELECT * FROM " + resource.table + " ORDER BY code");
    }

    public void save(CollegeResource resource, Map<String, Object> values, UUID actor) {
        // Table and column identifiers come exclusively from CollegeResource.
        String columns = String.join(", ", resource.fields);
        String parameters = resource.fields.stream().map(field -> "?").collect(Collectors.joining(", "));
        String updates = resource.fields.stream().filter(field -> !field.equals("code"))
                .map(field -> field + " = EXCLUDED." + field).collect(Collectors.joining(", "));
        var arguments = new java.util.ArrayList<Object>(resource.fields.stream().map(values::get).toList());
        arguments.add(actor.toString());
        arguments.add(actor.toString());
        jdbc.update("INSERT INTO " + resource.table + " (" + columns + ", created_by, updated_by)"
                + " VALUES (" + parameters + ", ?, ?) ON CONFLICT (code) DO UPDATE SET "
                + updates + ", updated_by = EXCLUDED.updated_by", arguments.toArray());
    }

    public List<Map<String, Object>> student(UUID account) {
        return jdbc.queryForList("""
                SELECT s.code, s.name, c.code AS cohort, p.name AS program
                FROM student_profile s JOIN college_cohort c ON c.code = s.cohort_code
                JOIN college_program p ON p.code = c.program_code
                WHERE s.account_id = ? AND s.active
                """, account);
    }

    public List<Map<String, Object>> faculty(UUID account) {
        return jdbc.queryForList("""
                SELECT o.code, o.name, o.course_code, o.cohort_code, o.term_code
                FROM faculty_profile f JOIN faculty_assignment a ON a.faculty_code = f.code
                JOIN course_offering o ON o.code = a.offering_code
                WHERE f.account_id = ? AND f.active AND a.active AND o.active ORDER BY o.code
                """, account);
    }
}
