package edu.vishvaya.portal.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AccountRequests {
    private AccountRequests() {}

    public record Create(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9][a-zA-Z0-9._@+-]{2,99}") String login,
            @NotBlank @Size(max = 120) String displayName,
            @NotBlank String temporaryPassword,
            @NotBlank @Pattern(regexp = "ADMIN|FACULTY|STUDENT") String role) {
        @Override public String toString() { return "Create[redacted]"; }
    }

    public record Permissions(@NotBlank @Pattern(regexp = "ADMIN|FACULTY|STUDENT") String role,
                              @jakarta.validation.constraints.NotNull Boolean enabled,
                              @jakarta.validation.constraints.NotNull Boolean canPublish) {}

    public record Reset(@NotBlank String temporaryPassword) {
        @Override public String toString() { return "Reset[redacted]"; }
    }

    public record PasswordChange(@NotBlank String currentPassword, @NotBlank String newPassword) {
        @Override public String toString() { return "PasswordChange[redacted]"; }
    }
}
