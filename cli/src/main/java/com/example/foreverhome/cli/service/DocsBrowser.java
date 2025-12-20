package com.example.foreverhome.cli.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for browsing embedded documentation files.
 * Loads markdown files from classpath and provides navigation and rendering.
 */
@Service
public class DocsBrowser {

    private final Map<String, DocEntry> documents = new LinkedHashMap<>();
    private final Map<String, List<String>> categories = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        try {
            loadEmbeddedDocs();
        } catch (IOException e) {
            // Log warning but don't fail startup
            System.err.println("Warning: Failed to load embedded docs: " + e.getMessage());
        }
    }

    private void loadEmbeddedDocs() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:docs/**/*.md");

        for (Resource resource : resources) {
            try {
                String path = resource.getURL().getPath();
                String topic = extractTopic(path);
                String category = extractCategory(path);
                String content = readContent(resource);
                String title = extractTitle(content, topic);

                documents.put(topic, new DocEntry(topic, title, category, resource, content));
                categories.computeIfAbsent(category, k -> new ArrayList<>()).add(topic);
            } catch (Exception e) {
                // Skip files that can't be loaded
            }
        }

        // Sort categories and topics
        for (List<String> topics : categories.values()) {
            Collections.sort(topics);
        }
    }

    /**
     * Get list of all topic identifiers.
     */
    public List<String> listTopics() {
        return new ArrayList<>(documents.keySet());
    }

    /**
     * Get topics organized by category.
     */
    public Map<String, List<String>> listByCategory() {
        return new LinkedHashMap<>(categories);
    }

    /**
     * Get the content of a document by topic.
     */
    public String getContent(String topic) {
        DocEntry entry = findEntry(topic);
        return entry != null ? entry.content() : null;
    }

    /**
     * Get the title of a document.
     */
    public String getTitle(String topic) {
        DocEntry entry = findEntry(topic);
        return entry != null ? entry.title() : topic;
    }

    /**
     * Get document metadata.
     */
    public DocEntry getEntry(String topic) {
        return findEntry(topic);
    }

    /**
     * Check if a topic exists.
     */
    public boolean exists(String topic) {
        return findEntry(topic) != null;
    }

    /**
     * Find topics matching a pattern.
     */
    public List<String> findTopics(String pattern) {
        String lowerPattern = pattern.toLowerCase();
        return documents.keySet().stream()
            .filter(topic -> topic.toLowerCase().contains(lowerPattern))
            .toList();
    }

    /**
     * Render markdown content to terminal-friendly format.
     */
    public String renderMarkdown(String markdown) {
        if (markdown == null) return "";

        // Clean Obsidian-specific syntax
        String content = cleanObsidianSyntax(markdown);

        // Render to terminal
        return renderToTerminal(content);
    }

    /**
     * Get a brief summary of a document (first paragraph).
     */
    public String getSummary(String topic) {
        String content = getContent(topic);
        if (content == null) return "";

        // Skip title and get first paragraph
        String[] lines = content.split("\n");
        StringBuilder summary = new StringBuilder();
        boolean inParagraph = false;

        for (String line : lines) {
            if (line.startsWith("#")) continue;
            if (line.isBlank()) {
                if (inParagraph) break;
                continue;
            }
            inParagraph = true;
            if (!summary.isEmpty()) summary.append(" ");
            summary.append(line.trim());
        }

        String result = summary.toString();
        if (result.length() > 200) {
            result = result.substring(0, 197) + "...";
        }
        return result;
    }

    private DocEntry findEntry(String topic) {
        // Direct match
        if (documents.containsKey(topic)) {
            return documents.get(topic);
        }

        // Case-insensitive match
        String lowerTopic = topic.toLowerCase();
        for (Map.Entry<String, DocEntry> entry : documents.entrySet()) {
            if (entry.getKey().toLowerCase().equals(lowerTopic)) {
                return entry.getValue();
            }
        }

        // Partial match (topic is the end of the path)
        for (Map.Entry<String, DocEntry> entry : documents.entrySet()) {
            if (entry.getKey().toLowerCase().endsWith("/" + lowerTopic) ||
                entry.getKey().toLowerCase().endsWith(lowerTopic)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private String cleanObsidianSyntax(String content) {
        // Convert [[link|text]] to text
        content = content.replaceAll("\\[\\[([^|\\]]+)\\|([^\\]]+)\\]\\]", "$2");
        // Convert [[link]] to link
        content = content.replaceAll("\\[\\[([^\\]]+)\\]\\]", "$1");
        // Remove Obsidian tags like #mvp #done
        content = content.replaceAll("\\s#\\w+", "");
        return content;
    }

    private String renderToTerminal(String markdown) {
        StringBuilder sb = new StringBuilder();
        String[] lines = markdown.split("\n");
        boolean inCodeBlock = false;
        boolean inTable = false;

        for (String line : lines) {
            // Code blocks
            if (line.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                if (inCodeBlock) {
                    sb.append("\n\033[2m"); // Dim for code
                } else {
                    sb.append("\033[0m\n");
                }
                continue;
            }

            if (inCodeBlock) {
                sb.append("    ").append(line).append("\n");
                continue;
            }

            // Headers
            if (line.startsWith("#### ")) {
                sb.append("\n\033[1m").append(line.substring(5)).append("\033[0m\n");
            } else if (line.startsWith("### ")) {
                sb.append("\n\033[1m").append(line.substring(4)).append("\033[0m\n");
            } else if (line.startsWith("## ")) {
                sb.append("\n\033[1;4m").append(line.substring(3)).append("\033[0m\n");
            } else if (line.startsWith("# ")) {
                sb.append("\n\033[1;36m").append(line.substring(2)).append("\033[0m\n");
            }
            // Tables
            else if (line.startsWith("|")) {
                if (!inTable) {
                    sb.append("\n");
                    inTable = true;
                }
                sb.append(formatTableRow(line)).append("\n");
            }
            // Horizontal rules
            else if (line.matches("^[-*_]{3,}$")) {
                inTable = false;
                sb.append("\n").append("─".repeat(60)).append("\n");
            }
            // Blockquotes
            else if (line.startsWith("> ")) {
                sb.append("\033[3m│ ").append(line.substring(2)).append("\033[0m\n");
            }
            // Unordered lists
            else if (line.matches("^\\s*[-*+]\\s+.*")) {
                String indent = line.replaceAll("^(\\s*).*", "$1");
                String text = line.replaceAll("^\\s*[-*+]\\s+", "");
                sb.append(indent).append("• ").append(formatInlineStyles(text)).append("\n");
            }
            // Ordered lists
            else if (line.matches("^\\s*\\d+\\.\\s+.*")) {
                sb.append(formatInlineStyles(line)).append("\n");
            }
            // Normal paragraphs
            else {
                if (!line.isBlank()) {
                    inTable = false;
                }
                sb.append(formatInlineStyles(line)).append("\n");
            }
        }

        return sb.toString();
    }

    private String formatTableRow(String row) {
        // Simple table formatting - just clean up pipes
        if (row.matches("^\\|[-:|\\s]+\\|$")) {
            // This is a separator row
            return "├" + "─".repeat(row.length() - 2) + "┤";
        }
        return row;
    }

    private String formatInlineStyles(String text) {
        // Bold: **text** or __text__
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "\033[1m$1\033[0m");
        text = text.replaceAll("__([^_]+)__", "\033[1m$1\033[0m");

        // Italic: *text* or _text_ (but not inside words)
        text = text.replaceAll("(?<![\\w*])\\*([^*]+)\\*(?![\\w*])", "\033[3m$1\033[0m");
        text = text.replaceAll("(?<![\\w_])_([^_]+)_(?![\\w_])", "\033[3m$1\033[0m");

        // Inline code: `code`
        text = text.replaceAll("`([^`]+)`", "\033[2m$1\033[0m");

        // Links: [text](url) - just show text
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "\033[4m$1\033[0m");

        return text;
    }

    private String extractTopic(String path) {
        // Extract the topic from the path
        // e.g., /docs/user-stories/foster.md -> user-stories/foster
        int docsIndex = path.lastIndexOf("/docs/");
        if (docsIndex >= 0) {
            String name = path.substring(docsIndex + 6);
            return name.replace(".md", "");
        }
        // Fallback
        int lastSlash = path.lastIndexOf('/');
        String name = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        return name.replace(".md", "");
    }

    private String extractCategory(String path) {
        String topic = extractTopic(path);
        if (topic.contains("/")) {
            return formatCategoryName(topic.substring(0, topic.indexOf("/")));
        }
        return "Core";
    }

    private String formatCategoryName(String name) {
        // Convert kebab-case to Title Case
        return Arrays.stream(name.split("-"))
            .map(word -> word.isEmpty() ? "" :
                Character.toUpperCase(word.charAt(0)) + word.substring(1))
            .reduce((a, b) -> a + " " + b)
            .orElse(name);
    }

    private String extractTitle(String content, String fallback) {
        // Look for first H1 header
        Pattern pattern = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // Format fallback from topic name
        return formatCategoryName(fallback.replaceAll(".*/", ""));
    }

    private String readContent(Resource resource) throws IOException {
        try (var reader = new InputStreamReader(
                resource.getInputStream(), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
            return sb.toString();
        }
    }

    /**
     * Document entry with metadata.
     */
    public record DocEntry(
        String topic,
        String title,
        String category,
        Resource resource,
        String content
    ) {}
}
