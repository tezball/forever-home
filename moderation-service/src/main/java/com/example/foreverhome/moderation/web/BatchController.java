package com.example.foreverhome.moderation.web;

import com.example.foreverhome.moderation.domain.ModerationJob;
import com.example.foreverhome.moderation.service.ModerationResultService;
import com.example.foreverhome.moderation.service.PetModerationOrchestrator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/batch")
public class BatchController {

    private final ModerationResultService resultService;
    private final PetModerationOrchestrator orchestrator;

    public BatchController(ModerationResultService resultService,
                            PetModerationOrchestrator orchestrator) {
        this.resultService = resultService;
        this.orchestrator = orchestrator;
    }

    @GetMapping
    public String batchPage(Model model) {
        List<ModerationJob> recentJobs = resultService.getRecentJobs(10);

        model.addAttribute("pageTitle", "Batch Moderation");
        model.addAttribute("currentPage", "batch");
        model.addAttribute("recentJobs", recentJobs);
        model.addAttribute("apiStatus", orchestrator.isApiAvailable() ? "AVAILABLE" : "UNAVAILABLE");

        return "batch";
    }

    @PostMapping("/run")
    public String runBatch(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "false") boolean textOnly,
            @RequestParam(defaultValue = "false") boolean imagesOnly,
            RedirectAttributes redirectAttributes) {

        try {
            ModerationJob job = orchestrator.runBatch(limit, textOnly, imagesOnly, null);
            if (job != null) {
                redirectAttributes.addFlashAttribute("success",
                        String.format("Batch job completed: %d/%d pets processed, %d flagged",
                                job.getProcessedPets(), job.getTotalPets(), job.getFlaggedPets()));
            } else {
                redirectAttributes.addFlashAttribute("info", "No pets available for moderation");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Batch moderation failed: " + e.getMessage());
        }

        return "redirect:/batch";
    }
}
