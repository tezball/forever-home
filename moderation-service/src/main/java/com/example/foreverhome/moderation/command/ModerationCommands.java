package com.example.foreverhome.moderation.command;

import com.example.foreverhome.moderation.domain.*;
import com.example.foreverhome.moderation.service.ModerationResultService;
import com.example.foreverhome.moderation.service.ModerationResultService.ModerationStats;
import com.example.foreverhome.moderation.service.PetModerationOrchestrator;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Spring Shell commands for content moderation.
 */
@Component
@Command(command = "moderate", group = "Moderation Commands")
public class ModerationCommands {

    private final PetModerationOrchestrator orchestrator;
    private final ModerationResultService resultService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ModerationCommands(PetModerationOrchestrator orchestrator,
                               ModerationResultService resultService) {
        this.orchestrator = orchestrator;
        this.resultService = resultService;
    }

    @Command(command = "pet", description = "Moderate a single pet profile")
    public String moderatePet(
            @Option(longNames = "pet-id", required = true, description = "Pet UUID") String petIdStr,
            @Option(longNames = "text-only", defaultValue = "false", description = "Only moderate text content") boolean textOnly,
            @Option(longNames = "images-only", defaultValue = "false", description = "Only moderate images") boolean imagesOnly) {

        UUID petId;
        try {
            petId = UUID.fromString(petIdStr);
        } catch (IllegalArgumentException e) {
            return "Error: Invalid UUID format: " + petIdStr;
        }

        if (!orchestrator.isApiAvailable()) {
            return "Error: Forever Home API is not available. Is the server running?";
        }

        List<ModerationResult> results = orchestrator.moderatePet(petId, textOnly, imagesOnly);

        if (results.isEmpty()) {
            return "No content to moderate for pet " + petId + " (pet may not exist)";
        }

        return formatResults(results);
    }

    @Command(command = "batch", description = "Run batch moderation on available pets")
    public String moderateBatch(
            @Option(longNames = "limit", defaultValue = "100", description = "Maximum pets to process") int limit,
            @Option(longNames = "text-only", defaultValue = "false", description = "Only moderate text content") boolean textOnly,
            @Option(longNames = "images-only", defaultValue = "false", description = "Only moderate images") boolean imagesOnly) {

        if (!orchestrator.isApiAvailable()) {
            return "Error: Forever Home API is not available. Is the server running?";
        }

        StringBuilder output = new StringBuilder();
        output.append("\n=== Batch Moderation ===\n\n");

        ModerationJob job = orchestrator.runBatch(limit, textOnly, imagesOnly,
                progress -> System.out.println(progress));

        if (job == null) {
            output.append("No pets found to moderate.\n");
            output.append("Make sure there are pets with 'AVAILABLE' status in the Forever Home database.\n");
            return output.toString();
        }

        output.append(String.format("Job ID: %s\n", job.getId()));
        output.append(String.format("Status: %s\n", job.getStatus()));
        output.append(String.format("Total Pets: %d\n", job.getTotalPets()));
        output.append(String.format("Processed: %d\n", job.getProcessedPets()));
        output.append(String.format("Flagged: %d\n", job.getFlaggedPets()));

        if (job.getStatus() == JobStatus.COMPLETED) {
            output.append(String.format("\nFlag Rate: %.1f%%\n", job.getProgressPercent() > 0 ?
                    (job.getFlaggedPets() * 100.0 / job.getProcessedPets()) : 0));
        } else if (job.getStatus() == JobStatus.FAILED) {
            output.append(String.format("\nError: %s\n", job.getErrorMessage()));
        }

        return output.toString();
    }

    @Command(command = "status", description = "Check moderation status for a pet")
    public String checkStatus(
            @Option(longNames = "pet-id", required = true, description = "Pet UUID") String petIdStr) {

        UUID petId;
        try {
            petId = UUID.fromString(petIdStr);
        } catch (IllegalArgumentException e) {
            return "Error: Invalid UUID format: " + petIdStr;
        }

        List<ModerationResult> results = resultService.getResultsForPet(petId);

        if (results.isEmpty()) {
            return "No moderation results found for pet " + petId;
        }

        return formatResults(results);
    }

    @Command(command = "flagged", description = "List flagged content awaiting review")
    public String listFlagged(
            @Option(longNames = "limit", defaultValue = "50", description = "Maximum results to show") int limit,
            @Option(longNames = "category", description = "Filter by category (e.g., INAPPROPRIATE, NOT_PET)") String category) {

        List<ModerationResult> results;
        if (category != null && !category.isBlank()) {
            results = resultService.getFlaggedResultsByCategory(category.toUpperCase(), limit);
        } else {
            results = resultService.getFlaggedResults(limit);
        }

        if (results.isEmpty()) {
            return "No flagged content found" + (category != null ? " for category " + category : "");
        }

        return formatFlaggedResults(results);
    }

