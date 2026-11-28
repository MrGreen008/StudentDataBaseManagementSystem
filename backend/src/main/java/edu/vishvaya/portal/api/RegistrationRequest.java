package edu.vishvaya.portal.api;

import jakarta.validation.constraints.*;

public record RegistrationRequest(
    @NotBlank @Pattern(regexp="[a-zA-Z0-9][a-zA-Z0-9._+-]{2,99}") String username,
    @NotBlank @Size(max=120) String fullName,
    @NotBlank @Pattern(regexp="[0-9]{10}") String mobileNumber,
    @NotBlank @Email @Size(max=254) String email,
    @NotBlank @Pattern(regexp="STUDENT|FACULTY") String role,
    @NotBlank @Size(max=12) String captcha) {
    @Override public String toString() { return "RegistrationRequest[redacted]"; }
}
