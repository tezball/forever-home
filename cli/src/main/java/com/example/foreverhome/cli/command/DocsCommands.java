package com.example.foreverhome.cli.command;

import com.example.foreverhome.cli.service.DocsBrowser;
import com.example.foreverhome.cli.service.DocsSearchService;
import com.example.foreverhome.cli.service.DocsSearchService.SearchResult;
import com.example.foreverhome.cli.service.ProcessExecutor;
import com.example.foreverhome.cli.ui.InteractiveMenu;
import com.example.foreverhome.cli.ui.InteractiveMenu.MenuOption;
import com.example.foreverhome.cli.ui.TerminalOutput;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Documentation browser commands.
 * Provides interactive navigation and full-text search of project documentation.
 */
@Component
@Command(command = "docs", group = "Documentation", description = "Browse project documentation")
public class DocsCommands {

    private final DocsBrowser browser;
    private final DocsSearchService searchService;
    private final ProcessExecutor executor;
    private final InteractiveMenu menu;
    private final TerminalOutput output;

    public DocsCommands(DocsBrowser browser, DocsSearchService searchService,
                        ProcessExecutor executor, InteractiveMenu menu,
                        TerminalOutput output) {
        this.browser = browser;
        this.searchService = searchService;
        this.executor = executor;
        this.menu = menu;
        this.output = output;
    }

    @Command(command = "", description = "Browse documentation interactively")
    public void browse() {
        output.header("Forever Home Documentation");
        output.println();

        // Show categories
        Map<String, List<String>> categories = browser.listByCategory();

        if (categories.isEmpty()) {
            output.warning("No documentation found");
            output.info("Documentation should be in the docs/ directory");
            return;
        }

        // Build menu options
        List<MenuOption> options = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            String category = entry.getKey();
            int count = entry.getValue().size();
            options.add(new MenuOption(
                category + " (" + count + " docs)",
                category,
                "Browse " + category.toLowerCase() + " documentation"
            ));
        }

        String selected = menu.selectWithDescription("Select a category:", options);
        if (selected == null) {
            return;
        }

