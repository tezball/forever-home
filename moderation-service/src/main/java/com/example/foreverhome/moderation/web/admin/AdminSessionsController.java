package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.service.admin.AdminSessionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/sessions")
public class AdminSessionsController {

    private final AdminSessionService sessionService;

    public AdminSessionsController(AdminSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public String sessions(Model model) {
        var dashboard = sessionService.getDashboard();

        model.addAttribute("pageTitle", "Session Management");
        model.addAttribute("currentPage", "admin-sessions");
        model.addAttribute("dashboard", dashboard);

        return "admin/sessions";
    }

    @GetMapping("/user/{userId}")
    public String userSessions(@PathVariable UUID userId, Model model) {
        var sessions = sessionService.getSessionsForUser(userId);

        model.addAttribute("pageTitle", "User Sessions");
        model.addAttribute("currentPage", "admin-sessions");
        model.addAttribute("sessions", sessions);
        model.addAttribute("userId", userId);

        return "admin/user-sessions";
    }

    @PostMapping("/{sessionId}/revoke")
    public String revokeSession(@PathVariable UUID sessionId, RedirectAttributes redirectAttributes) {
        try {
            sessionService.revokeSession(sessionId);
            redirectAttributes.addFlashAttribute("success", "Session revoked successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to revoke session: " + e.getMessage());
        }
        return "redirect:/admin/sessions";
    }

    @PostMapping("/user/{userId}/revoke-all")
    public String revokeAllUserSessions(@PathVariable UUID userId, RedirectAttributes redirectAttributes) {
        try {
            int count = sessionService.revokeAllSessionsForUser(userId);
            redirectAttributes.addFlashAttribute("success", count + " sessions revoked");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to revoke sessions: " + e.getMessage());
        }
        return "redirect:/admin/sessions";
    }

    @PostMapping("/revoke-all")
    public String revokeAllSessions(RedirectAttributes redirectAttributes) {
        try {
            int count = sessionService.revokeAllSessions();
            redirectAttributes.addFlashAttribute("success", count + " sessions revoked");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to revoke sessions: " + e.getMessage());
        }
        return "redirect:/admin/sessions";
    }

    @PostMapping("/cleanup")
    public String cleanupExpiredSessions(RedirectAttributes redirectAttributes) {
        try {
            int count = sessionService.cleanupExpiredSessions();
            redirectAttributes.addFlashAttribute("success", count + " expired sessions cleaned up");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to cleanup sessions: " + e.getMessage());
        }
        return "redirect:/admin/sessions";
    }
}
