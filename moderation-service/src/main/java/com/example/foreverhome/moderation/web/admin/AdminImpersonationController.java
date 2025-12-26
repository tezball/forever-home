package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.service.admin.AdminImpersonationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.UUID;

@Controller
@RequestMapping("/admin/impersonation")
public class AdminImpersonationController {

    private final AdminImpersonationService impersonationService;
    private final String mainAppUrl;

    public AdminImpersonationController(AdminImpersonationService impersonationService,
                                         @Value("${main-app.url:http://localhost:8080}") String mainAppUrl) {
        this.impersonationService = impersonationService;
        this.mainAppUrl = mainAppUrl;
    }

    @GetMapping
    public String impersonation(Model model) {
        var dashboard = impersonationService.getDashboard();

        model.addAttribute("pageTitle", "User Impersonation");
        model.addAttribute("currentPage", "admin-impersonation");
        model.addAttribute("dashboard", dashboard);

        return "admin/impersonation";
    }

    @PostMapping("/start")
    public RedirectView startImpersonation(@RequestParam UUID targetUserId,
                                            @RequestParam String reason,
                                            HttpServletRequest request,
                                            RedirectAttributes redirectAttributes) {
        try {
            // TODO: Get actual admin user ID from security context
            UUID adminId = UUID.randomUUID(); // Placeholder
            String ipAddress = request.getRemoteAddr();

            var session = impersonationService.startImpersonation(adminId, targetUserId, ipAddress, reason);

            // Redirect to main app with impersonation token
            String redirectUrl = mainAppUrl + "?impersonate=" + session.getToken();
            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to start impersonation: " + e.getMessage());
            return new RedirectView("/admin/impersonation");
        }
    }

    @PostMapping("/end")
    public String endImpersonation(@RequestParam String token, RedirectAttributes redirectAttributes) {
        try {
            // TODO: Get actual admin user ID from security context
            UUID adminId = UUID.randomUUID(); // Placeholder
            impersonationService.endImpersonation(token, adminId);
            redirectAttributes.addFlashAttribute("success", "Impersonation session ended");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to end impersonation: " + e.getMessage());
        }
        return "redirect:/admin/impersonation";
    }

    @PostMapping("/{id}/end")
    public String endImpersonationById(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            UUID adminId = UUID.randomUUID(); // TODO: Get from security context
            impersonationService.endImpersonationById(id, adminId);
            redirectAttributes.addFlashAttribute("success", "Impersonation session ended");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to end impersonation: " + e.getMessage());
        }
        return "redirect:/admin/impersonation";
    }

    @GetMapping("/history")
    public String history(@RequestParam(defaultValue = "1") int page, Model model) {
        var sessions = impersonationService.getSessionHistory(page, 50);

        model.addAttribute("pageTitle", "Impersonation History");
        model.addAttribute("currentPage", "admin-impersonation");
        model.addAttribute("sessions", sessions);
        model.addAttribute("page", page);

        return "admin/impersonation-history";
    }

    @GetMapping("/validate")
    @ResponseBody
    public boolean validateToken(@RequestParam String token) {
        return impersonationService.validateToken(token).isPresent();
    }
}
