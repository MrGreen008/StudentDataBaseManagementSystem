package edu.vishvaya.portal.persistence;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum CollegeResource {
    departments("academic_department", "display_order", "visible"),
    programs("college_program", "department_code", "active"),
    cohorts("college_cohort", "program_code", "active"),
    terms("academic_term", "starts_on", "ends_on", "active"),
    courses("college_course", "department_code", "active"),
    specializations("program_specialization", "program_code", "kind", "active"),
    offerings("course_offering", "course_code", "cohort_code", "term_code", "active"),
    students("student_profile", "account_id", "cohort_code", "active"),
    faculty("faculty_profile", "account_id", "department_code", "active"),
    assignments("faculty_assignment", "faculty_code", "offering_code", "active");

    public final String table;
    public final List<String> fields;

    CollegeResource(String table, String... fields) {
        this.table = table;
        var columns = new java.util.ArrayList<String>(List.of("code", "name"));
        columns.addAll(List.of(fields));
        this.fields = List.copyOf(columns);
    }

    public static CollegeResource resolve(String value) {
        try { return valueOf(value); }
        catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown college resource.");
        }
    }
}
