package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.service.admin.AdminAuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/audit")
public class AdminAuditController {

    private final AdminAuditService auditService;

    public AdminAuditController(AdminAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public String listAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {

        var logs = auditService.getAuditLogs(page, size);
        long totalCount = auditService.countAuditLogs();
        int totalPages = (int) Math.ceil((double) totalCount / size);

        model.addAttribute("pageTitle", "Audit Logs");
        model.addAttribute("currentPage", "admin-audit");
        model.addAttribute("logs", logs);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("totalPages", totalPages);

        return "admin/audit";
    }

    @PostMapping("/moderation-reset")
    public String resetModerationStatuses(RedirectAttributes redirectAttributes) {
        try {
            int count = auditService.resetModerationStatuses();
            redirectAttributes.addFlashAttribute("success",
                "All pet moderation statuses reset to PENDING. " + count + " pets affected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Failed to reset moderation statuses: " + e.getMessage());
        }
        return "redirect:/admin";
    }
}
