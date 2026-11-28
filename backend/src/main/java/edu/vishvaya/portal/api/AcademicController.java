package edu.vishvaya.portal.api;

import edu.vishvaya.portal.api.AcademicRequests.*;
import edu.vishvaya.portal.security.AccountPrincipal;
import edu.vishvaya.portal.service.AcademicService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class AcademicController {
    private final AcademicService academic;

    public AcademicController(AcademicService academic) {
        this.academic = academic;
    }

    @GetMapping({"/api/admin/academic/enrollments", "/api/student/academic/enrollments"})
    public List<Map<String, Object>> enrollments(@AuthenticationPrincipal AccountPrincipal principal) {
        return academic.enrollments(principal);
    }

    @PutMapping("/api/admin/academic/enrollments/{offering}/{student}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enroll(@PathVariable String offering, @PathVariable UUID student, @Valid @RequestBody Enrollment input,
                       @AuthenticationPrincipal AccountPrincipal principal) {
        academic.enroll(offering, student, input, principal);
    }

    @GetMapping("/api/admin/academic/teaching-choices")
    public List<Map<String, Object>> choices() {
        return academic.teachingChoices();
    }

    @GetMapping({"/api/admin/academic/timetable", "/api/faculty/academic/timetable", "/api/student/academic/timetable"})
    public List<Map<String, Object>> timetable(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                                               @AuthenticationPrincipal AccountPrincipal principal) {
        return academic.timetable(week, principal);
    }

    @PutMapping("/api/admin/academic/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void schedule(@PathVariable UUID id, @Valid @RequestBody Session input,
                         @AuthenticationPrincipal AccountPrincipal principal) {
        academic.schedule(id, input, principal);
    }

    @PostMapping("/api/admin/academic/sessions/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id, @Valid @RequestBody Revision input,
                       @AuthenticationPrincipal AccountPrincipal principal) {
        academic.cancel(id, input, principal);
    }

    @GetMapping({"/api/faculty/academic/sessions/{id}/register", "/api/admin/academic/sessions/{id}/register"})
    public Map<String, Object> roster(@PathVariable UUID id, @AuthenticationPrincipal AccountPrincipal principal) {
        return academic.roster(id, principal);
    }

    @PutMapping("/api/faculty/academic/sessions/{id}/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submit(@PathVariable UUID id, @Valid @RequestBody Register input,
                       @AuthenticationPrincipal AccountPrincipal principal) {
        academic.submit(id, input, principal);
    }

    @GetMapping("/api/student/academic/attendance")
    public List<Map<String, Object>> history(@AuthenticationPrincipal AccountPrincipal principal) {
        return academic.history(principal);
    }

    @GetMapping({"/api/student/academic/reports", "/api/faculty/academic/reports", "/api/admin/academic/reports"})
    public List<Map<String, Object>> reports(@AuthenticationPrincipal AccountPrincipal principal) {
        return academic.reports(principal);
    }

    @PutMapping("/api/student/academic/sessions/{session}/reports/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@PathVariable UUID session, @PathVariable UUID id, @Valid @RequestBody Report input,
                       @AuthenticationPrincipal AccountPrincipal principal) {
        academic.report(id, session, input, principal);
    }

    @PutMapping({"/api/admin/academic/reports/{id}/decision", "/api/faculty/academic/reports/{id}/decision"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decide(@PathVariable UUID id, @Valid @RequestBody Decision input,
                       @AuthenticationPrincipal AccountPrincipal principal) {
        academic.decide(id, input, principal);
    }

    @GetMapping({"/api/admin/academic/availability", "/api/faculty/academic/availability"})
    public List<Map<String, Object>> availability(@AuthenticationPrincipal AccountPrincipal principal) {
        return academic.availability(principal);
    }

    @PutMapping({"/api/admin/academic/availability/{id}", "/api/faculty/academic/availability/{id}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void availability(@PathVariable UUID id, @Valid @RequestBody Availability input,
                             @AuthenticationPrincipal AccountPrincipal principal) {
        academic.availability(id, input, principal);
    }
}
