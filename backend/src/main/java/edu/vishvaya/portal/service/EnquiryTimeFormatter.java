package edu.vishvaya.portal.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class EnquiryTimeFormatter {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM uuuu, hh:mm:ss a", Locale.ENGLISH);

    private EnquiryTimeFormatter() {
    }

    public static String formatForDisplay(OffsetDateTime timestamp) {
        return timestamp.atZoneSameInstant(DISPLAY_ZONE).format(DISPLAY_FORMAT) + " IST";
    }
}
