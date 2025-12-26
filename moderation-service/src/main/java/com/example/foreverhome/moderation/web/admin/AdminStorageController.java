package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.service.admin.AdminStorageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/storage")
public class AdminStorageController {

    private final AdminStorageService storageService;

    public AdminStorageController(AdminStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping
    public String storage(Model model) {
        var dashboard = storageService.getDashboard();
        var recentUploads = storageService.getRecentUploads(20);

        model.addAttribute("pageTitle", "Storage & Email");
        model.addAttribute("currentPage", "admin-storage");
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("recentUploads", recentUploads);

        return "admin/storage";
    }

    @GetMapping("/refresh")
    public String refresh(Model model) {
        return storage(model);
    }
}
