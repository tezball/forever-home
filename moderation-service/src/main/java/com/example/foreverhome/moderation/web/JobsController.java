package com.example.foreverhome.moderation.web;

import com.example.foreverhome.moderation.client.ForeverHomeApiClient;
import com.example.foreverhome.moderation.client.dto.PetProfileDto;
import com.example.foreverhome.moderation.domain.ContentType;
import com.example.foreverhome.moderation.domain.ModerationJob;
import com.example.foreverhome.moderation.domain.ModerationResult;
import com.example.foreverhome.moderation.service.ModerationResultService;
import com.example.foreverhome.moderation.service.PetModerationOrchestrator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
@RequestMapping("/jobs")
public class JobsController {

    private final ModerationResultService resultService;
    private final PetModerationOrchestrator orchestrator;
    private final ForeverHomeApiClient apiClient;

    public JobsController(ModerationResultService resultService,
                           PetModerationOrchestrator orchestrator,
                           ForeverHomeApiClient apiClient) {
        this.resultService = resultService;
        this.orchestrator = orchestrator;
        this.apiClient = apiClient;
    }

    @GetMapping
    public String jobsPage(@RequestParam(defaultValue = "50") int limit, Model model) {
        List<ModerationJob> jobs = resultService.getRecentJobs(limit);

        model.addAttribute("pageTitle", "Jobs History");
        model.addAttribute("currentPage", "jobs");
        model.addAttribute("jobs", jobs);
        model.addAttribute("limit", limit);
        model.addAttribute("apiStatus", orchestrator.isApiAvailable() ? "AVAILABLE" : "UNAVAILABLE");

        return "jobs";
    }

    @GetMapping("/{id}")
    public String jobDetailsPage(@PathVariable UUID id, Model model) {
        Optional<ModerationJob> jobOpt = resultService.getJob(id);
        if (jobOpt.isEmpty()) {
            return "redirect:/jobs?error=Job+not+found";
        }

        ModerationJob job = jobOpt.get();
        List<ModerationResult> results = resultService.getResultsForJob(id);

        // Build view models with actual content
        Map<UUID, PetProfileDto> petCache = new HashMap<>();
        List<ResultView> resultViews = new ArrayList<>();

        for (ModerationResult result : results) {
            // Get pet profile (cached)
            PetProfileDto pet = petCache.computeIfAbsent(result.getPetId(),
                    petId -> apiClient.getPet(petId).orElse(null));

            String petName = pet != null ? pet.name() : "Unknown Pet";
            String content = "";
            String imageUrl = null;

            if (result.getContentType() == ContentType.TEXT) {
                // Get the actual text content based on field name
                if (pet != null) {
                    content = switch (result.getContentIdentifier()) {
                        case "name" -> pet.name();
                        case "description" -> pet.description();
                        case "healthNotes" -> pet.healthNotes() != null ? pet.healthNotes() : "";
                        default -> "";
                    };
                }
            } else if (result.getContentType() == ContentType.IMAGE) {
                // contentIdentifier is the image URL
                imageUrl = result.getContentIdentifier();
            }

            resultViews.add(new ResultView(result, petName, content, imageUrl));
        }

        model.addAttribute("pageTitle", "Job Details");
        model.addAttribute("currentPage", "jobs");
        model.addAttribute("job", job);
        model.addAttribute("results", resultViews);
        model.addAttribute("apiStatus", orchestrator.isApiAvailable() ? "AVAILABLE" : "UNAVAILABLE");

        return "job-details";
    }

    /**
     * View model for displaying a moderation result with its content.
     */
    public record ResultView(
            ModerationResult result,
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
    }
}
