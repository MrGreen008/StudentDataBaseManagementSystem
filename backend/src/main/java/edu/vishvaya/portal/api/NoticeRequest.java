package edu.vishvaya.portal.api;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public record NoticeRequest(@NotBlank @Size(max = 40) String category,
                            @NotBlank @Size(max = 180) String title,
                            @NotBlank @Size(max = 600) String summary,
                            int priority, boolean published,
                            @NotNull OffsetDateTime publishedAt, OffsetDateTime expiresAt) {}
