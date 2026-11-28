package edu.vishvaya.portal.api;

import edu.vishvaya.portal.api.AcademicRequests.Revision;
import edu.vishvaya.portal.api.StudentServicesRequests.*;
import edu.vishvaya.portal.security.AccountPrincipal;
import edu.vishvaya.portal.service.StudentServices;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class StudentServicesController {
    private final StudentServices services;

    public StudentServicesController(StudentServices services) {
        this.services = services;
    }

    @GetMapping({"/api/admin/exams", "/api/faculty/exams", "/api/student/exams"})
    public List<Map<String, Object>> schedules(@AuthenticationPrincipal AccountPrincipal principal) {
        return services.schedules(principal);
    }

    @PutMapping("/api/admin/exams/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveExam(@PathVariable UUID id, @Valid @RequestBody Exam input,
                         @AuthenticationPrincipal AccountPrincipal principal) {
        services.saveExam(id, input, principal);
    }

    @GetMapping("/api/admin/exams/{id}/roster")
    public List<Map<String, Object>> roster(@PathVariable UUID id) {
        return services.roster(id);
    }

    @PutMapping("/api/admin/exams/{id}/scores/{student}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void score(@PathVariable UUID id, @PathVariable UUID student, @Valid @RequestBody Score input,
                      @AuthenticationPrincipal AccountPrincipal principal) {
        services.score(id, student, input, principal);
    }

    @DeleteMapping("/api/admin/exams/{id}/scores/{student}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discardScore(@PathVariable UUID id, @PathVariable UUID student, @Valid @RequestBody Revision input,
                             @AuthenticationPrincipal AccountPrincipal principal) {
        services.discardScore(id, student, input.revision(), principal);
    }

    @PostMapping("/api/admin/exams/{id}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable UUID id, @Valid @RequestBody Revision input,
                        @AuthenticationPrincipal AccountPrincipal principal) {
        services.release(id, input.revision(), principal);
    }

    @GetMapping("/api/student/results")
    public List<Map<String, Object>> results(@AuthenticationPrincipal AccountPrincipal principal) {
        return services.results(principal.id());
    }

    @GetMapping("/api/student/backlog-exams")
    public List<Map<String, Object>> backlog(@AuthenticationPrincipal AccountPrincipal principal) {
        return services.backlogOptions(principal.id());
    }

    @GetMapping({"/api/admin/student-services/requests", "/api/student/services/requests"})
    public List<Map<String, Object>> requests(@AuthenticationPrincipal AccountPrincipal principal) {
        return services.requests(principal);
    }

    @PutMapping("/api/student/services/requests/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submit(@PathVariable UUID id, @Valid @RequestBody Submission input,
                       @AuthenticationPrincipal AccountPrincipal principal) {
        services.submit(id, input, principal);
    }

    @PutMapping("/api/admin/student-services/requests/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void review(@PathVariable UUID id, @Valid @RequestBody Review input,
                       @AuthenticationPrincipal AccountPrincipal principal) {
        services.review(id, input, principal);
    }

    @GetMapping({"/api/admin/student-services/offices", "/api/student/services/offices"})
    public List<Map<String, Object>> routes() {
        return services.routes();
    }

    @PutMapping("/api/admin/student-services/offices/{kind}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void route(@PathVariable Kind kind, @Valid @RequestBody Route input,
                      @AuthenticationPrincipal AccountPrincipal principal) {
        services.saveRoute(kind, input, principal);
    }
}