        // Show topics in category
        browseCategory(selected);
    }

    private void browseCategory(String category) {
        Map<String, List<String>> categories = browser.listByCategory();
        List<String> topics = categories.get(category);

        if (topics == null || topics.isEmpty()) {
            output.warning("No documents in category: %s", category);
            return;
        }

        output.header(category + " Documentation");
        output.println();

        // Build menu options with summaries
        List<MenuOption> options = new ArrayList<>();
        for (String topic : topics) {
            String title = browser.getTitle(topic);
            String summary = browser.getSummary(topic);
            if (summary.length() > 80) {
                summary = summary.substring(0, 77) + "...";
            }
            options.add(new MenuOption(title, topic, summary));
        }

        String selected = menu.selectWithDescription("Select a document:", options);
        if (selected != null) {
            show(selected);
        }
    }

    @Command(command = "list", description = "List all available documentation")
    public void list() {
        output.header("Available Documentation");
        output.println();

        Map<String, List<String>> categories = browser.listByCategory();

        if (categories.isEmpty()) {
            output.warning("No documentation found");
            return;
        }

        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            output.println(output.bold(entry.getKey()));
            for (String topic : entry.getValue()) {
                String title = browser.getTitle(topic);
                output.println("  " + output.cyan(topic));
                if (!title.equals(topic)) {
                    output.println("    " + output.dim(title));
                }
            }
            output.println();
        }

        output.info("Use 'fh docs show <topic>' to view a document");
        output.info("Use 'fh docs search <query>' to search");
    }

    @Command(command = "show", description = "Display a documentation topic")
    public void show(
            @Option(longNames = "topic", shortNames = 't',
                    description = "Topic to display")
            String topic) {

        if (topic == null || topic.isBlank()) {
            output.error("Please specify a topic");
            output.info("Use 'fh docs list' to see available topics");
            return;
        }

        String content = browser.getContent(topic);
        if (content == null) {
            output.error("Topic not found: %s", topic);

            // Try to find similar topics
            List<String> similar = browser.findTopics(topic);
            if (!similar.isEmpty()) {
                output.println();
                output.info("Did you mean:");
                for (String t : similar.subList(0, Math.min(5, similar.size()))) {
                    output.item(t);
                }
            }

            output.println();
            output.info("Use 'fh docs list' to see all available topics");
            return;
        }

        String title = browser.getTitle(topic);
        output.header(title);

        // Render and display
        String rendered = browser.renderMarkdown(content);
        output.paged(rendered);
    }

    @Command(command = "search", description = "Search documentation")
    public void search(
            @Option(longNames = "query", shortNames = 'q',
                    description = "Search query")
            String query,
            @Option(longNames = "limit", shortNames = 'n',
                    defaultValue = "10",
                    description = "Maximum number of results")
            int limit) {

        if (query == null || query.isBlank()) {
            output.error("Please specify a search query");
            output.info("Example: fh docs search 'pet status'");
            return;
        }

        output.header("Search Results: " + query);
        output.println();

        List<SearchResult> results = searchService.search(query, limit);

        if (results.isEmpty()) {
            output.warning("No results found for: %s", query);
            output.println();
            output.info("Try different keywords or check 'fh docs list'");
            return;
        }

        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            output.println(output.cyan((i + 1) + ". " + result.title()));
            output.println("   " + output.dim("Topic: " + result.topic()));
            output.println("   " + result.snippet());
            output.println("   " + output.dim(String.format("Score: %.2f", result.score())));
            output.println();
        }

        output.info("Use 'fh docs show <topic>' to view a document");
    }

    @Command(command = "open", description = "Open documentation in external viewer")
    public void open(
            @Option(longNames = "topic", shortNames = 't',
                    description = "Topic to open")
            String topic,
            @Option(longNames = "editor", shortNames = 'e',
                    description = "Editor to use (default: system default)")
            String editor) {

        if (topic == null || topic.isBlank()) {
            output.error("Please specify a topic");
            return;
        }

        if (!browser.exists(topic)) {
            output.error("Topic not found: %s", topic);
            return;
        }

        // Get the file path from the docs directory
        String docsPath = executor.getProjectRoot().resolve("docs").toString();
        String filePath;

        if (topic.contains("/")) {
            filePath = docsPath + "/" + topic + ".md";
        } else {
            filePath = docsPath + "/" + topic + ".md";
        }

        // Check if file exists
        if (!executor.fileExists("docs/" + topic + ".md")) {
            // Try to find it
            output.warning("Could not locate file for topic: %s", topic);
            output.info("The document may be embedded. Use 'fh docs show %s' instead.", topic);
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();

        if (editor != null) {
            executor.run(editor, filePath);
        } else if (os.contains("mac")) {
            executor.run("open", filePath);
        } else if (os.contains("linux")) {
            executor.run("xdg-open", filePath);
        } else {
            output.error("Unsupported OS for 'open' command: %s", os);
            output.info("Use 'fh docs show %s' to view in terminal", topic);
        }

        output.success("Opened: %s", filePath);
    }

    @Command(command = "quickstart", description = "Show quick start guide")
    public void quickstart() {
        output.header("Forever Home Quick Start");
        output.println();

        output.println(output.bold("1. Start Development Environment"));
        output.println("   " + output.cyan("fh dev start"));
        output.println("   Starts PostgreSQL, LocalStack, Grafana, and the application");
        output.println();

        output.println(output.bold("2. Open the Application"));
        output.println("   Open http://localhost:8080 in your browser");
        output.println();

        output.println(output.bold("3. Test Accounts"));
        output.println("   Demo mode creates test users automatically:");
        output.println("   - Admin: admin@foreverhome.com");
        output.println("   - Use the 'Demo Login' button to select a role");
        output.println();

        output.println(output.bold("4. Run Tests"));
        output.println("   " + output.cyan("fh test e2e") + "        - Run all E2E tests");
        output.println("   " + output.cyan("fh test unit") + "       - Run unit tests");
        output.println("   " + output.cyan("fh test gatling") + "    - Run load tests");
        output.println();

        output.println(output.bold("5. View Logs & Metrics"));
        output.println("   - Grafana: http://localhost:3000 (admin/admin)");
        output.println("   - Mailpit: http://localhost:8025 (test emails)");
        output.println();

        output.println(output.bold("6. Deploy to AWS"));
        output.println("   " + output.cyan("fh deploy aws"));
        output.println();

        output.println(output.bold("Need Help?"));
        output.println("   " + output.cyan("fh docs list") + "       - List all documentation");
        output.println("   " + output.cyan("fh docs search <q>") + " - Search documentation");
        output.println("   " + output.cyan("fh help") + "            - Show all commands");
    }

    @Command(command = "architecture", description = "Show architecture overview")
    public void architecture() {
        show("domain-model");
    }

    @Command(command = "pet-status", description = "Show pet status lifecycle")
    public void petStatus() {
        show("pet-status");
    }
}
