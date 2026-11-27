package edu.vishvaya.portal.api;

import edu.vishvaya.portal.security.AccountPrincipal;
import edu.vishvaya.portal.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/accounts")
public class AccountController {
    private final AccountService accounts;
    public AccountController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping
    public List<AccountView> list() { return accounts.list(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView create(@Valid @RequestBody AccountRequests.Create request,
                              @AuthenticationPrincipal AccountPrincipal principal) {
        return accounts.create(request, principal.id());
    }

    @PutMapping("/{id}/permissions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void permissions(@PathVariable UUID id, @Valid @RequestBody AccountRequests.Permissions request,
                            @AuthenticationPrincipal AccountPrincipal principal) {
        accounts.permissions(id, request, principal.id());
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@PathVariable UUID id, @Valid @RequestBody AccountRequests.Reset request,
                      @AuthenticationPrincipal AccountPrincipal principal) {
        accounts.reset(id, request.temporaryPassword(), principal.id());
    }
}
