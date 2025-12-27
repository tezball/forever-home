package com.example.foreverhome.cli.service;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Full-text search service for documentation using Lucene.
 */
@Service
public class DocsSearchService {

    private final DocsBrowser browser;
    private final StandardAnalyzer analyzer;
    private ByteBuffersDirectory index;
    private boolean indexBuilt = false;

    public DocsSearchService(DocsBrowser browser) {
        this.browser = browser;
        this.analyzer = new StandardAnalyzer();
    }

    /**
     * Ensure the search index is built (lazy initialization).
     */
    private synchronized void ensureIndexBuilt() {
        if (!indexBuilt) {
            try {
                buildIndex();
            } catch (IOException e) {
                System.err.println("Warning: Failed to build search index: " + e.getMessage());
            }
        }
    }

    /**
     * Build the search index from all documents.
     */
    public void buildIndex() throws IOException {
        index = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        try (IndexWriter writer = new IndexWriter(index, config)) {
            for (String topic : browser.listTopics()) {
                String content = browser.getContent(topic);
                String title = browser.getTitle(topic);

                if (content != null) {
                    Document doc = new Document();
                    doc.add(new StringField("topic", topic, Field.Store.YES));
                    doc.add(new TextField("title", title, Field.Store.YES));
                    doc.add(new TextField("content", content, Field.Store.YES));
                    writer.addDocument(doc);
                }
            }
        }

        indexBuilt = true;
    }

    /**
     * Search for documents matching the query.
     */
    public List<SearchResult> search(String queryText) {
        return search(queryText, 10);
    }

    /**
     * Search for documents matching the query with limit.
     */
    public List<SearchResult> search(String queryText, int maxResults) {
        List<SearchResult> results = new ArrayList<>();

        if (queryText == null || queryText.isBlank()) {
            return results;
        }

        // Lazily build the index on first search
        ensureIndexBuilt();

        if (!indexBuilt) {
            return results;
        }

        try {
            IndexReader reader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(reader);

            // Search in both title and content with title having higher weight
            QueryParser parser = new QueryParser("content", analyzer);
            parser.setDefaultOperator(QueryParser.Operator.OR);

            // Escape special characters but allow wildcards
            String escapedQuery = escapeQuery(queryText);
            Query query = parser.parse(escapedQuery);

            TopDocs topDocs = searcher.search(query, maxResults);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.storedFields().document(scoreDoc.doc);
                String topic = doc.get("topic");
                String title = doc.get("title");
                String content = doc.get("content");
                String snippet = extractSnippet(content, queryText);

                results.add(new SearchResult(topic, title, snippet, scoreDoc.score));
            }

            reader.close();
        } catch (IOException | ParseException e) {
            // Return empty results on error
        }

        return results;
    }

    /**
     * Search and return just topic names.
     */
    public List<String> searchTopics(String queryText) {
        return search(queryText).stream()
            .map(SearchResult::topic)
            .toList();
    }

    /**
     * Extract a snippet around the first match.
     */
    private String extractSnippet(String content, String query) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // Clean markdown
        String cleanContent = content
            .replaceAll("```[\\s\\S]*?```", "") // Remove code blocks
            .replaceAll("^#+\\s+.*$", "") // Remove headers
            .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1") // Links to text
            .replaceAll("[*_`#>|]", "") // Remove markdown chars
            .replaceAll("\\s+", " ") // Normalize whitespace
            .trim();

        // Find first occurrence of any query word
        String lowerContent = cleanContent.toLowerCase();
        String[] queryWords = query.toLowerCase().split("\\s+");

        int bestPos = -1;
        for (String word : queryWords) {
            int pos = lowerContent.indexOf(word);
            if (pos >= 0 && (bestPos < 0 || pos < bestPos)) {
                bestPos = pos;
            }
        }

        if (bestPos < 0) {
            // No match found, return beginning
            return truncate(cleanContent, 150);
        }

        // Extract context around the match
        int start = Math.max(0, bestPos - 50);
        int end = Math.min(cleanContent.length(), bestPos + 100);

        // Adjust to word boundaries
        if (start > 0) {
            int wordStart = cleanContent.lastIndexOf(' ', start);
            if (wordStart > 0) start = wordStart + 1;
        }
        if (end < cleanContent.length()) {
            int wordEnd = cleanContent.indexOf(' ', end);
            if (wordEnd > 0) end = wordEnd;
        }

        String snippet = cleanContent.substring(start, end).trim();

        // Add ellipsis if truncated
        if (start > 0) snippet = "..." + snippet;
        if (end < cleanContent.length()) snippet = snippet + "...";

        return snippet;
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        int end = text.lastIndexOf(' ', maxLength);
        if (end < 0) end = maxLength;
        return text.substring(0, end) + "...";
    }

    private String escapeQuery(String query) {
        // Escape Lucene special characters except wildcards
        StringBuilder sb = new StringBuilder();
        for (char c : query.toCharArray()) {
            if ("+-&|!(){}[]^\"~:\\".indexOf(c) >= 0) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Search result with topic, title, snippet, and relevance score.
     */
    public record SearchResult(
        String topic,
        String title,
        String snippet,
        float score
    ) {}
}
