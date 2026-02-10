package com.demo.cloud.controller;

import com.demo.cloud.entity.IssueImage;
import com.demo.cloud.service.IssueImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class PublicImageController {

    @Autowired
    private IssueImageService imageService;

    @GetMapping("/issues/{issueId}/images")
    public List<IssueImage> getImagesByIssue(@PathVariable UUID issueId) {
        return imageService.getImagesByIssueId(issueId);
    }

    @GetMapping("/issues/{issueId}/images/count")
    public ResponseEntity<Long> countImages(@PathVariable UUID issueId) {
        return ResponseEntity.ok(imageService.countImagesByIssueId(issueId));
    }
}
