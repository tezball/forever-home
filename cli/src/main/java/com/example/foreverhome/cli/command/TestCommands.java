package com.example.foreverhome.cli.command;

import com.example.foreverhome.cli.config.CliProperties;
import com.example.foreverhome.cli.service.PortChecker;
import com.example.foreverhome.cli.service.ProcessExecutor;
import com.example.foreverhome.cli.ui.TerminalOutput;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Testing commands for E2E, Gatling, and unit tests.
 */
@Component
@Command(command = "test", group = "Testing", description = "Testing commands")
public class TestCommands {

    private static final Map<String, String> E2E_SUITES = Map.ofEntries(
        Map.entry("all", ""),
        Map.entry("auth", "auth-flows.spec.ts auth.spec.ts"),
        Map.entry("foster", "foster-journey.spec.ts"),
        Map.entry("adopter", "adopter-journey.spec.ts"),
        Map.entry("rescue", "rescue-journey.spec.ts"),
        Map.entry("vet", "vet-journey.spec.ts"),
        Map.entry("admin", "admin-journey.spec.ts"),
        Map.entry("lifecycle", "adoption-lifecycle.spec.ts"),
        Map.entry("public", "public-pages.spec.ts pet-browsing.spec.ts")
    );

    private static final Map<String, String> GATLING_SIMULATIONS = Map.of(
        "all", "com.example.foreverhome.simulation.AllUserFlowsSimulation",
        "allflows", "com.example.foreverhome.simulation.AllUserFlowsSimulation",
        "stress", "com.example.foreverhome.simulation.RegistrationStressSimulation",
        "endurance", "com.example.foreverhome.simulation.FullFeatureEnduranceSimulation",
        "registration", "com.example.foreverhome.simulation.UserRegistrationSimulation"
    );

    private final ProcessExecutor executor;
    private final PortChecker portChecker;
    private final CliProperties properties;
    private final TerminalOutput output;

    public TestCommands(ProcessExecutor executor, PortChecker portChecker,
                        CliProperties properties, TerminalOutput output) {
        this.executor = executor;
        this.portChecker = portChecker;
        this.properties = properties;
        this.output = output;
    }

    @Command(command = "e2e", description = "Run Playwright E2E tests")
    public void e2e(
            @Option(longNames = "suite", shortNames = 's',
                    defaultValue = "all",
                    description = "Test suite: all, auth, foster, adopter, rescue, vet, admin, lifecycle, public")
            String suite,
            @Option(longNames = "headed",
                    description = "Run in headed browser mode")
            boolean headed,
            @Option(longNames = "ui",
                    description = "Open Playwright UI mode")
            boolean ui,
            @Option(longNames = "debug",
                    description = "Run in debug mode")
            boolean debug,
            @Option(longNames = "project", shortNames = 'p',
                    description = "Browser to run tests on (chromium, firefox, webkit)")
            String project) {

        // Check if app is running
        if (!portChecker.isPortOpen(properties.appPort())) {
            output.error("App is not running on port %d", properties.appPort());
            output.info("Start the app first with: fh dev start");
            return;
        }

        output.header("Running E2E Tests");
        output.info("Target: http://localhost:%d", properties.appPort());
        output.println();

        // Ensure dependencies are installed
        if (!executor.fileExists("frontend/node_modules")) {
            output.info("Installing dependencies...");
            executor.runNpm("install");
        }

        // Build command
        List<String> args = new ArrayList<>();
        args.add("playwright");
        args.add("test");

        // Add test files for suite
        String files = E2E_SUITES.getOrDefault(suite.toLowerCase(), suite);
        if (!files.isEmpty()) {
            for (String file : files.split(" ")) {
                args.add(file);
            }
        }

        // Add flags
        if (headed) args.add("--headed");
        if (ui) args.add("--ui");
        if (debug) args.add("--debug");
        if (project != null) {
            args.add("--project=" + project);
        }

        output.info("Running: npx %s", String.join(" ", args));
        output.println();

        int exitCode = executor.runNpx(args.toArray(new String[0]));

        output.println();
        if (exitCode == 0) {
            output.success("All tests passed!");
        } else {
            output.error("Some tests failed");
            output.info("Run 'fh test e2e-report' to view the HTML report");
        }

        output.info("Reports at: frontend/playwright-report/");
    }

    @Command(command = "e2e-report", description = "Open Playwright HTML report")
    public void e2eReport() {
        output.info("Opening Playwright report...");
        executor.runNpx("playwright", "show-report");
    }

    @Command(command = "e2e-suites", description = "List available E2E test suites")
    public void e2eSuites() {
        output.header("Available E2E Test Suites");
        output.println();

        output.println("  " + output.cyan("all") + "        - Run ALL e2e tests (default)");
        output.println("  " + output.cyan("auth") + "       - Authentication flows (login, register, logout)");
        output.println("  " + output.cyan("foster") + "     - Foster user journey (create pet, submit to rescue)");
        output.println("  " + output.cyan("adopter") + "    - Adopter user journey (browse, favorite, apply)");
        output.println("  " + output.cyan("rescue") + "     - Rescue org journey (review pets, manage vets)");
        output.println("  " + output.cyan("vet") + "        - Vet journey (lookup pets, sign-off)");
        output.println("  " + output.cyan("admin") + "      - Admin journey (analytics, moderation)");
        output.println("  " + output.cyan("lifecycle") + "  - Full adoption lifecycle (cross-role)");
        output.println("  " + output.cyan("public") + "     - Public pages (unauthenticated browsing)");

        output.println();
        output.info("Usage: fh test e2e --suite=<name>");
        output.info("       fh test e2e --suite=auth --headed");
    }

