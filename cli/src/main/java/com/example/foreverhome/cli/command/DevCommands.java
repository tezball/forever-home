package com.example.foreverhome.cli.command;

import com.example.foreverhome.cli.config.CliProperties;
import com.example.foreverhome.cli.service.ConfigManager;
import com.example.foreverhome.cli.service.PortChecker;
import com.example.foreverhome.cli.service.ProcessExecutor;
import com.example.foreverhome.cli.ui.ProgressReporter;
import com.example.foreverhome.cli.ui.TerminalOutput;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Development environment commands.
 * Ports functionality from dev.sh for managing local development services.
 */
@ShellComponent
public class DevCommands {

    private final ProcessExecutor executor;
    private final PortChecker portChecker;
    private final ConfigManager config;
    private final CliProperties properties;
    private final TerminalOutput output;
    private final ProgressReporter progress;

    public DevCommands(ProcessExecutor executor, PortChecker portChecker,
                       ConfigManager config, CliProperties properties,
                       TerminalOutput output, ProgressReporter progress) {
        this.executor = executor;
        this.portChecker = portChecker;
        this.config = config;
        this.properties = properties;
        this.output = output;
        this.progress = progress;
    }

    @ShellMethod(key = "dev start", value = "Start all development services")
    public void start(
            @ShellOption(value = {"-s", "--s3"}, defaultValue = "false",
                    help = "Use AWS S3 instead of LocalStack")
            boolean useAwsS3) {

        String mode = useAwsS3 ? "AWS S3" : "LocalStack S3";
        output.header("Starting Forever Home (" + mode + ")");
        output.println();

        // Check prerequisites
        checkPrerequisites();

        // Load AWS credentials if needed
        if (useAwsS3) {
            loadAwsCredentials();
        }

        // Start Docker services
        startDockerServices();

        // Start the Spring Boot application
        startApplication(useAwsS3);

        // Show status
        output.println();
        status();

        output.println();
        output.info("Open http://localhost:%d in your browser", properties.appPort());
    }

    @ShellMethod(key = "dev stop", value = "Stop all services (preserves data)")
    public void stop() {
        output.header("Stopping Forever Home");
        output.println();

        // Stop the application
        progress.start("Stopping application");
        if (portChecker.isPortInUse(properties.appPort())) {
            portChecker.killPort(properties.appPort());
            progress.complete();
        } else {
            progress.skip("Not running");
        }

        // Stop Docker services
        progress.start("Stopping Docker services");
        executor.runDockerCompose("down");
        progress.complete();

        output.success("All services stopped");
    }

    @ShellMethod(key = "dev clean", value = "Stop services and remove all data")
    public void clean() {
        output.header("Stopping Forever Home (Clean)");
        output.println();
        output.warning("This will remove all data!");
        output.println();

        // Stop the application
        progress.start("Stopping application");
        if (portChecker.isPortInUse(properties.appPort())) {
            portChecker.killPort(properties.appPort());
            progress.complete();
        } else {
            progress.skip("Not running");
        }

        // Stop Docker services and remove volumes
        progress.start("Removing Docker volumes");
        executor.runDockerCompose("down", "-v");
        progress.complete();

        output.success("All services stopped and data removed");
    }

    @ShellMethod(key = "dev restart", value = "Clean restart (stop + clean + start)")
    public void restart(
            @ShellOption(value = {"-s", "--s3"}, defaultValue = "false",
                    help = "Use AWS S3 instead of LocalStack")
            boolean useAwsS3) {

        clean();
        output.println();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        start(useAwsS3);
    }

    @ShellMethod(key = "dev status", value = "Show service status")
    public void status() {
        output.header("Forever Home Service Status");
        output.println();

        checkService("PostgreSQL", properties.postgresPort(), null);
        checkService("LocalStack", properties.localstackPort(), null);
        checkService("Loki", properties.lokiPort(), "http://localhost:" + properties.lokiPort());
        checkService("Grafana", properties.grafanaPort(), "http://localhost:" + properties.grafanaPort() + " (admin/admin)");
        checkService("Mailpit", properties.mailpitUiPort(), "http://localhost:" + properties.mailpitUiPort());
        checkService("Moderation", properties.moderationServicePort(), "http://localhost:" + properties.moderationServicePort());
        checkService("App", properties.appPort(), "http://localhost:" + properties.appPort());

        output.println();
    }

