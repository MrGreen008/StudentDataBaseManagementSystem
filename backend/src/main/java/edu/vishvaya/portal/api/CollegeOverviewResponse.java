package edu.vishvaya.portal.api;

import java.io.Serializable;

public record CollegeOverviewResponse(
        String name,
        String city,
        long students,
        long faculty,
        long publications,
        long patentsAndDisclosures,
        long projects
) implements Serializable {
}
