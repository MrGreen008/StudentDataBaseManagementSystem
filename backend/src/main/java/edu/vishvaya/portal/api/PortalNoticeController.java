package edu.vishvaya.portal.api;

import edu.vishvaya.portal.security.AccountPrincipal;
import edu.vishvaya.portal.service.PortalNoticeService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.*;

@RestController
public class PortalNoticeController {
    private final PortalNoticeService notices;
    public PortalNoticeController(PortalNoticeService notices) { this.notices=notices; }
    @GetMapping("/api/portal/notices")
    public List<Map<String,Object>> inbox(@AuthenticationPrincipal AccountPrincipal principal) {
        return notices.inbox(principal.id());
    }
    @GetMapping("/api/staff/portal-notices")
    public List<Map<String,Object>> managed(@AuthenticationPrincipal AccountPrincipal principal) {
        return notices.managed(principal.id());
    }
    @PutMapping("/api/staff/portal-notices/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable UUID id, @Valid @RequestBody PortalNoticeRequest request,
                     @AuthenticationPrincipal AccountPrincipal principal) {
        notices.save(id,request,principal.id());
    }
}