    @ShellMethod(key = "dev logs", value = "View logs for a service")
    public void logs(
            @ShellOption(value = {"-s", "--service"}, defaultValue = "app",
                    help = "Service to view logs for (app, postgres, localstack, grafana, loki, mailpit)")
            String service,
            @ShellOption(value = {"-f", "--follow"}, defaultValue = "false",
                    help = "Follow log output")
            boolean follow,
            @ShellOption(value = {"-n", "--tail"}, defaultValue = "100",
                    help = "Number of lines to show")
            int tail) {

        if ("app".equals(service)) {
            output.info("Application logs are in the terminal where it was started");
            output.info("Or check Grafana Loki at http://localhost:%d", properties.grafanaPort());
        } else {
            String[] args = follow
                ? new String[]{"logs", "-f", "--tail", String.valueOf(tail), service}
                : new String[]{"logs", "--tail", String.valueOf(tail), service};
            executor.runDockerCompose(args);
        }
    }

    @ShellMethod(key = "dev db", value = "Database utilities")
    public void db() {
        output.info("Database commands:");
        output.item("fh dev db-reset  - Reset database to seed data");
        output.item("fh dev db-shell  - Open psql shell");
    }

    @ShellMethod(key = "dev db-reset", value = "Reset database to seed data")
    public void dbReset() {
        if (!portChecker.isPortOpen(properties.postgresPort())) {
            output.error("PostgreSQL is not running. Start services first: fh dev start");
            return;
        }

        output.warning("This will reset the database to seed data");
        output.println();

        progress.start("Resetting database");

        // The app handles reset via RESET_DATABASE=true environment variable
        // For now, we'll trigger it by restarting the app with that variable
        output.info("Restarting app with database reset...");

        // Kill the running app if any
        if (portChecker.isPortInUse(properties.appPort())) {
            portChecker.killPort(properties.appPort());
            portChecker.waitForPortFree(properties.appPort(), 10);
        }

        // Start with reset flag
        ProcessBuilder pb = new ProcessBuilder("./mvnw", "spring-boot:run", "-q");
        pb.directory(executor.getProjectRoot().toFile());
        pb.environment().put("RESET_DATABASE", "true");
        pb.inheritIO();

        try {
            pb.start();
            portChecker.waitForPort(properties.appPort(), "App", 60);
            progress.complete();
            output.success("Database has been reset");
        } catch (Exception e) {
            progress.fail(e.getMessage());
        }
    }

    @ShellMethod(key = "dev db-shell", value = "Open psql shell")
    public void dbShell() {
        if (!portChecker.isPortOpen(properties.postgresPort())) {
            output.error("PostgreSQL is not running. Start services first: fh dev start");
            return;
        }

        output.info("Connecting to PostgreSQL...");
        executor.run("docker", "compose", "exec", "postgres",
            "psql", "-U", "postgres", "-d", "foreverhome");
    }

