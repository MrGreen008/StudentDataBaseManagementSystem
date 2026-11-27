package edu.vishvaya.portal.api;

import edu.vishvaya.portal.persistence.CollegeResource;
import edu.vishvaya.portal.security.AccountPrincipal;
import edu.vishvaya.portal.service.CollegeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.*;

@RestController
public class CollegeController {
    private final CollegeService college;
    public CollegeController(CollegeService college) { this.college = college; }

    @GetMapping("/api/admin/setup")
    public Map<String, List<String>> schema() {
        Map<String, List<String>> schema = new LinkedHashMap<>();
        for (var resource : CollegeResource.values()) schema.put(resource.name(), resource.fields);
        return schema;
    }

    @GetMapping("/api/admin/setup/{resource}")
    public List<Map<String, Object>> list(@PathVariable String resource) { return college.list(resource); }

    @PutMapping("/api/admin/setup/{resource}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable String resource, @RequestBody Map<String, Object> values,
                     @AuthenticationPrincipal AccountPrincipal principal) {
        college.save(resource, values, principal.id());
    }

    @GetMapping("/api/student/dashboard")
    public List<Map<String, Object>> student(@AuthenticationPrincipal AccountPrincipal principal) {
        return college.student(principal.id());
    }

    @GetMapping("/api/faculty/dashboard")
    public List<Map<String, Object>> faculty(@AuthenticationPrincipal AccountPrincipal principal) {
        return college.faculty(principal.id());
    }
}
