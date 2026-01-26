package com.demo.cloud.controller;

import com.demo.cloud.dto.RoadIssuePointResponse;
import com.demo.cloud.service.RoadIssuesMapService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public")
public class PublicMapController {

    private final RoadIssuesMapService mapService;

    public PublicMapController(RoadIssuesMapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/road-issues")
    public List<RoadIssuePointResponse> getAllRoadIssues() {
        return mapService.getAllRoadIssuePoints();
    }
}
