package com.example.foreverhome.cli.ui;

import org.jline.terminal.Terminal;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;

/**
 * Service for reporting progress of long-running operations.
 */
@Component
public class ProgressReporter {

    private static final String RESET = "\033[0m";
    private static final String GREEN = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String RED = "\033[31m";
    private static final String DIM = "\033[2m";

    private final Terminal terminal;
    private final PrintWriter writer;

    private String currentTask;
    private long startTime;
    private boolean spinnerActive;
    private Thread spinnerThread;

    public ProgressReporter(Terminal terminal) {
        this.terminal = terminal;
        this.writer = terminal.writer();
    }

    /**
     * Start a new task with a spinner.
     */
    public void start(String task) {
        this.currentTask = task;
        this.startTime = System.currentTimeMillis();

        writer.print(YELLOW + "  " + task + "... " + RESET);
        writer.flush();
    }

    /**
     * Start a task with an animated spinner.
     */
    public void startWithSpinner(String task) {
        this.currentTask = task;
        this.startTime = System.currentTimeMillis();
        this.spinnerActive = true;

        spinnerThread = new Thread(() -> {
            String[] frames = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
            int i = 0;
            while (spinnerActive) {
                writer.print("\r" + YELLOW + "  " + frames[i % frames.length] + " " + currentTask + "... " + RESET);
                writer.flush();
                i++;
                try {
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        spinnerThread.setDaemon(true);
        spinnerThread.start();
    }

    /**
     * Mark the current task as complete.
     */
    public void complete() {
        stopSpinner();
        long duration = System.currentTimeMillis() - startTime;
        writer.println(GREEN + "Done" + RESET + DIM + " (" + formatDuration(duration) + ")" + RESET);
        writer.flush();
        currentTask = null;
    }

    /**
     * Mark the current task as complete with a custom message.
     */
    public void complete(String message) {
        stopSpinner();
        long duration = System.currentTimeMillis() - startTime;
        writer.println(GREEN + message + RESET + DIM + " (" + formatDuration(duration) + ")" + RESET);
        writer.flush();
        currentTask = null;
    }

    /**
     * Mark the current task as failed.
     */
    public void fail() {
        fail(null);
    }

    /**
     * Mark the current task as failed with a reason.
     */
    public void fail(String reason) {
        stopSpinner();
        String message = RED + "Failed" + RESET;
        if (reason != null && !reason.isEmpty()) {
            message += ": " + reason;
        }
        writer.println(message);
        writer.flush();
        currentTask = null;
    }

    /**
     * Mark the current task as skipped.
     */
    public void skip(String reason) {
        stopSpinner();
        writer.println(YELLOW + "Skipped" + RESET + (reason != null ? ": " + reason : ""));
        writer.flush();
        currentTask = null;
    }

    /**
     * Update progress with a percentage.
     */
    public void update(int percent) {
        if (currentTask == null) return;

        stopSpinner();

        int width = 20;
        int filled = (int) (width * percent / 100.0);

        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 0; i < width; i++) {
            if (i < filled) bar.append("=");
            else if (i == filled) bar.append(">");
            else bar.append(" ");
        }
        bar.append("] ").append(percent).append("%");

        writer.print("\r  " + currentTask + "... " + bar);
        writer.flush();
    }

    /**
     * Update progress with a message.
     */
    public void update(String message) {
        if (currentTask == null) return;

        stopSpinner();

        writer.print("\r  " + currentTask + "... " + DIM + message + RESET + "          ");
        writer.flush();
    }

    /**
     * Log an informational message during a task.
     */
    public void log(String message) {
        boolean wasSpinning = spinnerActive;
        stopSpinner();

        // Clear current line and print log
        writer.print("\r" + " ".repeat(80) + "\r");
        writer.println(DIM + "    " + message + RESET);

        // Restart spinner if it was active
        if (wasSpinning && currentTask != null) {
            startWithSpinner(currentTask);
        } else if (currentTask != null) {
            writer.print(YELLOW + "  " + currentTask + "... " + RESET);
        }
        writer.flush();
    }

    /**
     * Execute a task with automatic progress reporting.
     */
    public <T> T execute(String task, TaskSupplier<T> supplier) {
        start(task);
        try {
            T result = supplier.get();
            complete();
            return result;
        } catch (Exception e) {
            fail(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Execute a void task with automatic progress reporting.
     */
    public void execute(String task, TaskRunnable runnable) {
        start(task);
        try {
            runnable.run();
            complete();
        } catch (Exception e) {
            fail(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void stopSpinner() {
        if (spinnerActive) {
            spinnerActive = false;
            if (spinnerThread != null) {
                try {
                    spinnerThread.join(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // Clear the spinner line
            writer.print("\r  " + currentTask + "... ");
        }
    }

    private String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            return String.format("%.1fs", millis / 1000.0);
        } else {
            long seconds = millis / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    @FunctionalInterface
    public interface TaskSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface TaskRunnable {
        void run() throws Exception;
    }
}
