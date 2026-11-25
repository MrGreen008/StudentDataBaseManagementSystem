package edu.vishvaya.portal.persistence;

import java.time.OffsetDateTime;

public record SavedEnquiry(String reference, OffsetDateTime createdAt) {
}
