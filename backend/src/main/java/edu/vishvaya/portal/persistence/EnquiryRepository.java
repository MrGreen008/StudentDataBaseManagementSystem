package edu.vishvaya.portal.persistence;

import edu.vishvaya.portal.api.EnquiryRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@Repository
public class EnquiryRepository {

    private static final int REFERENCE_GENERATION_ATTEMPTS = 3;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;

    public EnquiryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SavedEnquiry save(EnquiryRequest request) {
        OffsetDateTime receivedAt = OffsetDateTime.now();
        String sql = """
                INSERT INTO admission_enquiry
                    (id, full_name, phone_number, email, programme_of_interest,
                     message, privacy_consent, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'NEW', ?)
                ON CONFLICT (id) DO NOTHING
                """;

        for (int attempt = 0; attempt < REFERENCE_GENERATION_ATTEMPTS; attempt++) {
            String reference = generateReference();
            int inserted = jdbc.update(sql,
                    reference,
                    request.fullName().trim(),
                    request.phoneNumber().trim(),
                    request.email() == null || request.email().isBlank() ? null : request.email().trim(),
                    request.programmeOfInterest() == null ? null : request.programmeOfInterest().trim(),
                    request.message().trim(),
                    request.privacyConsent(),
                    receivedAt
            );
            if (inserted == 1) {
                return new SavedEnquiry(reference, receivedAt);
            }
        }

        throw new IllegalStateException("Could not generate a unique enquiry reference.");
    }

    private String generateReference() {
        byte[] randomBytes = new byte[8];
        SECURE_RANDOM.nextBytes(randomBytes);
        return HexFormat.of().withUpperCase().formatHex(randomBytes);
    }
}
