package edu.vishvaya.portal.api;

import edu.vishvaya.portal.service.EnquiryService;
import edu.vishvaya.portal.service.PublicContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Public website", description = "Public pages, college data, announcements, and admission enquiries")
public class PublicSiteController {

    private final PublicContentService contentService;
    private final EnquiryService enquiryService;

    public PublicSiteController(PublicContentService contentService, EnquiryService enquiryService) {
        this.contentService = contentService;
        this.enquiryService = enquiryService;
    }

    @GetMapping("/announcements")
    @Operation(summary = "List currently published public announcements")
    public List<AnnouncementResponse> announcements() {
        return contentService.announcements();
    }

    @GetMapping("/overview")
    @Operation(summary = "Get public college profile and headline metrics")
    public CollegeOverviewResponse overview() {
        return contentService.overview();
    }

    @GetMapping("/departments")
    @Operation(summary = "List BTech departments shown on the public homepage")
    public List<DepartmentResponse> departments() {
        return contentService.departments();
    }

    @PostMapping("/enquiries")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit an admission enquiry")
    public EnquiryReceipt submitEnquiry(@Valid @RequestBody EnquiryRequest request) {
        return enquiryService.submit(request);
    }
}