    @Command(command = "gatling", description = "Run Gatling load tests")
    public void gatling(
            @Option(longNames = "simulation", shortNames = 's',
                    defaultValue = "all",
                    description = "Simulation: all, stress, endurance, registration")
            String simulation,
            @Option(longNames = "users", shortNames = 'u',
                    defaultValue = "5",
                    description = "Number of users per scenario")
            int users,
            @Option(longNames = "ramp", shortNames = 'r',
                    defaultValue = "30",
                    description = "Ramp-up duration in seconds")
            int rampDuration,
            @Option(longNames = "duration", shortNames = 'd',
                    defaultValue = "120",
                    description = "Total test duration in seconds")
            int testDuration) {

        // Check if app is running
        if (!portChecker.isPortOpen(properties.appPort())) {
            output.error("App is not running on port %d", properties.appPort());
            output.info("Start the app first with: fh dev start");
            return;
        }

        String simClass = GATLING_SIMULATIONS.getOrDefault(
            simulation.toLowerCase(), simulation);

        output.header("Running Gatling Load Tests");
        output.info("Simulation: %s", simClass);
        output.info("Users: %d, Ramp: %ds, Duration: %ds", users, rampDuration, testDuration);
        output.println();

        int exitCode = executor.runMaven("gatling:test",
            "-Dgatling.simulationClass=" + simClass,
            "-DUSERS=" + users,
            "-DRAMP_DURATION=" + rampDuration,
            "-DTEST_DURATION=" + testDuration,
            "-DBASE_URL=http://localhost:" + properties.appPort());

        output.println();
        if (exitCode == 0) {
            output.success("Load test completed");
        } else {
            output.error("Load test failed");
        }

        output.info("Reports at: target/gatling/");
    }

    @Command(command = "gatling-sims", description = "List available Gatling simulations")
    public void gatlingSims() {
        output.header("Available Gatling Simulations");
        output.println();

        output.println("  " + output.cyan("all") + "          - ALL user flows (default)");
        output.println("  " + output.cyan("stress") + "       - Stress test (high load)");
        output.println("  " + output.cyan("endurance") + "    - Endurance test (runs until Ctrl+C)");
        output.println("  " + output.cyan("registration") + " - User registration only");

        output.println();
        output.println("All flows covers:");
        output.item("Public browsing (pets, rescue profiles)");
        output.item("Foster flow (register, create pet, submit)");
        output.item("Adopter flow (browse, favorite, apply)");
        output.item("Rescue Org flow (approve pets, manage vets)");
        output.item("Vet flow (lookup pets, sign-off)");
        output.item("Admin flow (analytics, moderation)");

        output.println();
        output.info("Usage: fh test gatling --simulation=<name>");
        output.info("       fh test gatling --simulation=stress --users=50");
    }

    @Command(command = "unit", description = "Run unit tests")
    public void unit(
            @Option(longNames = "class", shortNames = 'c',
                    description = "Specific test class to run")
            String testClass,
            @Option(longNames = "method", shortNames = 'm',
                    description = "Specific test method to run (requires --class)")
            String testMethod) {

        output.header("Running Unit Tests");
        output.println();

        List<String> args = new ArrayList<>();
        args.add("test");

        if (testClass != null) {
            String testSelector = testMethod != null
                ? testClass + "#" + testMethod
                : testClass;
            args.add("-Dtest=" + testSelector);
            output.info("Running: %s", testSelector);
        } else {
            output.info("Running all unit tests...");
        }

        output.println();

        int exitCode = executor.runMaven(args.toArray(new String[0]));

        output.println();
        if (exitCode == 0) {
            output.success("All tests passed!");
        } else {
            output.error("Some tests failed");
        }
    }

    @Command(command = "build", description = "Build the project")
    public void build(
            @Option(longNames = "skip-tests", shortNames = 'T',
                    description = "Skip running tests")
            boolean skipTests,
            @Option(longNames = "skip-frontend", shortNames = 'F',
                    description = "Skip frontend build")
            boolean skipFrontend) {

        output.header("Building Project");
        output.println();

        List<String> args = new ArrayList<>();
        args.add("package");

        if (skipTests) {
            args.add("-DskipTests");
            output.info("Skipping tests");
        }

        if (skipFrontend) {
            args.add("-Dskip.frontend=true");
            output.info("Skipping frontend build");
        }

        int exitCode = executor.runMaven(args.toArray(new String[0]));

        output.println();
        if (exitCode == 0) {
            output.success("Build completed successfully");
            output.info("JAR at: target/forever-home-0.0.1-SNAPSHOT.jar");
        } else {
            output.error("Build failed");
        }
    }
}
