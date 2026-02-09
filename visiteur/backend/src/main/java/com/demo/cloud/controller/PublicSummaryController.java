package com.demo.cloud.controller;

import com.demo.cloud.dto.RoadIssuesSummaryResponse;
import com.demo.cloud.dto.StatisticsResponse;
import com.demo.cloud.service.RoadIssuesSummaryService;
import com.demo.cloud.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicSummaryController {

    private final RoadIssuesSummaryService summaryService;
    private final StatisticsService statisticsService;

    public PublicSummaryController(RoadIssuesSummaryService summaryService, StatisticsService statisticsService) {
        this.summaryService = summaryService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/summary")
    public RoadIssuesSummaryResponse getSummary() {
        return summaryService.getSummary();
    }

    @GetMapping("/statistics")
    public StatisticsResponse getStatistics() {
        return statisticsService.getStatistics();
    }
}
