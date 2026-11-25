package edu.vishvaya.portal.service;

import edu.vishvaya.portal.api.AnnouncementResponse;
import edu.vishvaya.portal.api.CollegeOverviewResponse;
import edu.vishvaya.portal.api.DepartmentResponse;
import edu.vishvaya.portal.persistence.PublicContentRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PublicContentService {

    private final PublicContentRepository repository;

    public PublicContentService(PublicContentRepository repository) {
        this.repository = repository;
    }

    @Cacheable("public-announcements")
    public List<AnnouncementResponse> announcements() {
        return repository.findPublishedAnnouncements();
    }

    @Cacheable("college-overview")
    public CollegeOverviewResponse overview() {
        return repository.findCollegeOverview();
    }

    @Cacheable("public-departments")
    public List<DepartmentResponse> departments() {
        return repository.findDepartments();
    }
}
