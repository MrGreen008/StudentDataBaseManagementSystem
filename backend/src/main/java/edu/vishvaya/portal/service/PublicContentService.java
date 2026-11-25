package edu.vishvaya.portal.service;

import edu.vishvaya.portal.api.AnnouncementResponse;
import edu.vishvaya.portal.api.CollegeOverviewResponse;
import edu.vishvaya.portal.api.DepartmentResponse;

import java.util.List;

public interface PublicContentService {

    List<AnnouncementResponse> announcements();

    CollegeOverviewResponse overview();

    List<DepartmentResponse> departments();
}
