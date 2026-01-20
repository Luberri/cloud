package com.demo.cloud.controller;

import com.demo.cloud.dto.RoadIssuesSummaryResponse;
import com.demo.cloud.service.RoadIssuesSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicSummaryController {

    private final RoadIssuesSummaryService summaryService;

    public PublicSummaryController(RoadIssuesSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/summary")
    public RoadIssuesSummaryResponse getSummary() {
        return summaryService.getSummary();
    }
}
