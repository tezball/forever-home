package com.example.foreverhome.cli.completion;

import com.example.foreverhome.cli.service.DocsBrowser;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides tab completion for documentation topics.
 */
@Component
public class DocTopicValueProvider implements ValueProvider {

    private final DocsBrowser browser;

    public DocTopicValueProvider(DocsBrowser browser) {
        this.browser = browser;
    }

    @Override
    public List<CompletionProposal> complete(CompletionContext completionContext) {
        String prefix = completionContext.currentWordUpToCursor();
        if (prefix == null) {
            prefix = "";
        }

        String finalPrefix = prefix.toLowerCase();

        return browser.listTopics().stream()
            .filter(topic -> topic.toLowerCase().startsWith(finalPrefix) ||
                             topic.toLowerCase().contains(finalPrefix))
            .map(topic -> new CompletionProposal(topic)
                .description(browser.getTitle(topic)))
            .collect(Collectors.toList());
    }
}
