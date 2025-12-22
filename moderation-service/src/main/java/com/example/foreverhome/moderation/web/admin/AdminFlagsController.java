package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.domain.admin.ContentFlag.FlagStatus;
import com.example.foreverhome.moderation.service.admin.AdminFlagService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/flags")
public class AdminFlagsController {

    private final AdminFlagService flagService;

    public AdminFlagsController(AdminFlagService flagService) {
        this.flagService = flagService;
    }

    @GetMapping
    public String listFlags(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        FlagStatus flagStatus = status != null && !status.isBlank() ? FlagStatus.valueOf(status) : null;
        var result = flagService.getFlags(flagStatus, page, size);

        model.addAttribute("pageTitle", "Content Flags");
        model.addAttribute("currentPage", "admin-flags");
        model.addAttribute("flags", result.flags());
        model.addAttribute("page", result.page());
        model.addAttribute("size", result.size());
        model.addAttribute("totalCount", result.totalCount());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("status", status);
        model.addAttribute("statuses", FlagStatus.values());

        return "admin/flags";
    }

    @PostMapping("/{id}/approve")
    public String approveFlag(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        try {
            flagService.approveFlag(id, notes);
            redirectAttributes.addFlashAttribute("success", "Flag approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve flag: " + e.getMessage());
        }
        return "redirect:/admin/flags";
    }

    @PostMapping("/{id}/dismiss")
    public String dismissFlag(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        try {
            flagService.dismissFlag(id, notes);
            redirectAttributes.addFlashAttribute("success", "Flag dismissed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to dismiss flag: " + e.getMessage());
        }
        return "redirect:/admin/flags";
    }
}
