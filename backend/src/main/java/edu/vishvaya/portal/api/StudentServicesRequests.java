package edu.vishvaya.portal.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class StudentServicesRequests {
    private StudentServicesRequests() {}

    public enum ExamKind { REGULAR, BACKLOG }
    public enum ExamState { DRAFT, SCHEDULED, CANCELLED, RELEASED }
    public enum Kind { BACKLOG, REVALUATION, RESULT_CORRECTION, BONA_FIDE, SCHOLARSHIP, ACCOUNT_SUPPORT }
    public enum RequestState { SUBMITTED, IN_REVIEW, APPROVED, REJECTED, COMPLETED }

    public record Exam(@NotBlank @Size(max=20) String offeringCode, @NotBlank @Size(max=140) String title,
                       @NotNull ExamKind kind, @NotBlank @Size(max=80) String room,
                       @NotNull OffsetDateTime startsAt, @NotNull OffsetDateTime endsAt,
                       @NotNull @Positive @Digits(integer=5,fraction=2) BigDecimal maxScore,
                       @NotNull @Positive @Digits(integer=5,fraction=2) BigDecimal passScore,
                       @NotNull ExamState state, @PositiveOrZero Long revision) {}

    public record Score(@NotNull @PositiveOrZero @Digits(integer=5,fraction=2) BigDecimal score,
                        @NotNull @PositiveOrZero Long examRevision, @PositiveOrZero Long revision) {}

    public record Submission(@NotNull Kind kind, UUID examId, @NotBlank @Size(max=1000) String details) {}

    public record Review(@NotNull RequestState state, @NotBlank @Size(max=1000) String response,
                         @PositiveOrZero @Digits(integer=5,fraction=2) BigDecimal score,
                         @NotNull @PositiveOrZero Long revision) {}

    public record Route(@NotBlank @Size(max=120) String office, @NotBlank @Size(max=1000) String instructions,
                        @Email @Size(max=254) String email, @NotNull @PositiveOrZero Long revision) {}
}