    @ShellMethod(key = "dev moderation-reset", value = "Reset all pet moderation statuses to PENDING for AI re-check")
    public void moderationReset(
            @ShellOption(value = {"-u", "--url"}, defaultValue = "http://localhost:8080",
                    help = "Base URL of the Forever Home API")
            String baseUrl) {

        if (!portChecker.isPortOpen(properties.appPort()) && baseUrl.contains("localhost:8080")) {
            output.error("Application is not running. Start services first: fh dev start");
            return;
        }

        output.header("Resetting Pet Moderation Statuses");
        output.println();

        progress.start("Calling moderation reset API");

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/dev/moderation/reset"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                progress.complete();

                // Parse simple JSON response: {"petsAffected":N,"message":"..."}
                String body = response.body();
                int petsAffected = 0;
                String message = "Moderation statuses reset";

                Matcher countMatcher = Pattern.compile("\"petsAffected\"\\s*:\\s*(\\d+)").matcher(body);
                if (countMatcher.find()) {
                    petsAffected = Integer.parseInt(countMatcher.group(1));
                }

                Matcher msgMatcher = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
                if (msgMatcher.find()) {
                    message = msgMatcher.group(1);
                }

                output.println();
                output.success(message);
                output.info("Pets affected: %d", petsAffected);
                output.println();
                output.info("All pets will be re-checked by the AI moderation service.");
            } else if (response.statusCode() == 404) {
                progress.fail("Endpoint not found");
                output.error("The moderation reset endpoint is not available.");
                output.info("Make sure test mode is enabled (app.test-mode.enabled=true)");
            } else {
                progress.fail("HTTP " + response.statusCode());
                output.error("Failed to reset moderation statuses: %s", response.body());
            }
        } catch (Exception e) {
            progress.fail(e.getMessage());
            output.error("Failed to connect to the API: %s", e.getMessage());
            output.info("Make sure the application is running: fh dev start");
        }
    }

    private void checkPrerequisites() {
        progress.start("Checking prerequisites");

        try {
            executor.requireCommand("docker");
            executor.requireCommand("java");
            progress.complete();
        } catch (RuntimeException e) {
            progress.fail(e.getMessage());
            throw e;
        }
    }

    private void loadAwsCredentials() {
        progress.start("Loading AWS credentials");

        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");

        if (accessKey == null || accessKey.isBlank() ||
            secretKey == null || secretKey.isBlank()) {

            // Check for .env file
            if (executor.fileExists(".env")) {
                output.info("Loading credentials from .env file");
                // Note: In a real implementation, we'd parse the .env file
                progress.complete();
            } else {
                progress.fail("AWS credentials not found");
                output.println();
                output.error("Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY, or create a .env file");
                output.info("Copy .env.example to .env and fill in your credentials");
                throw new RuntimeException("AWS credentials not configured");
            }
        } else {
            progress.complete();
        }

        String region = System.getenv().getOrDefault("AWS_REGION",
            config.getString("aws.region", "us-east-1"));
        String bucket = System.getenv().getOrDefault("AWS_S3_BUCKET",
            config.getString("aws.s3-bucket", "forever-home-images-dev"));

        output.info("AWS Region: %s", region);
        output.info("S3 Bucket: %s", bucket);
    }

    private void startDockerServices() {
        progress.start("Starting Docker services");
        int result = executor.runDockerCompose("up", "-d");
        if (result != 0) {
            progress.fail("Docker Compose failed");
            throw new RuntimeException("Failed to start Docker services");
        }
        progress.complete();

        // Wait for services
        output.println();
        waitForService("PostgreSQL", properties.postgresPort(), 30);
        waitForService("LocalStack", properties.localstackPort(), 30);
        waitForService("Loki", properties.lokiPort(), 30);
        waitForService("Grafana", properties.grafanaPort(), 30);
        waitForService("Mailpit", properties.mailpitUiPort(), 30);
        waitForService("Moderation", properties.moderationServicePort(), 90);
    }

    private void waitForService(String name, int port, int timeout) {
        progress.start("Waiting for " + name);
        boolean ready = portChecker.waitForPort(port, name, timeout);
        if (ready) {
            progress.complete();
        } else {
            progress.skip("May still be starting");
        }
    }

    private void startApplication(boolean useAwsS3) {
        if (portChecker.isPortOpen(properties.appPort())) {
            output.info("Application already running on port %d", properties.appPort());
            return;
        }

        progress.start("Starting Spring Boot application");

        try {
            ProcessBuilder pb;
            if (useAwsS3) {
                pb = new ProcessBuilder("./mvnw", "spring-boot:run",
                    "-Dspring-boot.run.profiles=default,s3-aws",
                    "-Dmaven.test.skip=true", "-q");
            } else {
                pb = new ProcessBuilder("./mvnw", "spring-boot:run",
                    "-Dmaven.test.skip=true", "-q");
            }

            pb.directory(executor.getProjectRoot().toFile());
            pb.environment().putAll(System.getenv());

            // Redirect output to null (background process)
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);

            pb.start();

            // Wait for the app to be ready
            boolean ready = portChecker.waitForPort(properties.appPort(), "App", 90);
            if (ready) {
                progress.complete();
            } else {
                progress.fail("Timeout waiting for app to start");
            }
        } catch (Exception e) {
            progress.fail(e.getMessage());
            throw new RuntimeException("Failed to start application", e);
        }
    }

    private void checkService(String name, int port, String extra) {
        boolean running = portChecker.isPortOpen(port);
        String status = running ? output.green("Running") : output.red("Stopped");
        String portInfo = "port " + port;
        String extraInfo = (extra != null && running) ? " - " + extra : "";

        output.print("  [%-10s] %s on %s%s%n", name, status, portInfo, extraInfo);
    }
}
