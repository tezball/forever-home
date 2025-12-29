package com.example.foreverhome.moderation.web;

import com.example.foreverhome.moderation.client.ForeverHomeApiClient;
import com.example.foreverhome.moderation.domain.ModerationJob;
import com.example.foreverhome.moderation.service.ModerationResultService;
import com.example.foreverhome.moderation.service.ModerationResultService.ModerationStats;
import com.example.foreverhome.moderation.service.PetModerationOrchestrator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class DashboardController {

    private final ModerationResultService resultService;
    private final PetModerationOrchestrator orchestrator;
    private final ForeverHomeApiClient apiClient;

    public DashboardController(ModerationResultService resultService,
                                PetModerationOrchestrator orchestrator,
                                ForeverHomeApiClient apiClient) {
        this.resultService = resultService;
        this.orchestrator = orchestrator;
        this.apiClient = apiClient;
    }

    /**
     * Redirect root to the Review Queue (new primary view).
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/queue";
    }

    /**
     * Dashboard stats view (still accessible directly).
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        ModerationStats stats = resultService.getStatistics();
        List<ModerationJob> recentJobs = resultService.getRecentJobs(5);
        boolean apiAvailable = orchestrator.isApiAvailable();
        String configStatus = orchestrator.getConfigStatus();

        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("stats", stats);
        model.addAttribute("recentJobs", recentJobs);
        model.addAttribute("apiStatus", apiAvailable ? "AVAILABLE" : "UNAVAILABLE");
        model.addAttribute("configStatus", configStatus);

        return "dashboard";
    }

    @PostMapping("/reset")
    public String resetModerationData(RedirectAttributes redirectAttributes) {
        // Reset pet moderation statuses in the main app
        int petsAffected = apiClient.resetModerationStatuses();

        if (petsAffected < 0) {
            redirectAttributes.addFlashAttribute("error",
                "Failed to reset pet moderation statuses. Make sure test mode is enabled in the main app.");
            return "redirect:/";
        }

        // Clear local moderation data (results, flags, jobs)
        resultService.clearAllData();

        redirectAttributes.addFlashAttribute("success",
            String.format("Reset complete! %d pet(s) set to PENDING. All moderation results cleared.", petsAffected));

        return "redirect:/";
    }
}
