package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.service.admin.AdminUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/approvals")
public class AdminApprovalsController {

    private final AdminUserService userService;

    public AdminApprovalsController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listApprovals(Model model) {
        var pendingApprovals = userService.getPendingApprovals();

        model.addAttribute("pageTitle", "Pending Approvals");
        model.addAttribute("currentPage", "admin-approvals");
        model.addAttribute("approvals", pendingApprovals);

        return "admin/approvals";
    }

    @PostMapping("/{id}/approve")
    public String approveUser(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            userService.approveUser(id);
            redirectAttributes.addFlashAttribute("success", "User approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve user: " + e.getMessage());
        }
        return "redirect:/admin/approvals";
    }

    @PostMapping("/{id}/reject")
    public String rejectUser(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes) {
        try {
            userService.rejectUser(id, reason);
            redirectAttributes.addFlashAttribute("success", "User rejected successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reject user: " + e.getMessage());
        }
        return "redirect:/admin/approvals";
    }
}
