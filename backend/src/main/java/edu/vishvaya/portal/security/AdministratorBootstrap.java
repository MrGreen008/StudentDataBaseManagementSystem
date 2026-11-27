package edu.vishvaya.portal.security;

import edu.vishvaya.portal.service.AccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdministratorBootstrap implements ApplicationRunner {
    private final AccountService accounts;
    private final String login;
    private final String password;

    public AdministratorBootstrap(AccountService accounts,
            @Value("${PORTAL_BOOTSTRAP_LOGIN:}") String login,
            @Value("${PORTAL_BOOTSTRAP_PASSWORD:}") String password) {
        this.accounts = accounts;
        this.login = login;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!login.isBlank()) accounts.bootstrap(login, password);
    }
}
