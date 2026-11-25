package edu.vishvaya.portal.service.impl;

import edu.vishvaya.portal.api.AnnouncementResponse;
import edu.vishvaya.portal.api.CollegeOverviewResponse;
import edu.vishvaya.portal.api.DepartmentResponse;
import edu.vishvaya.portal.persistence.PublicContentRepository;
import edu.vishvaya.portal.service.PublicContentService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PublicContentServiceImpl implements PublicContentService {

    private final PublicContentRepository repository;

    public PublicContentServiceImpl(PublicContentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Cacheable("public-announcements")
    public List<AnnouncementResponse> announcements() {
        return repository.findPublishedAnnouncements();
    }

    @Override
    @Cacheable("college-overview")
    public CollegeOverviewResponse overview() {
        return repository.findCollegeOverview();
    }

    @Override
    @Cacheable("public-departments")
    public List<DepartmentResponse> departments() {
        return repository.findDepartments();
    }
}
