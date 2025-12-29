package com.example.foreverhome.moderation.web;

import com.example.foreverhome.moderation.client.ForeverHomeApiClient;
import com.example.foreverhome.moderation.client.dto.PetProfileDto;
import com.example.foreverhome.moderation.domain.*;
import com.example.foreverhome.moderation.service.ModerationResultService;
import com.example.foreverhome.moderation.service.ModerationResultService.ModerationStats;
import com.example.foreverhome.moderation.service.PetModerationOrchestrator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Unified queue controller for the new moderation dashboard.
 * Replaces separate Dashboard and Flagged controllers with a single review queue.
 */
@Controller
@RequestMapping("/queue")
public class QueueController {

    private final ModerationResultService resultService;
    private final PetModerationOrchestrator orchestrator;
    private final ForeverHomeApiClient apiClient;

    public QueueController(ModerationResultService resultService,
                           PetModerationOrchestrator orchestrator,
                           ForeverHomeApiClient apiClient) {
        this.resultService = resultService;
        this.orchestrator = orchestrator;
        this.apiClient = apiClient;
    }

    /**
     * Main queue view - unified review interface.
     */
    @GetMapping
    public String queue(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String age,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "50") int limit,
            Model model) {

        // Get flagged results with filters
        List<ModerationResult> results = getFilteredResults(type, category, severity, limit);

        // Sort results
        results = sortResults(results, sort);

        // Enrich results with pet data and compute age
        List<QueueItem> queueItems = enrichWithPetData(results);

        // Filter by age if specified
        if (age != null && !age.isBlank()) {
            queueItems = filterByAge(queueItems, age);
        }

        // Get statistics
        ModerationStats stats = resultService.getStatistics();

        // Calculate queue health metrics
        QueueHealth queueHealth = calculateQueueHealth(queueItems);

        model.addAttribute("pageTitle", "Review Queue");
        model.addAttribute("currentPage", "queue");
        model.addAttribute("queueItems", queueItems);
        model.addAttribute("stats", stats);
        model.addAttribute("queueHealth", queueHealth);
        model.addAttribute("categories", ModerationCategory.values());
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedSeverity", severity);
        model.addAttribute("selectedAge", age);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("limit", limit);
        model.addAttribute("apiStatus", orchestrator.isApiAvailable() ? "AVAILABLE" : "UNAVAILABLE");

        return "queue";
    }

    /**
     * HTMX endpoint - returns just the queue items for real-time updates.
     */
    @GetMapping("/items")
    public String queueItems(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String age,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "50") int limit,
            Model model) {

        List<ModerationResult> results = getFilteredResults(type, category, severity, limit);
        results = sortResults(results, sort);
        List<QueueItem> queueItems = enrichWithPetData(results);

        if (age != null && !age.isBlank()) {
            queueItems = filterByAge(queueItems, age);
        }

        model.addAttribute("queueItems", queueItems);
        return "fragments/queue-items :: queueItems";
    }

    /**
     * HTMX endpoint - returns queue count for badge updates.
     */
    @GetMapping("/count")
    @ResponseBody
    public Map<String, Object> queueCount() {
        ModerationStats stats = resultService.getStatistics();
        return Map.of(
            "pending", stats.flaggedCount(),
            "total", stats.totalResults()
        );
    }

    /**
     * HTMX endpoint - returns detail panel for a specific item.
     */
    @GetMapping("/{id}/detail")
    public String itemDetail(@PathVariable UUID id, Model model) {
        // Find the result
        List<ModerationResult> allResults = resultService.getFlaggedResults(1000);
        ModerationResult result = allResults.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (result == null) {
            model.addAttribute("error", "Item not found");
            return "fragments/detail-panel :: detailPanel";
        }

        // Get pet data
        PetProfileDto pet = apiClient.getPet(result.getPetId()).orElse(null);

        // Get all results for this pet
        List<ModerationResult> petResults = resultService.getResultsForPet(result.getPetId());

        QueueItem item = new QueueItem(result, pet, calculateAge(result.getCreatedAt()));

        model.addAttribute("item", item);
        model.addAttribute("pet", pet);
        model.addAttribute("petResults", petResults);
        return "fragments/detail-panel :: detailPanel";
    }

    /**
     * Quick approve action.
     */
    @PostMapping("/{id}/approve")
    public String approve(
            @PathVariable UUID id,
            @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
            RedirectAttributes redirectAttributes) {

        try {
            resultService.review(id, "approve", null);
            if (htmxRequest != null) {
                return "fragments/queue-items :: itemRemoved";
            }
            redirectAttributes.addFlashAttribute("success", "Item approved");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve: " + e.getMessage());
        }

        return "redirect:/queue";
    }

    /**
     * Quick reject action.
     */
    @PostMapping("/{id}/reject")
    public String reject(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes,
            @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
            RedirectAttributes redirectAttributes) {

        try {
            resultService.review(id, "reject", notes);
            if (htmxRequest != null) {
                return "fragments/queue-items :: itemRemoved";
            }
            redirectAttributes.addFlashAttribute("success", "Item rejected");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reject: " + e.getMessage());
        }

        return "redirect:/queue";
    }

    /**
     * Re-run moderation on an item.
     */
    @PostMapping("/{id}/rerun")
    public String rerun(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {

        try {
            orchestrator.rerunModeration(id);
            redirectAttributes.addFlashAttribute("success", "Moderation re-run complete");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to re-run: " + e.getMessage());
        }

        return "redirect:/queue";
    }

    // === Helper Methods ===

    private List<ModerationResult> getFilteredResults(String type, String category, String severity, int limit) {
        List<ModerationResult> results;

        if (category != null && !category.isBlank()) {
            results = resultService.getFlaggedResultsByCategory(category, limit);
        } else {
            results = resultService.getFlaggedResults(limit);
        }

        // Filter by content type
        if (type != null && !type.isBlank()) {
            ContentType contentType = ContentType.valueOf(type.toUpperCase());
            results = results.stream()
                    .filter(r -> r.getContentType() == contentType)
                    .toList();
        }

        // Filter by severity (based on flags)
        if (severity != null && !severity.isBlank()) {
            results = results.stream()
                    .filter(r -> hasMatchingSeverity(r, severity))
                    .toList();
        }

        return new ArrayList<>(results);
    }

    private boolean hasMatchingSeverity(ModerationResult result, String severity) {
        if (result.getFlags() == null || result.getFlags().isEmpty()) {
            return false;
        }
        return result.getFlags().stream()
                .anyMatch(flag -> getSeverityFromCategory(flag.getCategory()).equalsIgnoreCase(severity));
    }

    private String getSeverityFromCategory(ModerationCategory category) {
        return switch (category) {
            case SCAM, HARMFUL, NSFW -> "HIGH";
            case INAPPROPRIATE, MISLEADING -> "MEDIUM";
            case SPAM, NOT_PET, UNCLEAR, OTHER -> "LOW";
        };
    }

    private List<ModerationResult> sortResults(List<ModerationResult> results, String sort) {
        List<ModerationResult> sorted = new ArrayList<>(results);
        switch (sort) {
            case "oldest" -> sorted.sort(Comparator.comparing(ModerationResult::getCreatedAt));
            case "confidence_low" -> sorted.sort(Comparator.comparing(
                    r -> r.getConfidenceScore() != null ? r.getConfidenceScore() : 1.0));
            case "confidence_high" -> sorted.sort(Comparator.comparing(
                    (ModerationResult r) -> r.getConfidenceScore() != null ? r.getConfidenceScore() : 0.0).reversed());
            default -> sorted.sort(Comparator.comparing(ModerationResult::getCreatedAt).reversed()); // newest
        }
        return sorted;
    }

    private List<QueueItem> enrichWithPetData(List<ModerationResult> results) {
        // Cache pet lookups to avoid redundant API calls
        Map<UUID, PetProfileDto> petCache = new HashMap<>();

        return results.stream()
                .map(result -> {
                    PetProfileDto pet = petCache.computeIfAbsent(
                            result.getPetId(),
                            id -> apiClient.getPet(id).orElse(null)
                    );
                    String ageLabel = calculateAge(result.getCreatedAt());
                    return new QueueItem(result, pet, ageLabel);
                })
                .toList();
    }

    private String calculateAge(Instant createdAt) {
        if (createdAt == null) return "Unknown";

        Duration duration = Duration.between(createdAt, Instant.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutes();

        if (minutes < 60) {
            return minutes + "m ago";
        } else if (hours < 24) {
            return hours + "h ago";
        } else {
            long days = duration.toDays();
            return days + "d ago";
        }
    }

    private List<QueueItem> filterByAge(List<QueueItem> items, String age) {
        Instant now = Instant.now();
        return items.stream()
                .filter(item -> {
                    if (item.result().getCreatedAt() == null) return false;
                    Duration duration = Duration.between(item.result().getCreatedAt(), now);
                    long hours = duration.toHours();

                    return switch (age) {
                        case "1h" -> hours < 1;
                        case "4h" -> hours >= 1 && hours < 4;
                        case "24h" -> hours >= 4 && hours < 24;
                        case "old" -> hours >= 24;
                        default -> true;
                    };
                })
                .toList();
    }

    private QueueHealth calculateQueueHealth(List<QueueItem> items) {
        int total = items.size();
        int under1h = 0, under4h = 0, under24h = 0, over24h = 0;

        Instant now = Instant.now();
        for (QueueItem item : items) {
            if (item.result().getCreatedAt() == null) continue;
            long hours = Duration.between(item.result().getCreatedAt(), now).toHours();

            if (hours < 1) under1h++;
            else if (hours < 4) under4h++;
            else if (hours < 24) under24h++;
            else over24h++;
        }

        return new QueueHealth(total, under1h, under4h, under24h, over24h);
    }

    // === Data Transfer Objects ===

    /**
     * Queue item combining moderation result with pet data and computed age.
     */
    public record QueueItem(
            ModerationResult result,
            PetProfileDto pet,
            String ageLabel
    ) {
        public String petName() {
            return pet != null ? pet.name() : "Unknown Pet";
        }

        public String petSpecies() {
            return pet != null ? pet.species() : "";
        }

        public String primaryImageUrl() {
            if (pet != null && pet.imageUrls() != null && !pet.imageUrls().isEmpty()) {
                return pet.imageUrls().get(0);
            }
            return null;
        }

        public String contentPreview() {
            if (result.getContentType() == ContentType.TEXT) {
                // For text, show the actual content if we have it
                if (pet != null) {
                    return switch (result.getContentIdentifier()) {
                        case "description" -> truncate(pet.description(), 100);
                        case "name" -> pet.name();
                        case "healthNotes" -> truncate(pet.healthNotes(), 100);
                        default -> result.getContentIdentifier();
                    };
                }
                return result.getContentIdentifier();
            } else {
                // For images, return the image URL
                return result.getContentIdentifier();
            }
        }

        public String highestSeverity() {
            if (result.getFlags() == null || result.getFlags().isEmpty()) {
                return "LOW";
            }
            boolean hasHigh = result.getFlags().stream()
                    .anyMatch(f -> isHighSeverity(f.getCategory()));
            if (hasHigh) return "HIGH";

            boolean hasMedium = result.getFlags().stream()
                    .anyMatch(f -> isMediumSeverity(f.getCategory()));
            if (hasMedium) return "MEDIUM";

            return "LOW";
        }

        private boolean isHighSeverity(ModerationCategory cat) {
            return cat == ModerationCategory.SCAM ||
                   cat == ModerationCategory.HARMFUL ||
                   cat == ModerationCategory.NSFW;
        }

        private boolean isMediumSeverity(ModerationCategory cat) {
            return cat == ModerationCategory.INAPPROPRIATE ||
                   cat == ModerationCategory.MISLEADING;
        }

        private String truncate(String text, int maxLen) {
            if (text == null) return "";
            if (text.length() <= maxLen) return text;
            return text.substring(0, maxLen - 3) + "...";
        }

        public String ageClass() {
            if (result.getCreatedAt() == null) return "gray";
            long hours = Duration.between(result.getCreatedAt(), Instant.now()).toHours();
            if (hours >= 24) return "red"; // SLA warning
            if (hours >= 4) return "amber";
            return "gray";
        }
    }

    /**
     * Queue health metrics.
     */
    public record QueueHealth(
            int total,
            int under1h,
            int under4h,
            int under24h,
            int over24h
    ) {
        public boolean hasSlaWarning() {
            return over24h > 0;
        }

        public int percentOver24h() {
            if (total == 0) return 0;
            return (int) ((over24h * 100.0) / total);
        }
    }
}
