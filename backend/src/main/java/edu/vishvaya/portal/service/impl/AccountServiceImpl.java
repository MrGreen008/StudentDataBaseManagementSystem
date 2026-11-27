package edu.vishvaya.portal.service.impl;

import edu.vishvaya.portal.api.AccountRequests;
import edu.vishvaya.portal.api.AccountView;
import edu.vishvaya.portal.persistence.AccountRepository;
import edu.vishvaya.portal.persistence.PortalAccount;
import edu.vishvaya.portal.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accounts;
    private final PasswordEncoder passwords;

    public AccountServiceImpl(AccountRepository accounts, PasswordEncoder passwords) {
        this.accounts = accounts;
        this.passwords = passwords;
    }

    private String hash(String password) {
        if (password == null || password.isBlank() || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use a password of at least 12 characters and at most 72 UTF-8 bytes.");
        }
        return passwords.encode(password);
    }

    @Override
    @Transactional
    public void bootstrap(String login, String password) {
        accounts.lockIdentity();
        if (accounts.count() > 0) return;
        if (login == null || !login.matches("[a-zA-Z0-9][a-zA-Z0-9._@+-]{2,99}")) {
            throw new IllegalStateException("A valid bootstrap login is required.");
        }
        accounts.insert(UUID.randomUUID(), login.toLowerCase(Locale.ROOT), "College administrator",
                hash(password), "ADMIN", "SYSTEM");
    }

    @Override public List<AccountView> list() {
        return accounts.list().stream().map(AccountView::from).toList();
    }

    @Override public PortalAccount get(UUID id) {
        return accounts.byId(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Account unavailable."));
    }

    @Override
    @Transactional
    public AccountView create(AccountRequests.Create request, UUID actor) {
        String login = request.login().toLowerCase(Locale.ROOT);
        accounts.lockIdentity();
        if (accounts.byLogin(login).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Login already in use.");
        }
        UUID id = UUID.randomUUID();
        accounts.insert(id, login, request.displayName().trim(), hash(request.temporaryPassword()),
                request.role(), actor.toString());
        return AccountView.from(get(id));
    }

    @Override
    @Transactional
    public void permissions(UUID id, AccountRequests.Permissions request, UUID actor) {
        accounts.lockIdentity();
        PortalAccount account = get(id);
        if (account.enabled() && account.role().equals("ADMIN")
                && (!request.enabled() || !request.role().equals("ADMIN"))
                && accounts.activeAdministrators() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Keep at least one active administrator.");
        }
        accounts.permissions(id, request.role(), request.enabled(),
                request.role().equals("FACULTY") && request.canPublish(), actor.toString());
    }

    @Override
    @Transactional
    public void reset(UUID id, String password, UUID actor) {
        accounts.lockIdentity();
        get(id);
        accounts.resetPassword(id, hash(password), actor.toString());
    }

    @Override
    @Transactional
    public void changePassword(UUID id, long version, String currentPassword, String newPassword) {
        PortalAccount account = get(id);
        if (currentPassword.getBytes(StandardCharsets.UTF_8).length > 72
                || !passwords.matches(currentPassword, account.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }
        if (currentPassword.equals(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a different password.");
        }
        if (!accounts.changePassword(id, version, hash(newPassword))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account changed. Sign in again.");
        }
    }
}
