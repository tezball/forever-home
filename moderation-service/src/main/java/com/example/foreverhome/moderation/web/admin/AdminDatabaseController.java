package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.service.admin.AdminDatabaseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/database")
public class AdminDatabaseController {

    private final AdminDatabaseService databaseService;

    public AdminDatabaseController(AdminDatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @GetMapping
    public String database(Model model) {
        var dashboard = databaseService.getDashboard();
        var activeQueries = databaseService.getActiveQueries();
        var slowQueryCandidates = databaseService.getSlowQueryCandidates();

        model.addAttribute("pageTitle", "Database Health");
        model.addAttribute("currentPage", "admin-database");
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("activeQueries", activeQueries);
        model.addAttribute("slowQueryCandidates", slowQueryCandidates);

        return "admin/database";
    }

    @GetMapping("/refresh")
    public String refresh(Model model) {
        return database(model);
    }
}
