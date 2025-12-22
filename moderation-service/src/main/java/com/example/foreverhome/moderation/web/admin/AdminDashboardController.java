package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.service.ModerationResultService;
import com.example.foreverhome.moderation.service.admin.AdminAnalyticsService;
import com.example.foreverhome.moderation.service.admin.AdminFlagService;
import com.example.foreverhome.moderation.service.admin.AdminUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final AdminAnalyticsService analyticsService;
    private final AdminUserService userService;
    private final AdminFlagService flagService;
    private final ModerationResultService moderationResultService;

    public AdminDashboardController(AdminAnalyticsService analyticsService,
                                     AdminUserService userService,
                                     AdminFlagService flagService,
                                     ModerationResultService moderationResultService) {
        this.analyticsService = analyticsService;
        this.userService = userService;
        this.flagService = flagService;
        this.moderationResultService = moderationResultService;
    }

    @GetMapping
    public String dashboard(Model model) {
        var analytics = analyticsService.getAnalytics();
        var pendingApprovals = userService.getPendingApprovals();
        var moderationStats = moderationResultService.getStatistics();

        model.addAttribute("pageTitle", "Admin Dashboard");
        model.addAttribute("currentPage", "admin-dashboard");
        model.addAttribute("analytics", analytics);
        model.addAttribute("pendingApprovals", pendingApprovals);
        model.addAttribute("moderationStats", moderationStats);

        return "admin/dashboard";
    }
}
