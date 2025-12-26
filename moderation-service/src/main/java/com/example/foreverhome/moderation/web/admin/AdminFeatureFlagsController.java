package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.service.admin.AdminFeatureFlagService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/feature-flags")
public class AdminFeatureFlagsController {

    private final AdminFeatureFlagService featureFlagService;

    public AdminFeatureFlagsController(AdminFeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    public String featureFlags(Model model) {
        var flags = featureFlagService.getAllFlags();
        var summary = featureFlagService.getSummary();

        model.addAttribute("pageTitle", "Feature Flags");
        model.addAttribute("currentPage", "admin-feature-flags");
        model.addAttribute("flags", flags);
        model.addAttribute("summary", summary);

        return "admin/feature-flags";
    }

    @PostMapping("/create")
    public String createFlag(@RequestParam String flagKey,
                             @RequestParam String description,
                             @RequestParam(defaultValue = "false") boolean enabled,
                             RedirectAttributes redirectAttributes) {
        try {
            UUID createdBy = null; // TODO: Get from security context
            featureFlagService.createFlag(flagKey, description, enabled, createdBy);
            redirectAttributes.addFlashAttribute("success", "Feature flag created: " + flagKey);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create flag: " + e.getMessage());
        }
        return "redirect:/admin/feature-flags";
    }

    @PostMapping("/{id}/toggle")
    public String toggleFlag(@PathVariable UUID id,
                             @RequestParam boolean enabled,
                             RedirectAttributes redirectAttributes) {
        try {
            UUID updatedBy = null; // TODO: Get from security context
            if (enabled) {
                featureFlagService.enableFlag(id, updatedBy);
            } else {
                featureFlagService.disableFlag(id, updatedBy);
            }
            redirectAttributes.addFlashAttribute("success", "Feature flag updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to toggle flag: " + e.getMessage());
        }
        return "redirect:/admin/feature-flags";
    }

    @PostMapping("/{id}/rollout")
    public String updateRollout(@PathVariable UUID id,
                                @RequestParam int percentage,
                                RedirectAttributes redirectAttributes) {
        try {
            UUID updatedBy = null; // TODO: Get from security context
            featureFlagService.updateRollout(id, percentage, updatedBy);
            redirectAttributes.addFlashAttribute("success", "Rollout percentage updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update rollout: " + e.getMessage());
        }
        return "redirect:/admin/feature-flags";
    }

    @PostMapping("/{id}/roles")
    public String updateTargetRoles(@PathVariable UUID id,
                                    @RequestParam(required = false) String roles,
                                    RedirectAttributes redirectAttributes) {
        try {
            UUID updatedBy = null; // TODO: Get from security context
            List<String> roleList = roles != null && !roles.isBlank()
                    ? Arrays.asList(roles.split(","))
                    : List.of();
            featureFlagService.updateTargetRoles(id, roleList, updatedBy);
            redirectAttributes.addFlashAttribute("success", "Target roles updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update roles: " + e.getMessage());
        }
        return "redirect:/admin/feature-flags";
    }

    @PostMapping("/{id}/delete")
    public String deleteFlag(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            UUID deletedBy = null; // TODO: Get from security context
            featureFlagService.deleteFlag(id, deletedBy);
            redirectAttributes.addFlashAttribute("success", "Feature flag deleted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete flag: " + e.getMessage());
        }
        return "redirect:/admin/feature-flags";
    }

    @GetMapping("/check/{flagKey}")
    @ResponseBody
    public boolean checkFlag(@PathVariable String flagKey) {
        return featureFlagService.isEnabled(flagKey);
    }
}
