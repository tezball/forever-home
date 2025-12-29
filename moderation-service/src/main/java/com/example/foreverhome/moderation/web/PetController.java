package com.example.foreverhome.moderation.web;

import com.example.foreverhome.moderation.client.ForeverHomeApiClient;
import com.example.foreverhome.moderation.client.dto.PetProfileDto;
import com.example.foreverhome.moderation.domain.ModerationResult;
import com.example.foreverhome.moderation.service.ModerationResultService;
import com.example.foreverhome.moderation.service.PetModerationOrchestrator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/pet")
public class PetController {

    private final ForeverHomeApiClient apiClient;
    private final ModerationResultService resultService;
    private final PetModerationOrchestrator orchestrator;

    public PetController(ForeverHomeApiClient apiClient,
                          ModerationResultService resultService,
                          PetModerationOrchestrator orchestrator) {
        this.apiClient = apiClient;
        this.resultService = resultService;
        this.orchestrator = orchestrator;
    }

    @GetMapping
    public String petPage(@RequestParam(required = false) String id, Model model) {
        model.addAttribute("pageTitle", "Pet Lookup");
        model.addAttribute("currentPage", "pet");
        model.addAttribute("apiStatus", orchestrator.isApiAvailable() ? "AVAILABLE" : "UNAVAILABLE");

        if (id != null && !id.isBlank()) {
            try {
                UUID petId = UUID.fromString(id);
                Optional<PetProfileDto> petOpt = apiClient.getPet(petId);

                if (petOpt.isPresent()) {
                    PetProfileDto pet = petOpt.get();
                    List<ModerationResult> results = resultService.getResultsForPet(petId);

                    model.addAttribute("pet", pet);
                    model.addAttribute("results", results);
                    model.addAttribute("searchedId", id);
                } else {
                    model.addAttribute("error", "Pet not found: " + id);
                    model.addAttribute("searchedId", id);
                }
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", "Invalid pet ID format: " + id);
                model.addAttribute("searchedId", id);
            }
        }

        return "pet";
    }

    @PostMapping("/moderate")
    public String moderatePet(
            @RequestParam String petId,
            @RequestParam(defaultValue = "false") boolean textOnly,
            @RequestParam(defaultValue = "false") boolean imagesOnly,
            RedirectAttributes redirectAttributes) {

        try {
            UUID id = UUID.fromString(petId);
            List<ModerationResult> results = orchestrator.moderatePet(id, textOnly, imagesOnly);

            if (results.isEmpty()) {
                redirectAttributes.addFlashAttribute("info", "No content to moderate for this pet");
            } else {
                long flagged = results.stream()
                        .filter(r -> r.getStatus().name().equals("FLAGGED"))
                        .count();
                redirectAttributes.addFlashAttribute("success",
                        String.format("Moderation complete: %d results, %d flagged", results.size(), flagged));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Moderation failed: " + e.getMessage());
        }

        return "redirect:/pet?id=" + petId;
    }

    /**
     * Manually approve a flagged moderation result.
     */
    @PostMapping("/result/{resultId}/approve")
    public String approveResult(
            @PathVariable UUID resultId,
            @RequestParam String petId,
            RedirectAttributes redirectAttributes) {

        try {
            resultService.review(resultId, "approve", "Manual approval via Pet Lookup");
            redirectAttributes.addFlashAttribute("success", "Item approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve: " + e.getMessage());
        }

        return "redirect:/pet?id=" + petId;
    }

    /**
     * Manually reject a flagged moderation result.
     */
    @PostMapping("/result/{resultId}/reject")
    public String rejectResult(
            @PathVariable UUID resultId,
            @RequestParam String petId,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {

        try {
            resultService.review(resultId, "reject", notes != null ? notes : "Manual rejection via Pet Lookup");
            redirectAttributes.addFlashAttribute("success", "Item rejected successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reject: " + e.getMessage());
        }

        return "redirect:/pet?id=" + petId;
    }
}
