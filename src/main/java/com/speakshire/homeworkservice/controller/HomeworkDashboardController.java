package com.speakshire.homeworkservice.controller;

import com.speakshire.homeworkservice.dto.HomeworkDashboardSummary;
import com.speakshire.homeworkservice.service.HomeworkDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/homework/dashboard")
@RequiredArgsConstructor
public class HomeworkDashboardController {

    private final HomeworkDashboardService homeworkDashboardService;

    @GetMapping("/summary")
    public HomeworkDashboardSummary getSummary(@RequestParam UUID userId,
                                               @RequestParam String role,
                                               @RequestParam(defaultValue = "3") Integer limit) {
        return homeworkDashboardService.getSummary(userId, role, limit);
    }
}
