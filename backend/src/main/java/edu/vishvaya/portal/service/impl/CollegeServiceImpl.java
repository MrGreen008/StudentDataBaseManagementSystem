package edu.vishvaya.portal.service.impl;

import edu.vishvaya.portal.persistence.*;
import edu.vishvaya.portal.service.CollegeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;

@Service
public class CollegeServiceImpl implements CollegeService {
    private final CollegeRepository college;
    private final AccountRepository accounts;

    public CollegeServiceImpl(CollegeRepository college, AccountRepository accounts) {
        this.college = college;
        this.accounts = accounts;
    }

    @Override public List<Map<String, Object>> list(String resource) {
        return college.list(CollegeResource.resolve(resource));
    }

    @Override
    @Transactional
    public void save(String name, Map<String, Object> input, UUID actor) {
        CollegeResource resource = CollegeResource.resolve(name);
        if (!input.keySet().equals(new HashSet<>(resource.fields))) {
            throw badRequest("Supply exactly the fields shown in the form.");
        }
        Map<String, Object> values = new HashMap<>(input);
        if (!(values.get("code") instanceof String code) || !code.matches("[a-zA-Z0-9._-]{1,20}")) {
            throw badRequest("Use a code with up to 20 letters, numbers, dots, underscores or hyphens.");
        }
        if (!(values.get("name") instanceof String label) || label.isBlank() || label.length() > 140) {
            throw badRequest("Supply a name of up to 140 characters.");
        }
        for (String field : resource.fields) {
            Object value = values.get(field);
            if (field.equals("active") || field.equals("visible")) {
                if (!(value instanceof Boolean)) throw badRequest("Supply a boolean for " + field);
            } else if (field.equals("display_order")) {
                if (!(value instanceof Integer)) throw badRequest("Supply an integer display order.");
            } else if (!(value instanceof String text) || text.isBlank()) {
                throw badRequest("Supply a value for " + field);
            }
        }
        // Keep academic eligibility checks consistent with concurrent college setup edits.
        accounts.lockIdentity();
        try {
            if (resource == CollegeResource.terms) {
                values.put("starts_on", LocalDate.parse((String) values.get("starts_on")));
                values.put("ends_on", LocalDate.parse((String) values.get("ends_on")));
            }
            if (resource == CollegeResource.students || resource == CollegeResource.faculty) {
                UUID account = UUID.fromString((String) values.get("account_id"));
                String expected = resource == CollegeResource.students ? "STUDENT" : "FACULTY";
                if (!accounts.byId(account).map(row -> row.role().equals(expected)).orElse(false)) {
                    throw badRequest("Select an account with the matching role.");
                }
                values.put("account_id", account);
            }
        } catch (IllegalArgumentException exception) {
            throw badRequest("Check account IDs and dates.");
        }
        college.save(resource, values, actor);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    @Override public List<Map<String, Object>> student(UUID account) { return college.student(account); }
    @Override public List<Map<String, Object>> faculty(UUID account) { return college.faculty(account); }
}
