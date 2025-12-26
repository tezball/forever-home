package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.domain.admin.PlatformSetting;
import com.example.foreverhome.moderation.service.admin.AdminContentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/content")
public class AdminContentController {

    private final AdminContentService contentService;

    public AdminContentController(AdminContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public String content(Model model) {
        var dashboard = contentService.getDashboard();
        var categories = contentService.getAllCategories();

        model.addAttribute("pageTitle", "Content Management");
        model.addAttribute("currentPage", "admin-content");
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("categories", categories);

        return "admin/content";
    }

    @GetMapping("/category/{category}")
    public String categorySettings(@PathVariable String category, Model model) {
        var settings = contentService.getSettingsForCategory(category);

        model.addAttribute("pageTitle", "Settings: " + category);
        model.addAttribute("currentPage", "admin-content");
        model.addAttribute("settings", settings);
        model.addAttribute("category", category);

        return "admin/content-category";
    }

    @PostMapping("/update")
    public String updateSetting(@RequestParam String settingKey,
                                @RequestParam String settingValue,
                                RedirectAttributes redirectAttributes) {
        try {
            UUID updatedBy = null; // TODO: Get from security context
            contentService.updateSetting(settingKey, settingValue, updatedBy);
            redirectAttributes.addFlashAttribute("success", "Setting updated: " + settingKey);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update setting: " + e.getMessage());
        }
        return "redirect:/admin/content";
    }

    @PostMapping("/create")
    public String createSetting(@RequestParam String settingKey,
                                @RequestParam String settingValue,
                                @RequestParam String settingType,
                                @RequestParam String category,
                                @RequestParam(required = false) String description,
                                RedirectAttributes redirectAttributes) {
        try {
            UUID createdBy = null; // TODO: Get from security context
            PlatformSetting.SettingType type = PlatformSetting.SettingType.valueOf(settingType);
            contentService.createSetting(settingKey, settingValue, type, category, description, createdBy);
            redirectAttributes.addFlashAttribute("success", "Setting created: " + settingKey);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create setting: " + e.getMessage());
        }
        return "redirect:/admin/content";
    }

    @PostMapping("/delete")
    public String deleteSetting(@RequestParam String settingKey, RedirectAttributes redirectAttributes) {
        try {
            UUID deletedBy = null; // TODO: Get from security context
            contentService.deleteSetting(settingKey, deletedBy);
            redirectAttributes.addFlashAttribute("success", "Setting deleted: " + settingKey);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete setting: " + e.getMessage());
        }
        return "redirect:/admin/content";
    }

    @GetMapping("/setting/{key}")
    @ResponseBody
    public String getSetting(@PathVariable String key, @RequestParam(defaultValue = "") String defaultValue) {
        return contentService.getSettingValue(key, defaultValue);
    }
}
