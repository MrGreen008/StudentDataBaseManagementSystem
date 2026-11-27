package edu.vishvaya.portal.api;

import edu.vishvaya.portal.security.AccountPrincipal;
import edu.vishvaya.portal.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/staff/announcements")
public class NoticeController {
    private final NoticeService notices;
    public NoticeController(NoticeService notices) { this.notices = notices; }
    @GetMapping public List<Map<String, Object>> list() { return notices.list(); }
    @PutMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable UUID id, @Valid @RequestBody NoticeRequest request,
                     @AuthenticationPrincipal AccountPrincipal principal) {
        notices.save(id, request, principal.id());
    }
}
