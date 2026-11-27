package edu.vishvaya.portal.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CollegeService {
    List<Map<String, Object>> list(String resource);
    void save(String resource, Map<String, Object> values, UUID actor);
    List<Map<String, Object>> student(UUID account);
    List<Map<String, Object>> faculty(UUID account);
}
