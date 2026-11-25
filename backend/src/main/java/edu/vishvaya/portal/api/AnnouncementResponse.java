package edu.vishvaya.portal.api;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementResponse(
        UUID id,
        String category,
        String title,
        String summary,
        String actionLabel,
        String actionUrl,
        OffsetDateTime publishedAt
) implements Serializable {
}