    @Command(command = "review", description = "Mark a moderation result as reviewed")
    public String reviewResult(
            @Option(longNames = "result-id", required = true, description = "Result UUID") String resultIdStr,
            @Option(longNames = "action", required = true, description = "Action: approve or reject") String action,
            @Option(longNames = "notes", defaultValue = "", description = "Review notes") String notes) {

        UUID resultId;
        try {
            resultId = UUID.fromString(resultIdStr);
        } catch (IllegalArgumentException e) {
            return "Error: Invalid UUID format: " + resultIdStr;
        }

        if (!action.equalsIgnoreCase("approve") && !action.equalsIgnoreCase("reject")) {
            return "Error: Action must be 'approve' or 'reject'";
        }

        try {
            ModerationResult result = resultService.review(resultId, action, notes);
            return String.format("Result %s marked as %s%s",
                    resultId, result.getStatus(),
                    notes.isBlank() ? "" : " with notes: " + notes);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    @Command(command = "stats", description = "Show moderation statistics")
    public String showStats() {
        ModerationStats stats = resultService.getStatistics();

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Moderation Statistics ===\n\n");

        sb.append("Results by Status:\n");
        sb.append(String.format("  Total:    %,d\n", stats.totalResults()));
        sb.append(String.format("  Pending:  %,d\n", stats.pendingCount()));
        sb.append(String.format("  Approved: %,d (%.1f%%)\n", stats.approvedCount(), stats.approvalRate()));
        sb.append(String.format("  Flagged:  %,d (%.1f%%)\n", stats.flaggedCount(), stats.flagRate()));
        sb.append(String.format("  Rejected: %,d\n", stats.rejectedCount()));

        sb.append("\nResults by Content Type:\n");
        sb.append(String.format("  Text:   %,d\n", stats.textResults()));
        sb.append(String.format("  Image:  %,d\n", stats.imageResults()));

        sb.append("\nPets:\n");
        sb.append(String.format("  Flagged Pets: %,d\n", stats.flaggedPets()));

        sb.append("\nBatch Jobs:\n");
        sb.append(String.format("  Completed:     %,d\n", stats.completedJobs()));
        sb.append(String.format("  Failed:        %,d\n", stats.failedJobs()));
        sb.append(String.format("  Total Processed: %,d\n", stats.totalProcessedPets()));
        sb.append(String.format("  Total Flagged:   %,d\n", stats.totalFlaggedPets()));

        return sb.toString();
    }

    @Command(command = "jobs", description = "Show recent moderation jobs")
    public String showJobs(
            @Option(longNames = "limit", defaultValue = "10", description = "Maximum jobs to show") int limit) {

        List<ModerationJob> jobs = resultService.getRecentJobs(limit);

        if (jobs.isEmpty()) {
            return "No moderation jobs found";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Recent Moderation Jobs ===\n\n");

        for (ModerationJob job : jobs) {
            sb.append(String.format("Job: %s\n", job.getId()));
            sb.append(String.format("  Status:    %s\n", job.getStatus()));
            sb.append(String.format("  Progress:  %d/%d (%.1f%%)\n",
                    job.getProcessedPets(), job.getTotalPets(), job.getProgressPercent()));
            sb.append(String.format("  Flagged:   %d\n", job.getFlaggedPets()));
            if (job.getStartedAt() != null) {
                sb.append(String.format("  Started:   %s\n",
                        DATE_FORMAT.format(job.getStartedAt().atZone(java.time.ZoneId.systemDefault()))));
            }
            if (job.getCompletedAt() != null) {
                sb.append(String.format("  Completed: %s\n",
                        DATE_FORMAT.format(job.getCompletedAt().atZone(java.time.ZoneId.systemDefault()))));
            }
            if (job.getErrorMessage() != null) {
                sb.append(String.format("  Error: %s\n", job.getErrorMessage()));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    @Command(command = "check-api", description = "Check if Forever Home API is available")
    public String checkApi() {
        boolean available = orchestrator.isApiAvailable();
        return available ?
                "Forever Home API is available and responding" :
                "Forever Home API is not available. Make sure the server is running.";
    }

    @Command(command = "config", description = "Show current moderation configuration")
    public String showConfig() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Moderation Configuration ===\n\n");
        sb.append(orchestrator.getConfigStatus().replace(", ", "\n"));
        sb.append("\n\nTo change settings, edit application.properties and restart.\n");
        return sb.toString();
    }

    private String formatResults(List<ModerationResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Moderation Results ===\n\n");

        for (ModerationResult r : results) {
            sb.append(String.format("[%s] %s: %s",
                    r.getContentType(), r.getContentIdentifier(), r.getStatus()));

            if (r.getConfidenceScore() != null) {
                sb.append(String.format(" (confidence: %.2f)", r.getConfidenceScore()));
            }
            sb.append("\n");

            if (r.getFlags() != null && !r.getFlags().isEmpty()) {
                for (FlaggedContent flag : r.getFlags()) {
                    sb.append(String.format("  - [%s] %s: %s\n",
                            flag.getSeverity(), flag.getCategory(), flag.getDescription()));
                    if (flag.getSuggestedAction() != null) {
                        sb.append(String.format("    Suggested action: %s\n", flag.getSuggestedAction()));
                    }
                }
            }

            sb.append(String.format("  Result ID: %s\n\n", r.getId()));
        }

        return sb.toString();
    }

    private String formatFlaggedResults(List<ModerationResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Flagged Content ===\n");
        sb.append(String.format("Total: %d items\n\n", results.size()));

        for (ModerationResult r : results) {
            sb.append(String.format("Pet: %s | %s: %s\n",
                    r.getPetId(), r.getContentType(), r.getContentIdentifier()));
            sb.append(String.format("Result ID: %s\n", r.getId()));

            for (FlaggedContent flag : r.getFlags()) {
                sb.append(String.format("  [%s] %s: %s\n",
                        flag.getSeverity(), flag.getCategory(), flag.getDescription()));
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
