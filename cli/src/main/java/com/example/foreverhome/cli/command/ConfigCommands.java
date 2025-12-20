package com.example.foreverhome.cli.command;

import com.example.foreverhome.cli.config.CliProperties;
import com.example.foreverhome.cli.service.ConfigManager;
import com.example.foreverhome.cli.service.ProcessExecutor;
import com.example.foreverhome.cli.ui.TerminalOutput;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Configuration management commands.
 */
@Component
@Command(command = "config", group = "Configuration", description = "Configuration management")
public class ConfigCommands {

    private final ConfigManager config;
    private final CliProperties properties;
    private final ProcessExecutor executor;
    private final TerminalOutput output;

    public ConfigCommands(ConfigManager config, CliProperties properties,
                          ProcessExecutor executor, TerminalOutput output) {
        this.config = config;
        this.properties = properties;
        this.executor = executor;
        this.output = output;
    }

    @Command(command = "show", description = "Display current configuration")
    public void show() {
        output.header("Configuration");
        output.println();

        printMap(config.getAll(), 0);

        output.println();
        output.println(output.dim("Config file: " + config.getConfigFilePath()));
    }

    @Command(command = "set", description = "Set a configuration value")
    public void set(
            @Option(longNames = "key", shortNames = 'k',
                    description = "Config key (e.g., aws.region)")
            String key,
            @Option(longNames = "value", shortNames = 'v',
                    description = "Value to set")
            String value) {

        if (key == null || key.isBlank()) {
            output.error("Key is required");
            output.info("Example: fh config set --key aws.region --value us-west-2");
            return;
        }

        if (value == null) {
            output.error("Value is required");
            return;
        }

        // Try to parse as appropriate type
        Object parsedValue = parseValue(value);
        config.set(key, parsedValue);
        output.success("Set %s = %s", key, parsedValue);
    }

    @Command(command = "get", description = "Get a configuration value")
    public void get(
            @Option(longNames = "key", shortNames = 'k',
                    description = "Config key")
            String key) {

        if (key == null || key.isBlank()) {
            output.error("Key is required");
            return;
        }

        Object value = config.get(key);
        if (value == null) {
            output.warning("Key not found: %s", key);
        } else if (value instanceof Map) {
            output.println(key + ":");
            printMap((Map<String, Object>) value, 1);
        } else {
            output.println(key + " = " + value);
        }
    }

    @Command(command = "unset", description = "Remove a configuration value")
    public void unset(
            @Option(longNames = "key", shortNames = 'k',
                    description = "Config key to remove")
            String key) {

        if (key == null || key.isBlank()) {
            output.error("Key is required");
            return;
        }

        if (config.has(key)) {
            config.remove(key);
            output.success("Removed: %s", key);
        } else {
            output.warning("Key not found: %s", key);
        }
    }

    @Command(command = "reset", description = "Reset to default configuration")
    public void reset() {
        config.reset();
        output.success("Configuration reset to defaults");
    }

    @Command(command = "edit", description = "Open configuration file in editor")
    public void edit() {
        String editor = config.getString("docs.editor", "nano");
        String configPath = config.getConfigFilePath().toString();

        output.info("Opening %s in %s...", configPath, editor);
        executor.run(editor, configPath);
    }

    @Command(command = "env", description = "Show environment status")
    public void env() {
        output.header("Environment Status");
        output.println();

        // Project info
        output.println(output.bold("Project"));
        output.println("  Root:        " + executor.getProjectRoot());
        output.println("  CLI Version: " + properties.version());
        output.println();

        // Java info
        output.println(output.bold("Java"));
        output.println("  Version:     " + System.getProperty("java.version"));
        output.println("  Vendor:      " + System.getProperty("java.vendor"));
        String vmName = System.getProperty("java.vm.name");
        output.println("  VM:          " + vmName);
        output.println("  Native:      " + (vmName.contains("Substrate") ? output.green("Yes") : "No"));
        output.println();

        // AWS info
        output.println(output.bold("AWS Configuration"));
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String region = System.getenv().getOrDefault("AWS_REGION",
            config.getString("aws.region", "us-east-1"));
        output.println("  Region:      " + region);
        output.println("  Access Key:  " + (accessKey != null ? maskKey(accessKey) : output.dim("(not set)")));
        output.println("  S3 Bucket:   " + config.getString("aws.s3-bucket", output.dim("(not set)")));
        output.println();

        // Docker info
        output.println(output.bold("Docker"));
        String dockerVersion = executor.capture("docker", "--version").trim();
        if (!dockerVersion.isBlank()) {
            output.println("  Version:     " + dockerVersion.replace("Docker version ", ""));
            output.println("  Running:     " + (isDockerRunning() ? output.green("Yes") : output.red("No")));
        } else {
            output.println("  Status:      " + output.red("Not installed"));
        }
        output.println();

        // Terraform info
        output.println(output.bold("Terraform"));
        String tfVersion = executor.capture("terraform", "version", "-json");
        if (!tfVersion.isBlank()) {
            // Simple extraction
            if (tfVersion.contains("terraform_version")) {
                int start = tfVersion.indexOf("\"terraform_version\":\"") + 21;
                int end = tfVersion.indexOf("\"", start);
                output.println("  Version:     " + tfVersion.substring(start, end));
            } else {
                output.println("  Version:     " + executor.capture("terraform", "-version").split("\n")[0]);
            }
            boolean hasState = executor.fileExists("terraform/terraform.tfstate");
            output.println("  State:       " + (hasState ? output.green("Found") : output.dim("Not initialized")));
        } else {
            output.println("  Status:      " + output.dim("Not installed"));
        }
    }

    @Command(command = "path", description = "Show project paths")
    public void path() {
        output.header("Project Paths");
        output.println();

        output.println("  Project Root:  " + executor.getProjectRoot());
        output.println("  Config File:   " + config.getConfigFilePath());
        output.println("  Frontend:      " + executor.getProjectRoot().resolve("frontend"));
        output.println("  Docs:          " + executor.getProjectRoot().resolve("docs"));
        output.println("  Terraform:     " + executor.getProjectRoot().resolve("terraform"));
        output.println("  Tests:         " + executor.getProjectRoot().resolve("src/test"));
    }

    @SuppressWarnings("unchecked")
    private void printMap(Map<String, Object> map, int indent) {
        String prefix = "  ".repeat(indent);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                output.println(prefix + entry.getKey() + ":");
                printMap((Map<String, Object>) entry.getValue(), indent + 1);
            } else {
                output.println(prefix + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    private Object parseValue(String value) {
        // Try boolean
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;

        // Try integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}

        // Try double
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
        } catch (NumberFormatException ignored) {}

        // Return as string
        return value;
    }

    private String maskKey(String key) {
        if (key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    private boolean isDockerRunning() {
        int result = executor.runQuiet("docker", "info");
        return result == 0;
    }
}
