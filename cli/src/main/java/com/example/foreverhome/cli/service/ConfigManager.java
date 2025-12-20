package com.example.foreverhome.cli.service;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for managing persistent CLI configuration.
 * Configuration is stored in ~/.config/fh/config.yml
 */
@Service
public class ConfigManager {

    private static final Path CONFIG_DIR = Path.of(
        System.getProperty("user.home"), ".config", "fh");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.yml");

    private Map<String, Object> config;

    @PostConstruct
    public void init() {
        loadConfig();
    }

    /**
     * Load configuration from file or create defaults.
     */
    public void loadConfig() {
        if (Files.exists(CONFIG_FILE)) {
            try {
                Yaml yaml = new Yaml();
                String content = Files.readString(CONFIG_FILE);
                config = yaml.load(content);
                if (config == null) {
                    config = new LinkedHashMap<>();
                }
            } catch (IOException e) {
                config = new LinkedHashMap<>();
            }
        } else {
            config = getDefaultConfig();
        }
    }

    /**
     * Save configuration to file.
     */
    public void saveConfig() {
        try {
            Files.createDirectories(CONFIG_DIR);

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            options.setIndent(2);

            Yaml yaml = new Yaml(options);
            String content = yaml.dump(config);
            Files.writeString(CONFIG_FILE, content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config: " + e.getMessage(), e);
        }
    }

    /**
     * Get a configuration value by key.
     * Supports nested keys with dot notation (e.g., "aws.region").
     */
    public Object get(String key) {
        return getNestedValue(config, key);
    }

    /**
     * Get a string configuration value with default.
     */
    public String getString(String key, String defaultValue) {
        Object value = get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Get an integer configuration value with default.
     */
    public int getInt(String key, int defaultValue) {
        Object value = get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Get a boolean configuration value with default.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    /**
     * Set a configuration value.
     * Supports nested keys with dot notation.
     */
    public void set(String key, Object value) {
        setNestedValue(config, key, value);
        saveConfig();
    }

    /**
     * Reset configuration to defaults.
     */
    public void reset() {
        config = getDefaultConfig();
        saveConfig();
    }

    /**
     * Get all configuration as a map.
     */
    public Map<String, Object> getAll() {
        return new LinkedHashMap<>(config);
    }

    /**
     * Check if a configuration key exists.
     */
    public boolean has(String key) {
        return get(key) != null;
    }

    /**
     * Remove a configuration key.
     */
    public void remove(String key) {
        String[] parts = key.split("\\.");
        if (parts.length == 1) {
            config.remove(key);
        } else {
            // Navigate to parent and remove the last key
            Map<String, Object> parent = config;
            for (int i = 0; i < parts.length - 1; i++) {
                Object next = parent.get(parts[i]);
                if (next instanceof Map) {
                    parent = (Map<String, Object>) next;
                } else {
                    return; // Key doesn't exist
                }
            }
            parent.remove(parts[parts.length - 1]);
        }
        saveConfig();
    }

    /**
     * Get the path to the config file.
     */
    public Path getConfigFilePath() {
        return CONFIG_FILE;
    }

    private Map<String, Object> getDefaultConfig() {
        Map<String, Object> defaults = new LinkedHashMap<>();

        // AWS configuration
        Map<String, Object> aws = new LinkedHashMap<>();
        aws.put("region", "us-east-1");
        aws.put("s3-bucket", "forever-home-images-dev");
        defaults.put("aws", aws);

        // Development configuration
        Map<String, Object> dev = new LinkedHashMap<>();
        dev.put("app-port", 8080);
        dev.put("postgres-port", 5432);
        dev.put("auto-open-browser", false);
        dev.put("use-aws-s3", false);
        defaults.put("dev", dev);

        // Documentation browser configuration
        Map<String, Object> docs = new LinkedHashMap<>();
        docs.put("editor", "code");
        docs.put("pager", "less");
        defaults.put("docs", docs);

        // Deployment configuration
        Map<String, Object> deploy = new LinkedHashMap<>();
        deploy.put("auto-approve", false);
        deploy.put("default-tag", "latest");
        defaults.put("deploy", deploy);

        return defaults;
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String key) {
        String[] parts = key.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> map, String key, Object value) {
        String[] parts = key.split("\\.");
        Map<String, Object> current = map;

        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(
                parts[i], k -> new LinkedHashMap<>());
        }

        current.put(parts[parts.length - 1], value);
    }
}
