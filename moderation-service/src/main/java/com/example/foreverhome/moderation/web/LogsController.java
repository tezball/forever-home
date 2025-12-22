package com.example.foreverhome.moderation.web;

import com.example.foreverhome.moderation.domain.AiInteractionLog;
import com.example.foreverhome.moderation.domain.ContentType;
import com.example.foreverhome.moderation.service.AiInteractionLogService;
import com.example.foreverhome.moderation.service.PetModerationOrchestrator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/logs")
public class LogsController {

    private final AiInteractionLogService logService;
    private final PetModerationOrchestrator orchestrator;

    public LogsController(AiInteractionLogService logService, PetModerationOrchestrator orchestrator) {
        this.logService = logService;
        this.orchestrator = orchestrator;
    }

    @GetMapping
    public String logsPage(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String type,
            Model model) {

        List<AiInteractionLog> logs;
        if (type != null && !type.isEmpty()) {
            try {
                ContentType contentType = ContentType.valueOf(type.toUpperCase());
                logs = logService.getLogsByContentType(contentType, limit);
            } catch (IllegalArgumentException e) {
                logs = logService.getRecentLogs(limit);
            }
        } else {
            logs = logService.getRecentLogs(limit);
        }

        model.addAttribute("pageTitle", "AI Interaction Logs");
        model.addAttribute("currentPage", "logs");
        model.addAttribute("logs", logs);
        model.addAttribute("stats", logService.getStats());
        model.addAttribute("limit", limit);
        model.addAttribute("selectedType", type);
        model.addAttribute("apiStatus", orchestrator.isApiAvailable() ? "AVAILABLE" : "UNAVAILABLE");

        return "logs";
    }

    @GetMapping("/{id}")
    public String logDetailPage(@PathVariable UUID id, Model model) {
        AiInteractionLog log = logService.getLog(id);
        if (log == null) {
            return "redirect:/logs?error=Log+not+found";
        }

        model.addAttribute("pageTitle", "Log Details");
        model.addAttribute("currentPage", "logs");
        model.addAttribute("log", log);
        model.addAttribute("apiStatus", orchestrator.isApiAvailable() ? "AVAILABLE" : "UNAVAILABLE");

        return "log-details";
    }

    @PostMapping("/clear")
    public String clearLogs() {
        logService.clearLogs();
        return "redirect:/logs?success=Logs+cleared";
    }
}
