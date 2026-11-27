package edu.vishvaya.portal.security;

import edu.vishvaya.portal.persistence.PortalAccount;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class AccountPrincipal implements UserDetails, CredentialsContainer {
    private final UUID id;
    private final String login;
    private String password;
    private final long version;
    private final boolean enabled;
    private final boolean passwordChangeRequired;
    private final Collection<GrantedAuthority> authorities;

    public AccountPrincipal(PortalAccount account) {
        id = account.id();
        login = account.login();
        password = account.passwordHash();
        version = account.securityVersion();
        enabled = account.enabled();
        passwordChangeRequired = account.passwordChangeRequired();
        var grants = new ArrayList<GrantedAuthority>();
        if (!passwordChangeRequired) {
            grants.add(new SimpleGrantedAuthority("ROLE_" + account.role()));
            if (account.role().equals("ADMIN") || account.canPublish()) {
                grants.add(new SimpleGrantedAuthority("PUBLISH"));
            }
        }
        authorities = java.util.List.copyOf(grants);
    }

    public UUID id() { return id; }
    public long version() { return version; }
    public boolean passwordChangeRequired() { return passwordChangeRequired; }
    @Override public String getUsername() { return login; }
    @Override public String getPassword() { return password; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public void eraseCredentials() { password = null; }
}
