package edu.vishvaya.portal.api;

import edu.vishvaya.portal.persistence.PortalAccount;
import java.util.UUID;

public record AccountView(UUID id, String login, String displayName, String role,
                          boolean enabled, boolean passwordChangeRequired, boolean canPublish) {
    public static AccountView from(PortalAccount account) {
        return new AccountView(account.id(), account.login(), account.displayName(), account.role(),
                account.enabled(), account.passwordChangeRequired(), account.canPublish());
    }
}
