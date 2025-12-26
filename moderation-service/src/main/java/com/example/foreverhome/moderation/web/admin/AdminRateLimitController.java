package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.service.admin.AdminRateLimitService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/rate-limits")
public class AdminRateLimitController {

    private final AdminRateLimitService rateLimitService;

    public AdminRateLimitController(AdminRateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @GetMapping
    public String rateLimits(Model model) {
        var dashboard = rateLimitService.getDashboard();
        var whitelistedUsers = rateLimitService.getWhitelistedUsers();

        model.addAttribute("pageTitle", "Rate Limits");
        model.addAttribute("currentPage", "admin-rate-limits");
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("whitelistedUsers", whitelistedUsers);

        return "admin/rate-limits";
    }

    @PostMapping("/whitelist/add")
    public String addToWhitelist(@RequestParam String userId, RedirectAttributes redirectAttributes) {
        try {
            rateLimitService.addToWhitelist(userId);
            redirectAttributes.addFlashAttribute("success", "User added to whitelist");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add user: " + e.getMessage());
        }
        return "redirect:/admin/rate-limits";
    }

    @PostMapping("/whitelist/remove")
    public String removeFromWhitelist(@RequestParam String userId, RedirectAttributes redirectAttributes) {
        try {
            rateLimitService.removeFromWhitelist(userId);
            redirectAttributes.addFlashAttribute("success", "User removed from whitelist");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to remove user: " + e.getMessage());
        }
        return "redirect:/admin/rate-limits";
    }
}
