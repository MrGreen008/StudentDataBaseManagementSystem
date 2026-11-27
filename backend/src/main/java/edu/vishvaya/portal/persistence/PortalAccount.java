package edu.vishvaya.portal.persistence;

import java.util.UUID;

public record PortalAccount(UUID id, String login, String displayName, String passwordHash,
                            String role, boolean enabled, boolean passwordChangeRequired,
                            boolean canPublish, long securityVersion) {
    @Override
    public String toString() {
        return "PortalAccount[id=" + id + "]";
    }
}
