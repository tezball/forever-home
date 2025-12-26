package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.service.admin.AdminApiHealthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin/api-health")
public class AdminApiHealthController {

    private final AdminApiHealthService apiHealthService;

    public AdminApiHealthController(AdminApiHealthService apiHealthService) {
        this.apiHealthService = apiHealthService;
    }

    @GetMapping
    public String apiHealth(Model model) {
        var dashboard = apiHealthService.getDashboard();

        model.addAttribute("pageTitle", "API Health");
        model.addAttribute("currentPage", "admin-api-health");
        model.addAttribute("dashboard", dashboard);

        return "admin/api-health";
    }

    @GetMapping("/refresh")
    public String refresh(Model model) {
        return apiHealth(model);
    }

    @GetMapping("/json")
    @ResponseBody
    public AdminApiHealthService.ApiHealthDashboard getHealthJson() {
        return apiHealthService.getDashboard();
    }

    @GetMapping("/check/main-app")
    @ResponseBody
    public AdminApiHealthService.HealthCheck checkMainApp() {
        return apiHealthService.checkMainApp();
    }

    @GetMapping("/check/database")
    @ResponseBody
    public AdminApiHealthService.HealthCheck checkDatabase() {
        return apiHealthService.checkDatabase();
    }

    @GetMapping("/check/s3")
    @ResponseBody
    public AdminApiHealthService.HealthCheck checkS3() {
        return apiHealthService.checkS3();
    }

    @GetMapping("/check/ollama")
    @ResponseBody
    public AdminApiHealthService.HealthCheck checkOllama() {
        return apiHealthService.checkOllama();
    }
}
