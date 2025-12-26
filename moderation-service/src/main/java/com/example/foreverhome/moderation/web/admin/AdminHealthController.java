package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.service.admin.AdminHealthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/health")
public class AdminHealthController {

    private final AdminHealthService healthService;

    public AdminHealthController(AdminHealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public String health(Model model) {
        var systemHealth = healthService.getSystemHealth();
        var metricsSummary = healthService.getMetricsSummary();
        var recentErrors = healthService.getRecentErrors(20);

        model.addAttribute("pageTitle", "System Health");
        model.addAttribute("currentPage", "admin-health");
        model.addAttribute("systemHealth", systemHealth);
        model.addAttribute("metricsSummary", metricsSummary);
        model.addAttribute("recentErrors", recentErrors);

        return "admin/health";
    }

    @GetMapping("/refresh")
    public String refresh(Model model) {
        return health(model);
    }
}
