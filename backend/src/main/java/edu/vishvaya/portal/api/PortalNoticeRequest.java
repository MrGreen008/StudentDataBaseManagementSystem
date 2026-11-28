package edu.vishvaya.portal.api;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PortalNoticeRequest(@NotBlank @Size(max=180) String title,
        @NotBlank @Size(max=2000) String summary,
        @NotBlank @Pattern(regexp="STUDENT|FACULTY|ADMIN") String audience,
        String cohortCode, UUID accountId, boolean published,
        @NotNull OffsetDateTime publishedAt, OffsetDateTime expiresAt,
        @Pattern(regexp="GENERAL|TIMETABLE|EXAM_DATES|HOLIDAY") String topic) {
    public PortalNoticeRequest(String title, String summary, String audience, String cohortCode, UUID accountId,
                               boolean published, OffsetDateTime publishedAt, OffsetDateTime expiresAt) {
        this(title, summary, audience, cohortCode, accountId, published, publishedAt, expiresAt, null);
    }
}
