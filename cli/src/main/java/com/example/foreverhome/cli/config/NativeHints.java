package com.example.foreverhome.cli.config;

import com.example.foreverhome.cli.ui.InteractiveMenu;
import com.example.foreverhome.cli.ui.ProgressReporter;
import com.example.foreverhome.cli.ui.TerminalOutput;
import com.example.foreverhome.cli.service.ProcessExecutor;
import com.example.foreverhome.cli.service.ConfigManager;
import com.example.foreverhome.cli.service.DocsBrowser;
import com.example.foreverhome.cli.service.DocsSearchService;
import com.example.foreverhome.cli.service.PortChecker;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * GraalVM Native Image runtime hints for the CLI.
 */
@Configuration
@ImportRuntimeHints(NativeHints.CliRuntimeHints.class)
public class NativeHints {

    static class CliRuntimeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Register embedded documentation resources
            hints.resources().registerPattern("docs/**/*.md");
            hints.resources().registerPattern("docs/*.md");

            // JLine terminal classes
            registerJLineHints(hints);

            // Lucene classes for search
            registerLuceneHints(hints);

            // SnakeYAML for config
            registerYamlHints(hints);

            // Register CLI components for CGLIB proxies (used with @Lazy)
            registerCliComponentHints(hints);
        }

        private void registerCliComponentHints(RuntimeHints hints) {
            // Register all UI and service components that may be lazily initialized
            Class<?>[] lazyComponents = {
                InteractiveMenu.class,
                InteractiveMenu.MenuOption.class,
                ProgressReporter.class,
                TerminalOutput.class,
                ProcessExecutor.class,
                ConfigManager.class,
                DocsBrowser.class,
                DocsSearchService.class,
                PortChecker.class
            };

            for (Class<?> clazz : lazyComponents) {
                hints.reflection().registerType(clazz,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.DECLARED_FIELDS,
                    MemberCategory.PUBLIC_FIELDS);
            }

            // Register proxy hints for Spring CGLIB proxies
            hints.proxies().registerJdkProxy(
                org.springframework.aop.SpringProxy.class,
                org.springframework.aop.framework.Advised.class,
                org.springframework.core.DecoratingProxy.class
            );
        }

        private void registerJLineHints(RuntimeHints hints) {
            // JLine terminal implementations
            try {
                hints.reflection().registerType(
                    org.jline.terminal.impl.DumbTerminal.class,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);
            } catch (Exception ignored) {}

            try {
                Class<?> posixTerminal = Class.forName("org.jline.terminal.impl.PosixSysTerminal");
                hints.reflection().registerType(posixTerminal,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);
            } catch (Exception ignored) {}

            try {
                Class<?> jansiTerminal = Class.forName("org.jline.terminal.impl.jansi.JansiTerminal");
                hints.reflection().registerType(jansiTerminal,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);
            } catch (Exception ignored) {}
        }

        private void registerLuceneHints(RuntimeHints hints) {
            // Lucene codec
            try {
                Class<?> codec = Class.forName("org.apache.lucene.codecs.lucene99.Lucene99Codec");
                hints.reflection().registerType(codec,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
            } catch (Exception ignored) {}

            try {
                Class<?> codec = Class.forName("org.apache.lucene.codecs.lucene912.Lucene912Codec");
                hints.reflection().registerType(codec,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
            } catch (Exception ignored) {}

            // Standard analyzer
            hints.reflection().registerType(
                org.apache.lucene.analysis.standard.StandardAnalyzer.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

            // Query parser
            hints.reflection().registerType(
                org.apache.lucene.queryparser.classic.QueryParser.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS);
        }

        private void registerYamlHints(RuntimeHints hints) {
            hints.reflection().registerType(
                org.yaml.snakeyaml.Yaml.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS);

            hints.reflection().registerType(
                java.util.LinkedHashMap.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

            hints.reflection().registerType(
                java.util.ArrayList.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        }
    }
}
