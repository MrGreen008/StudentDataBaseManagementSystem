package edu.vishvaya.portal.service.impl;

import edu.vishvaya.portal.api.StudentServicesRequests.*;
import edu.vishvaya.portal.persistence.AccountRepository;
import edu.vishvaya.portal.persistence.ExamRepository;
import edu.vishvaya.portal.persistence.StudentRequestRepository;
import edu.vishvaya.portal.security.AccountPrincipal;
import edu.vishvaya.portal.service.StudentServices;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class StudentServicesImpl implements StudentServices {
    private static final ZoneId CAMPUS = ZoneId.of("Asia/Kolkata");
    private final ExamRepository exams;
    private final StudentRequestRepository requests;
    private final AccountRepository accounts;

    public StudentServicesImpl(ExamRepository exams, StudentRequestRepository requests, AccountRepository accounts) {
        this.exams = exams;
        this.requests = requests;
        this.accounts = accounts;
    }

    @Override
    public List<Map<String, Object>> schedules(AccountPrincipal principal) {
        return exams.schedules(principal.role(), principal.id());
    }

    @Override
    @Transactional
    public void saveExam(UUID id, Exam input, AccountPrincipal principal) {
        lock(principal);
        if (input.state() == ExamState.RELEASED || (input.state() == ExamState.CANCELLED && input.revision() == null)) {
            throw badRequest("Choose Draft or Scheduled for a new exam. Release results from the register.");
        }
        var start = input.startsAt().atZoneSameInstant(CAMPUS).toLocalDate();
        if (!input.endsAt().isAfter(input.startsAt())
                || !start.equals(input.endsAt().minusNanos(1).atZoneSameInstant(CAMPUS).toLocalDate())
                || input.passScore().compareTo(input.maxScore()) > 0) {
            throw badRequest("Use a single campus date, increasing times, and a passing score within the maximum.");
        }
        var previous = exams.exam(id);
        if (previous.isPresent()) {
            var exam = previous.get();
            if ("RELEASED".equals(exam.get("state")) || "CANCELLED".equals(exam.get("state"))
                    || !Objects.equals(input.revision(), exam.get("revision"))) throw stale();
            boolean sensitiveEdit = !input.offeringCode().equals(exam.get("offering_code"))
                    || !input.kind().name().equals(exam.get("kind"))
                    || !instant(exam, "starts_at").equals(input.startsAt().toInstant())
                    || !instant(exam, "ends_at").equals(input.endsAt().toInstant())
                    || !sameScore(input.maxScore(), exam.get("max_score"))
                    || !sameScore(input.passScore(), exam.get("pass_score"));
            if ((sensitiveEdit || input.state() == ExamState.DRAFT)
                    && (requests.hasExamRequests(id) || requests.hasDraftScores(id))) {
                throw conflict("Keep the published schedule and scoring rules once requests or marks exist. Cancel the exam if it cannot proceed.");
            }
        }
        if (input.state() != ExamState.CANCELLED
                && !exams.validOffering(input.offeringCode(), input.kind() == ExamKind.REGULAR, start)) {
            throw badRequest("Select a valid offering. Regular exams must fall within the offering's term.");
        }
        if (input.state() == ExamState.SCHEDULED && exams.overlaps(id, input)) {
            throw conflict("The room or cohort already has an exam during that time.");
        }
        if (!exams.save(id, input, principal.id().toString())) throw stale();
    }

    @Override
    public List<Map<String, Object>> roster(UUID id) {
        return exams.roster(id);
    }

    @Override
    @Transactional
    public void score(UUID exam, UUID student, Score input, AccountPrincipal principal) {
        lock(principal);
        var row = writableExam(exam);
        if (!input.examRevision().equals(row.get("revision"))) throw stale();
        if (exams.roster(exam).stream().noneMatch(entry -> student.equals(entry.get("student_id"))
                && Boolean.TRUE.equals(entry.get("eligible")))) {
            throw missing("Student is outside this exam's eligible roster.");
        }
        checkScore(input.score(), row);
        if (!exams.saveScore(exam, student, input, principal.id())) throw stale();
        exams.touchExam(exam, principal.id());
    }

    @Override
    @Transactional
    public void discardScore(UUID exam, UUID student, long revision, AccountPrincipal principal) {
        lock(principal);
        writableExam(exam);
        if (!exams.discardDraft(exam, student, revision)) throw stale();
        exams.touchExam(exam, principal.id());
    }

    @Override
    @Transactional
    public void release(UUID exam, long revision, AccountPrincipal principal) {
        lock(principal);
        var row = exams.exam(exam).orElseThrow(() -> missing("Exam not found."));
        if ("RELEASED".equals(row.get("state")) && ((Number) row.get("revision")).longValue() == revision + 1) return;
        if (!"SCHEDULED".equals(row.get("state")) || !Objects.equals(revision, row.get("revision"))) throw stale();
        if (instant(row, "ends_at").isAfter(Instant.now())) throw conflict("Results can be released after the exam ends.");
        var roster = exams.roster(exam);
        if (roster.isEmpty() || roster.stream().anyMatch(student -> !Boolean.TRUE.equals(student.get("eligible"))
                || student.get("score") == null)) {
            throw conflict("Every eligible student needs a mark. Remove draft marks for anyone outside the roster.");
        }
        exams.release(exam, principal.id());
    }

    @Override
    public List<Map<String, Object>> results(UUID student) {
        return exams.results(student);
    }

    @Override
    public List<Map<String, Object>> backlogOptions(UUID student) {
        return exams.backlogOptions(student);
    }

    @Override
    public List<Map<String, Object>> requests(AccountPrincipal principal) {
        return requests.requests(principal.role().equals("STUDENT") ? principal.id() : null);
    }

    @Override
    @Transactional
    public void submit(UUID id, Submission input, AccountPrincipal principal) {
        lock(principal);
        var previous = requests.request(id);
        if (previous.isPresent()) {
            var row = previous.get();
            if (principal.id().equals(row.get("student_id")) && input.kind().name().equals(row.get("kind"))
                    && Objects.equals(input.examId(), row.get("exam_id")) && input.details().equals(row.get("details"))) return;
            throw stale();
        }
        UUID examId = input.examId();
        Long resultRevision = null;
        if (input.kind() == Kind.BACKLOG) {
            if (examId == null || !exams.backlogEligible(examId, principal.id())) {
                throw badRequest("Choose an eligible, upcoming backlog exam.");
            }
        } else if (resultRequest(input.kind())) {
            if (examId == null) throw badRequest("Choose a published result.");
            var result = publishedResult(examId, principal.id());
            resultRevision = (Long) result.get("revision");
        } else if (examId != null) {
            throw badRequest("This request type does not use an exam.");
        }
        if (!requests.submit(id, input, resultRevision, principal.id())) {
            throw conflict("An open request already exists for this exam.");
        }
    }

    @Override
    @Transactional
    public void review(UUID id, Review input, AccountPrincipal principal) {
        lock(principal);
        var row = requests.request(id).orElseThrow(() -> missing("Request not found."));
        Kind kind = Kind.valueOf((String) row.get("kind"));
        RequestState current = RequestState.valueOf((String) row.get("state"));
        boolean approved = input.state() == RequestState.APPROVED;
        boolean approveResult = approved && resultRequest(kind);
        if (!input.revision().equals(row.get("revision"))) {
            if (((Number) row.get("revision")).longValue() == input.revision() + 1
                    && principal.id().equals(row.get("reviewed_by")) && input.state() == current
                    && input.response().equals(row.get("response"))
                    && sameScore(input.score(), row.get("new_score"))) return;
            throw stale();
        }
        if (!transition(kind, current, input.state())) throw badRequest("Choose a valid next status for this request.");
        if (!approveResult && input.score() != null) throw badRequest("A new score applies only to approved result requests.");
        UUID examId = (UUID) row.get("exam_id");
        UUID owner = (UUID) row.get("student_id");
        if (approved && kind == Kind.BACKLOG && !exams.backlogEligible(examId, owner)) {
            throw conflict("This student is no longer eligible for this backlog sitting.");
        }
        BigDecimal oldScore = null, newScore = null;
        if (approveResult) {
            var result = publishedResult(examId, owner);
            if (!result.get("revision").equals(row.get("result_revision"))) throw stale();
            if (kind == Kind.RESULT_CORRECTION && input.score() == null) {
                throw badRequest("Enter the corrected score.");
            }
            oldScore = (BigDecimal) result.get("score");
            newScore = input.score();
            if (newScore != null) {
                checkScore(newScore, result);
                if (!exams.updateScore(examId, owner, newScore, (Long) result.get("revision"), principal.id())) throw stale();
            }
        }
        requests.review(id, input, oldScore, newScore, principal.id());
    }

    @Override
    public List<Map<String, Object>> routes() {
        return requests.routes();
    }

    @Override
    @Transactional
    public void saveRoute(Kind kind, Route input, AccountPrincipal principal) {
        lock(principal);
        if (!requests.saveRoute(kind, input, principal.id())) throw stale();
    }

    private static boolean transition(Kind kind, RequestState current, RequestState next) {
        return switch (current) {
            case SUBMITTED -> next == RequestState.IN_REVIEW || next == RequestState.APPROVED || next == RequestState.REJECTED;
            case IN_REVIEW -> next == RequestState.APPROVED || next == RequestState.REJECTED;
            case APPROVED -> kind != Kind.BACKLOG && !resultRequest(kind) && next == RequestState.COMPLETED;
            default -> false;
        };
    }

    private static boolean resultRequest(Kind kind) {
        return kind == Kind.REVALUATION || kind == Kind.RESULT_CORRECTION;
    }

    private Map<String, Object> writableExam(UUID id) {
        var exam = exams.exam(id).orElseThrow(() -> missing("Exam not found."));
        if (!"SCHEDULED".equals(exam.get("state"))) throw conflict("Draft marks require a scheduled exam.");
        return exam;
    }

    private Map<String, Object> publishedResult(UUID exam, UUID student) {
        return exams.result(exam, student).filter(row -> "RELEASED".equals(row.get("state")))
                .orElseThrow(() -> missing("Published result not found."));
    }

    private static Instant instant(Map<String, Object> row, String field) {
        return ((Timestamp) row.get(field)).toInstant();
    }

    private static boolean sameScore(BigDecimal score, Object stored) {
        return score == null ? stored == null : stored instanceof BigDecimal other && score.compareTo(other) == 0;
    }

    private static void checkScore(BigDecimal score, Map<String, Object> exam) {
        if (score.compareTo((BigDecimal) exam.get("max_score")) > 0) throw badRequest("Score exceeds the exam maximum.");
    }

    private void lock(AccountPrincipal principal) {
        accounts.lockIdentity();
        if (accounts.byId(principal.id()).filter(account -> account.enabled() && !account.passwordChangeRequired()
                && account.role().equals(principal.role()) && account.securityVersion() == principal.version()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This session is no longer authorized.");
        }
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

    private static ResponseStatusException missing(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
