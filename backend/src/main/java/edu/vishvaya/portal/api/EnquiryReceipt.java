package edu.vishvaya.portal.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EnquiryReceipt(UUID reference, OffsetDateTime receivedAt, String message) {
}
