package edu.vishvaya.portal.service.impl;

import edu.vishvaya.portal.api.AcademicRequests.*;
import edu.vishvaya.portal.persistence.AcademicRepository;
import edu.vishvaya.portal.persistence.AccountRepository;
import edu.vishvaya.portal.persistence.AttendanceRepository;
import edu.vishvaya.portal.security.AccountPrincipal;
import edu.vishvaya.portal.service.AcademicService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AcademicServiceImpl implements AcademicService {
    private static final ZoneId CAMPUS = ZoneId.of("Asia/Kolkata");
    private final AcademicRepository academic;
    private final AttendanceRepository attendance;
    private final AccountRepository accounts;

    public AcademicServiceImpl(AcademicRepository academic, AttendanceRepository attendance, AccountRepository accounts) {
        this.academic = academic;
        this.attendance = attendance;
        this.accounts = accounts;
    }

    @Override
    public List<Map<String, Object>> enrollments(AccountPrincipal principal) {
        return academic.enrollments(principal.role().equals("STUDENT") ? principal.id() : null);
    }

    @Override
    @Transactional
    public void enroll(String offering, UUID student, Enrollment input, AccountPrincipal principal) {
        lock(principal);
        if (input.endsOn().isBefore(input.startsOn()) || !academic.eligibleEnrollment(offering, student, input)) {
            throw badRequest("Select an active student and offering in the same cohort, with dates within the term.");
        }
        if (!academic.saveEnrollment(offering, student, input, principal.id().toString())) throw stale();
    }

    @Override
    public List<Map<String, Object>> teachingChoices() {
        return academic.teachingChoices();
    }

    @Override
    public List<Map<String, Object>> timetable(LocalDate week, AccountPrincipal principal) {
        if (week.getYear() < 1900 || week.getYear() > 9999) throw badRequest("Choose a valid timetable week.");
        return academic.timetable(principal.role(), principal.id(), week.atStartOfDay(CAMPUS).toOffsetDateTime(),
                week.plusDays(7).atStartOfDay(CAMPUS).toOffsetDateTime());
    }

    @Override
    @Transactional
    public void schedule(UUID id, Session input, AccountPrincipal principal) {
        lock(principal);
        if (!input.endsAt().isAfter(input.startsAt())) throw badRequest("End time must be after start time.");
        LocalDate firstDate = input.startsAt().atZoneSameInstant(CAMPUS).toLocalDate();
        LocalDate lastDate = input.endsAt().minusNanos(1).atZoneSameInstant(CAMPUS).toLocalDate();
        if (!firstDate.equals(lastDate)) throw badRequest("A class must take place within one campus day.");
        String cohort = academic.schedulingCohort(input, firstDate, lastDate)
                .orElseThrow(() -> badRequest("Select an assigned instructor and a class date within the active term."));
        if (academic.overlaps(id, input, cohort)) {
            throw conflict("The room, instructor or cohort already has a class during that time.");
        }
        if (!academic.saveSession(id, input, cohort, principal.id().toString())) throw stale();
    }

    @Override
    @Transactional
    public void cancel(UUID id, Revision input, AccountPrincipal principal) {
        lock(principal);
        if (!academic.cancelSession(id, input.revision(), principal.id().toString())) throw stale();
    }

    @Override
    @Transactional
    public Map<String, Object> roster(UUID id, AccountPrincipal principal) {
        lock(principal);
        requireInstructor(id, principal);
        var session = academic.session(id).orElseThrow(AcademicServiceImpl::missing);
        return Map.of("state", session.get("state"), "revision", session.get("revision"),
                "started", !((Timestamp) session.get("starts_at")).toInstant().isAfter(Instant.now()),
                "students", attendance.roster(id));
    }

    @Override
    @Transactional
    public void submit(UUID id, Register input, AccountPrincipal principal) {
        lock(principal);
        requireInstructor(id, principal);
        var session = academic.session(id).orElseThrow(AcademicServiceImpl::missing);
        if ("SUBMITTED".equals(session.get("state"))
                && ((Number) session.get("revision")).longValue() == input.revision() + 1
                && attendance.matchesSubmission(id, input.marks())) {
            return;
        }
        if (!"SCHEDULED".equals(session.get("state")) || !input.revision().equals(session.get("revision"))) throw stale();
        if (((Timestamp) session.get("starts_at")).toInstant().isAfter(Instant.now())) {
            throw conflict("Attendance opens when the class starts.");
        }
        var students = attendance.roster(id).stream().map(row -> (UUID) row.get("student_id")).collect(Collectors.toSet());
        if (!students.equals(input.marks().keySet())) {
            throw conflict("The submitted register must match the entire current roster. Reload the class and try again.");
        }
        attendance.submit(id, input, principal.id().toString());
    }

    @Override
    public List<Map<String, Object>> history(AccountPrincipal principal) {
        return attendance.history(principal.id());
    }

    @Override
    public List<Map<String, Object>> reports(AccountPrincipal principal) {
        return attendance.reports(principal.role(), principal.id());
    }

    @Override
    @Transactional
    public void report(UUID id, UUID session, Report input, AccountPrincipal principal) {
        lock(principal);
        var existing = attendance.report(id);
        if (existing.isPresent()) {
            var report = existing.get();
            if (principal.id().equals(report.get("student_id")) && session.equals(report.get("session_id"))
                    && input.explanation().equals(report.get("explanation"))
                    && input.markRevision().equals(report.get("mark_revision"))) return;
            throw stale();
        }
        var mark = attendance.mark(session, principal.id()).orElseThrow(AcademicServiceImpl::missing);
        if (!input.markRevision().equals(mark.get("revision"))) throw stale();
        if (!attendance.openReport(id, session, principal.id(), input, (String) mark.get("mark"))) {
            throw conflict("You already have an open report for this class.");
        }
    }

    @Override
    @Transactional
    public void decide(UUID id, Decision input, AccountPrincipal principal) {
        lock(principal);
        var report = attendance.report(id).orElseThrow(AcademicServiceImpl::missing);
        UUID session = (UUID) report.get("session_id");
        UUID student = (UUID) report.get("student_id");
        requireInstructor(session, principal);
        String decision = input.accepted() ? "ACCEPTED" : "REJECTED";
        String correction = input.accepted() && input.correctedMark() != null ? input.correctedMark().name() : null;
        if (!"OPEN".equals(report.get("decision"))) {
            if (decision.equals(report.get("decision")) && input.reason().equals(report.get("review_reason"))
                    && Objects.equals(correction, report.get("corrected_mark"))
                    && principal.id().equals(report.get("reviewed_by"))
                    && ((Number) report.get("revision")).longValue() == input.revision() + 1) return;
            throw stale();
        }
        if (!input.revision().equals(report.get("revision"))) throw stale();
        if (input.accepted() && (correction == null || correction.equals(report.get("original_mark")))) {
            throw badRequest("Select a corrected mark that differs from the original.");
        }
        var mark = attendance.mark(session, student).orElseThrow(AcademicServiceImpl::missing);
        if (!mark.get("revision").equals(report.get("mark_revision"))) throw stale();
        attendance.decide(id, session, student, input, correction, principal.id());
    }

    @Override
    public List<Map<String, Object>> availability(AccountPrincipal principal) {
        return academic.availability(principal.role().equals("FACULTY") ? principal.id() : null);
    }

    @Override
    @Transactional
    public void availability(UUID id, Availability input, AccountPrincipal principal) {
        lock(principal);
        if (!principal.role().equals("ADMIN") && !principal.id().equals(input.instructorId())) throw missing();
        if (!academic.activeFaculty(input.instructorId())) throw badRequest("Select an active faculty profile.");
        var previous = academic.availabilityEntry(id);
        if (previous.isPresent() && !input.instructorId().equals(previous.get().get("instructor_id"))) throw missing();
        boolean withdrawal = input.state() == AvailabilityState.WITHDRAWN;
        if (withdrawal && previous.isEmpty()) throw missing();
        boolean daily = withdrawal ? (Boolean) previous.orElseThrow().get("daily")
                : input.state() != AvailabilityState.LEAVE;
        if (input.endsOn().isBefore(input.startsOn())) throw badRequest("End date must be on or after start date.");
        if (!withdrawal && daily && (!input.startsOn().equals(LocalDate.now(CAMPUS)) || !input.endsOn().equals(input.startsOn()))) {
            throw badRequest("Day-of availability applies to today in campus time.");
        }
        if (previous.isPresent() && daily != (Boolean) previous.get().get("daily")) {
            throw badRequest("Keep the original entry type when editing availability.");
        }
        if (!withdrawal && academic.availabilityOverlap(id, input, daily)) {
            throw conflict("An availability entry already covers those dates. Edit the existing entry.");
        }
        if (!academic.saveAvailability(id, input, daily, principal.id().toString())) throw stale();
    }

    private void lock(AccountPrincipal principal) {
        accounts.lockIdentity();
        if (accounts.byId(principal.id()).filter(account -> account.enabled()
                && !account.passwordChangeRequired() && account.role().equals(principal.role())
                && account.securityVersion() == principal.version()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This session is no longer authorized.");
        }
    }

    private void requireInstructor(UUID session, AccountPrincipal principal) {
        if (!principal.role().equals("ADMIN") && !academic.assigned(session, principal.id())) throw missing();
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private static ResponseStatusException stale() {
        return conflict("This record changed. Reload the section and try again.");
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic record not found.");
    }
}
