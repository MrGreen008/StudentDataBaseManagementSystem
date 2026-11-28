package edu.vishvaya.portal.service;

import edu.vishvaya.portal.api.StudentServicesRequests.*;
import edu.vishvaya.portal.security.AccountPrincipal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StudentServices {
    List<Map<String, Object>> schedules(AccountPrincipal principal);
    void saveExam(UUID id, Exam input, AccountPrincipal principal);
    List<Map<String, Object>> roster(UUID id);
    void score(UUID exam, UUID student, Score input, AccountPrincipal principal);
    void discardScore(UUID exam, UUID student, long revision, AccountPrincipal principal);
    void release(UUID exam, long revision, AccountPrincipal principal);
    List<Map<String, Object>> results(UUID student);
    List<Map<String, Object>> backlogOptions(UUID student);
    List<Map<String, Object>> requests(AccountPrincipal principal);
    void submit(UUID id, Submission input, AccountPrincipal principal);
    void review(UUID id, Review input, AccountPrincipal principal);
    List<Map<String, Object>> routes();
    void saveRoute(Kind kind, Route input, AccountPrincipal principal);
}
