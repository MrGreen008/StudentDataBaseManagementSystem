package edu.vishvaya.portal.api;

import edu.vishvaya.portal.security.AccountPrincipal;
import edu.vishvaya.portal.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class IdentityController {
    private final AccountService accounts;
    public IdentityController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) { return token; }

    @GetMapping("/me")
    public AccountView me(@AuthenticationPrincipal AccountPrincipal principal) {
        return AccountView.from(accounts.get(principal.id()));
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@AuthenticationPrincipal AccountPrincipal principal,
                         @Valid @RequestBody AccountRequests.PasswordChange body,
                         HttpServletRequest request) {
        accounts.changePassword(principal.id(), principal.version(), body.currentPassword(), body.newPassword());
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
    }
}
