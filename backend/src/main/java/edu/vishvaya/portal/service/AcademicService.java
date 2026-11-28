package edu.vishvaya.portal.service;

import edu.vishvaya.portal.api.AcademicRequests.*;
import edu.vishvaya.portal.security.AccountPrincipal;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AcademicService {
    List<Map<String, Object>> enrollments(AccountPrincipal principal);
    void enroll(String offering, UUID student, Enrollment input, AccountPrincipal principal);
    List<Map<String, Object>> teachingChoices();
    List<Map<String, Object>> timetable(LocalDate week, AccountPrincipal principal);
    void schedule(UUID id, Session input, AccountPrincipal principal);
    void cancel(UUID id, Revision input, AccountPrincipal principal);
    Map<String, Object> roster(UUID id, AccountPrincipal principal);
    void submit(UUID id, Register input, AccountPrincipal principal);
    List<Map<String, Object>> history(AccountPrincipal principal);
    List<Map<String, Object>> reports(AccountPrincipal principal);
    void report(UUID id, UUID session, Report input, AccountPrincipal principal);
    void decide(UUID id, Decision input, AccountPrincipal principal);
    List<Map<String, Object>> availability(AccountPrincipal principal);
    void availability(UUID id, Availability input, AccountPrincipal principal);
}
