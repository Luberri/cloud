package com.demo.cloud.controller;

import com.demo.cloud.entity.IssueImage;
import com.demo.cloud.repository.IssueImageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/issues/{issueId}/images")
@CrossOrigin(origins = "*")
public class IssueImageController {

    private final IssueImageRepository issueImageRepository;

    public IssueImageController(IssueImageRepository issueImageRepository) {
        this.issueImageRepository = issueImageRepository;
    }

    @GetMapping
    public ResponseEntity<List<IssueImage>> getImagesByIssue(@PathVariable UUID issueId) {
        List<IssueImage> images = issueImageRepository.findByRoadIssueId(issueId);
        return ResponseEntity.ok(images);
    }
}