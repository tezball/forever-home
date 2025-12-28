package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.client.ForeverHomeApiClient;
import com.example.foreverhome.moderation.client.dto.PetProfileDto;
import com.example.foreverhome.moderation.domain.ContentType;
import com.example.foreverhome.moderation.domain.ModerationCategory;
import com.example.foreverhome.moderation.domain.ModerationResult;
import com.example.foreverhome.moderation.domain.ModerationStatus;
import com.example.foreverhome.moderation.service.ModerationResultService;
import com.example.foreverhome.moderation.service.PetModerationOrchestrator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/admin/moderation-queue")
public class AdminModerationQueueController {

    private final ModerationResultService resultService;
    private final ForeverHomeApiClient apiClient;
    private final PetModerationOrchestrator orchestrator;

    public AdminModerationQueueController(
            ModerationResultService resultService,
            ForeverHomeApiClient apiClient,
            PetModerationOrchestrator orchestrator) {
        this.resultService = resultService;
        this.apiClient = apiClient;
        this.orchestrator = orchestrator;
    }

    @GetMapping
    public String moderationQueue(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String contentType,
            @RequestParam(defaultValue = "50") int limit,
            Model model) {

        List<ModerationResult> results;
        if (category != null && !category.isBlank()) {
            results = resultService.getFlaggedResultsByCategory(category, limit);
        } else {
            results = resultService.getFlaggedResults(limit);
        }

        // Filter by content type if specified
        if (contentType != null && !contentType.isBlank()) {
            ContentType type = ContentType.valueOf(contentType);
            results = results.stream()
                    .filter(r -> r.getContentType() == type)
                    .toList();
        }

        // Build view models with pet data
        Map<UUID, PetProfileDto> petCache = new HashMap<>();
        List<QueueItemView> queueItems = new ArrayList<>();

        for (ModerationResult result : results) {
            PetProfileDto pet = petCache.computeIfAbsent(result.getPetId(),
                    petId -> apiClient.getPet(petId).orElse(null));

            String petName = pet != null ? pet.name() : "Unknown Pet";
            String content = "";
            String imageUrl = null;

            if (result.getContentType() == ContentType.TEXT) {
                if (pet != null) {
                    content = switch (result.getContentIdentifier()) {
                        case "name" -> pet.name();
                        case "description" -> pet.description();
                        case "healthNotes" -> pet.healthNotes() != null ? pet.healthNotes() : "";
                        default -> "";
                    };
                }
            } else if (result.getContentType() == ContentType.IMAGE) {
                imageUrl = result.getContentIdentifier();
            }

            queueItems.add(new QueueItemView(result, pet, petName, content, imageUrl));
        }

        // Stats
        var stats = resultService.getStatistics();
        long totalFlagged = stats.flaggedCount();
        long textFlagged = results.stream().filter(r -> r.getContentType() == ContentType.TEXT).count();
        long imageFlagged = results.stream().filter(r -> r.getContentType() == ContentType.IMAGE).count();

        model.addAttribute("pageTitle", "Moderation Queue");
        model.addAttribute("currentPage", "admin-moderation-queue");
        model.addAttribute("queueItems", queueItems);
        model.addAttribute("totalFlagged", totalFlagged);
        model.addAttribute("textFlagged", textFlagged);
        model.addAttribute("imageFlagged", imageFlagged);
        model.addAttribute("categories", ModerationCategory.values());
        model.addAttribute("contentTypes", ContentType.values());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedContentType", contentType);
        model.addAttribute("limit", limit);
        model.addAttribute("apiStatus", orchestrator.isApiAvailable() ? "AVAILABLE" : "UNAVAILABLE");

        return "admin/moderation-queue";
    }

    @PostMapping("/review/{resultId}")
    public String reviewResult(
            @PathVariable UUID resultId,
            @RequestParam String action,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String contentType,
            RedirectAttributes redirectAttributes) {

        try {
            resultService.review(resultId, action, notes);
            redirectAttributes.addFlashAttribute("success",
                    "Content " + action + "d successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to " + action + " content: " + e.getMessage());
        }

        String redirect = "redirect:/admin/moderation-queue";
        if (category != null || contentType != null) {
            redirect += "?";
            if (category != null) redirect += "category=" + category + "&";
            if (contentType != null) redirect += "contentType=" + contentType;
        }
        return redirect;
    }

    @PostMapping("/bulk-review")
    public String bulkReview(
            @RequestParam("resultIds") List<UUID> resultIds,
            @RequestParam String action,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {

        int successCount = 0;
        int errorCount = 0;

        for (UUID resultId : resultIds) {
            try {
                resultService.review(resultId, action, notes);
                successCount++;
            } catch (Exception e) {
                errorCount++;
            }
        }

        if (errorCount == 0) {
            redirectAttributes.addFlashAttribute("success",
                    successCount + " item(s) " + action + "d successfully");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    successCount + " item(s) " + action + "d, " + errorCount + " failed");
        }

        return "redirect:/admin/moderation-queue";
    }

    @PostMapping("/rerun/{resultId}")
    public String rerunModeration(
            @PathVariable UUID resultId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String contentType,
            RedirectAttributes redirectAttributes) {

        try {
            var newResult = orchestrator.rerunModeration(resultId);

            if (newResult.isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "Failed to re-run moderation: Result not found or pet data unavailable");
            } else {
                ModerationResult result = newResult.get();
                String statusMsg = result.getStatus() == ModerationStatus.FLAGGED
                        ? "flagged with " + result.getFlags().size() + " issue(s)"
                        : result.getStatus().name().toLowerCase();
                redirectAttributes.addFlashAttribute("success",
                        "Moderation re-run complete: Content is now " + statusMsg);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to re-run moderation: " + e.getMessage());
        }

        // Preserve filter state
        String redirect = "redirect:/admin/moderation-queue";
        if (category != null || contentType != null) {
            redirect += "?";
            if (category != null) redirect += "category=" + category + "&";
            if (contentType != null) redirect += "contentType=" + contentType;
        }
        return redirect;
    }

    /**
     * View model for queue items.
     */
    public record QueueItemView(
            ModerationResult result,
            PetProfileDto pet,
            String petName,
            String textContent,
            String imageUrl
    ) {
        public boolean isText() {
            return result.getContentType() == ContentType.TEXT;
        }

        public boolean isImage() {
            return result.getContentType() == ContentType.IMAGE;
        }

        public String getPetSpecies() {
            return pet != null ? pet.species() : "Unknown";
        }

        public String getPetBreed() {
            return pet != null ? pet.breed() : "Unknown";
        }
    }
}
