package com.example.foreverhome.cli.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service for executing shell commands via ProcessBuilder.
 * Handles command execution, output capture, and working directory management.
 */
@Service
public class ProcessExecutor {

    private final Path projectRoot;

    public ProcessExecutor() {
        this.projectRoot = findProjectRoot();
    }

    /**
     * Run a command in the project root directory, inheriting IO.
     */
    public int run(String... command) {
        return runInDirectory(null, command);
    }

    /**
     * Run a command in a subdirectory of the project root, inheriting IO.
     */
    public int runInDirectory(String subdir, String... command) {
        try {
            ProcessBuilder pb = createProcessBuilder(subdir, command);
            pb.inheritIO();

            Process process = pb.start();
            return process.waitFor();
        } catch (Exception e) {
            System.err.println("Command failed: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Run a command quietly (no output), return exit code.
     */
    public int runQuiet(String... command) {
        try {
            ProcessBuilder pb = createProcessBuilder(null, command);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);

            Process process = pb.start();
            return process.waitFor();
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * Run a command and capture its output.
     */
    public String capture(String... command) {
        return captureInDirectory(null, command);
    }

    /**
     * Run a command in a subdirectory and capture its output.
     */
    public String captureInDirectory(String subdir, String... command) {
        try {
            ProcessBuilder pb = createProcessBuilder(subdir, command);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String result = new String(process.getInputStream().readAllBytes());
            process.waitFor();
            return result;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Run a command with input piped to stdin.
     */
    public int runWithInput(String input, String... command) {
        try {
            ProcessBuilder pb = createProcessBuilder(null, command);

            Process process = pb.start();

            try (OutputStream os = process.getOutputStream()) {
                os.write(input.getBytes());
                os.flush();
            }

            return process.waitFor();
        } catch (Exception e) {
            System.err.println("Command failed: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Run a command and stream output line by line to a consumer.
     */
    public int runWithOutput(Consumer<String> outputConsumer, String... command) {
        return runWithOutputInDirectory(null, outputConsumer, command);
    }

    /**
     * Run a command in a subdirectory and stream output line by line.
     */
    public int runWithOutputInDirectory(String subdir, Consumer<String> outputConsumer, String... command) {
        try {
            ProcessBuilder pb = createProcessBuilder(subdir, command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputConsumer.accept(line);
                }
            }

            return process.waitFor();
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * Run a command in the background (non-blocking).
     */
    public Process runInBackground(String... command) {
        return runInBackgroundInDirectory(null, command);
    }

    /**
     * Run a command in the background in a subdirectory.
     */
    public Process runInBackgroundInDirectory(String subdir, String... command) {
        try {
            ProcessBuilder pb = createProcessBuilder(subdir, command);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            return pb.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start background process: " + e.getMessage(), e);
        }
    }

    /**
     * Run docker compose command.
     */
    public int runDockerCompose(String... args) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("compose");
        command.addAll(Arrays.asList(args));
        return run(command.toArray(new String[0]));
    }

    /**
     * Run Maven wrapper command.
     */
    public int runMaven(String... args) {
        List<String> command = new ArrayList<>();
        command.add("./mvnw");
        command.addAll(Arrays.asList(args));
        return run(command.toArray(new String[0]));
    }

    /**
     * Run npm command in the frontend directory.
     */
    public int runNpm(String... args) {
        List<String> command = new ArrayList<>();
        command.add("npm");
        command.addAll(Arrays.asList(args));
        return runInDirectory("frontend", command.toArray(new String[0]));
    }

    /**
     * Run npx command in the frontend directory.
     */
    public int runNpx(String... args) {
        List<String> command = new ArrayList<>();
        command.add("npx");
        command.addAll(Arrays.asList(args));
        return runInDirectory("frontend", command.toArray(new String[0]));
    }

    /**
     * Check if a required command is available.
     */
    public void requireCommand(String command) {
        int result = runQuiet("which", command);
        if (result != 0) {
            throw new RuntimeException("Required command not found: " + command);
        }
    }

    /**
     * Check if a file exists relative to project root.
     */
    public boolean fileExists(String path) {
        return projectRoot.resolve(path).toFile().exists();
    }

    /**
     * Get the project root directory.
     */
    public Path getProjectRoot() {
        return projectRoot;
    }

    private ProcessBuilder createProcessBuilder(String subdir, String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);

        Path workDir = subdir != null
            ? projectRoot.resolve(subdir)
            : projectRoot;
        pb.directory(workDir.toFile());

        // Inherit environment
        pb.environment().putAll(System.getenv());

        return pb;
    }

    private Path findProjectRoot() {
        // Start from current directory and look for markers
        Path current = Path.of(System.getProperty("user.dir"));

        while (current != null) {
            if (current.resolve("pom.xml").toFile().exists() &&
                current.resolve("dev.sh").toFile().exists()) {
                return current;
            }
            // Also check if we're in the cli subdirectory
            if (current.resolve("cli").toFile().exists() &&
                current.resolve("cli/pom.xml").toFile().exists()) {
                return current;
            }
            current = current.getParent();
        }

        // Fallback to current directory
        return Path.of(System.getProperty("user.dir"));
    }
}
