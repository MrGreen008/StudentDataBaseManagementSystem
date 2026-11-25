package edu.vishvaya.portal.api;

import java.time.OffsetDateTime;

public record EnquiryReceipt(
        String reference,
        OffsetDateTime receivedAt,
        String receivedAtDisplay,
        String message
) {
}
