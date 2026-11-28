package edu.vishvaya.portal.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PortalAuthenticationProvider extends DaoAuthenticationProvider {
    public PortalAuthenticationProvider(UserDetailsService users, PasswordEncoder passwords) {
        super(users);
        setPasswordEncoder(passwords);
    }
    @Override
    protected void additionalAuthenticationChecks(UserDetails user, UsernamePasswordAuthenticationToken authentication) {
        super.additionalAuthenticationChecks(user, authentication);
        if (!(authentication.getDetails() instanceof PortalLoginDetails details)
                || !(user instanceof AccountPrincipal principal)
                || !principal.role().equals(details.role())) {
            throw new BadCredentialsException("Check your credentials and selected portal.");
        }
    }
}
