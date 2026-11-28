package edu.vishvaya.portal.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public final class AcademicRequests {
    private AcademicRequests() {}

    public enum Mark { PRESENT, ABSENT }
    public enum AvailabilityState { LEAVE, AVAILABLE, UNAVAILABLE, WITHDRAWN }

    public record Enrollment(@NotNull LocalDate startsOn, @NotNull LocalDate endsOn,
                             @PositiveOrZero Long revision) {}

    public record Session(@NotBlank @Size(max=20) String offeringCode, @NotNull UUID instructorId,
                          @NotBlank @Size(max=80) String room, @NotNull OffsetDateTime startsAt,
                          @NotNull OffsetDateTime endsAt, @PositiveOrZero Long revision) {}

    public record Revision(@NotNull @PositiveOrZero Long revision) {}

    public record Register(@NotNull @PositiveOrZero Long revision,
                           @NotEmpty @Size(max=1000) Map<UUID, @NotNull Mark> marks) {}

    public record Report(@NotBlank @Size(max=1000) String explanation,
                         @NotNull @PositiveOrZero Long markRevision) {}

    public record Decision(@NotNull @PositiveOrZero Long revision, @NotNull Boolean accepted,
                           Mark correctedMark, @NotBlank @Size(max=1000) String reason) {}

    public record Availability(@NotNull UUID instructorId, @NotNull LocalDate startsOn,
                               @NotNull LocalDate endsOn, @NotNull AvailabilityState state,
                               @Size(max=500) String notes, @PositiveOrZero Long revision) {}
}
