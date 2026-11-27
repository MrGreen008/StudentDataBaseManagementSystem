package edu.vishvaya.portal.security;

import edu.vishvaya.portal.persistence.AccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class AccountUserDetailsService implements UserDetailsService {
    private final AccountRepository accounts;
    public AccountUserDetailsService(AccountRepository accounts) { this.accounts = accounts; }

    @Override
    public UserDetails loadUserByUsername(String login) {
        return accounts.byLogin(login.trim().toLowerCase(Locale.ROOT))
                .map(AccountPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Sign-in failed."));
    }
}
