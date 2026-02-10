package com.demo.cloud.controller;

import com.demo.cloud.dto.IssueImageResponse;
import com.demo.cloud.service.IssueImageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/public")
public class PublicImageController {

    private final IssueImageService imageService;

    public PublicImageController(IssueImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/road-issues/{issueId}/images")
    public List<IssueImageResponse> getImagesForIssue(@PathVariable UUID issueId) {
        return imageService.getImagesByRoadIssue(issueId);
    }

    @GetMapping("/road-issues/{issueId}/images/count")
    public long countImagesForIssue(@PathVariable UUID issueId) {
        return imageService.countImagesByRoadIssue(issueId);
    }
}
