package edu.vishvaya.portal.persistence;

import edu.vishvaya.portal.api.EnquiryRequest;
import edu.vishvaya.portal.api.EnquiryReceipt;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public class EnquiryRepository {

    private final JdbcTemplate jdbc;

    public EnquiryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public EnquiryReceipt save(EnquiryRequest request) {
        UUID reference = UUID.randomUUID();
        OffsetDateTime receivedAt = OffsetDateTime.now();
        jdbc.update("""
                INSERT INTO admission_enquiry
                    (id, full_name, phone_number, email, programme_of_interest,
                     message, privacy_consent, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'NEW', ?)
                """,
                reference,
                request.fullName().trim(),
                request.phoneNumber().trim(),
                request.email() == null || request.email().isBlank() ? null : request.email().trim(),
                request.programmeOfInterest() == null ? null : request.programmeOfInterest().trim(),
                request.message().trim(),
                request.privacyConsent(),
                receivedAt
        );
        return new EnquiryReceipt(reference, receivedAt, "Your enquiry has been received.");
    }
}
