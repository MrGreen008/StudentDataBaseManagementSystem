package edu.vishvaya.portal.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnquiryRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = "^[+0-9()\\-\\s]{8,20}$", message = "Enter a valid phone number") String phoneNumber,
        @Email @Size(max = 254) String email,
        @Size(max = 120) String programmeOfInterest,
        @NotBlank @Size(max = 1500) String message,
        @AssertTrue(message = "Consent is required to submit an enquiry") boolean privacyConsent
) {
}
