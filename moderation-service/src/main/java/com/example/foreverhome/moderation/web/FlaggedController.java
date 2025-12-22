package com.example.foreverhome.moderation.web;

import com.example.foreverhome.moderation.domain.ModerationCategory;
import com.example.foreverhome.moderation.domain.ModerationResult;
import com.example.foreverhome.moderation.service.ModerationResultService;
import com.example.foreverhome.moderation.service.PetModerationOrchestrator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/flagged")
public class FlaggedController {

    private final ModerationResultService resultService;
    private final PetModerationOrchestrator orchestrator;

    public FlaggedController(ModerationResultService resultService,
                              PetModerationOrchestrator orchestrator) {
        this.resultService = resultService;
        this.orchestrator = orchestrator;
    }

    @GetMapping
    public String flaggedList(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "50") int limit,
            Model model) {

        List<ModerationResult> results;
        if (category != null && !category.isBlank()) {
            results = resultService.getFlaggedResultsByCategory(category, limit);
        } else {
            results = resultService.getFlaggedResults(limit);
        }

        model.addAttribute("pageTitle", "Flagged Content");
        model.addAttribute("currentPage", "flagged");
        model.addAttribute("results", results);
        model.addAttribute("categories", ModerationCategory.values());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("limit", limit);
        model.addAttribute("apiStatus", orchestrator.isApiAvailable() ? "AVAILABLE" : "UNAVAILABLE");

        return "flagged";
    }

    @PostMapping("/{id}/review")
    public String reviewResult(
            @PathVariable UUID id,
            @RequestParam String action,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {

        try {
            resultService.review(id, action, notes);
            redirectAttributes.addFlashAttribute("success",
                    "Result " + action + "d successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to " + action + " result: " + e.getMessage());
        }

        return "redirect:/flagged";
    }
}
