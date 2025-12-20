package com.example.foreverhome.cli.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Service for checking port availability and managing processes on ports.
 */
@Service
public class PortChecker {

    private final ProcessExecutor executor;

    public PortChecker(ProcessExecutor executor) {
        this.executor = executor;
    }

    /**
     * Check if a port is accepting connections.
     */
    public boolean isPortOpen(int port) {
        try (Socket socket = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Wait for a port to start accepting connections.
     *
     * @param port the port to check
     * @param serviceName name of the service (for logging)
     * @param timeoutSeconds maximum time to wait
     * @return true if port is accepting connections, false if timeout
     */
    public boolean waitForPort(int port, String serviceName, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < deadline) {
            if (isPortOpen(port)) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    /**
     * Wait for a port to start accepting connections with default timeout.
     */
    public boolean waitForPort(int port, String serviceName) {
        return waitForPort(port, serviceName, 30);
    }

    /**
     * Kill any process listening on the specified port.
     */
    public void killPort(int port) {
        // Get PIDs using the port
        String pids = executor.capture("lsof", "-ti:" + port);
        if (pids == null || pids.isBlank()) {
            return;
        }

        // Kill each PID
        for (String pid : pids.trim().split("\n")) {
            if (!pid.isBlank()) {
                executor.runQuiet("kill", "-9", pid.trim());
            }
        }

        // Wait a moment for processes to terminate
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check if a port is in use by any process.
     */
    public boolean isPortInUse(int port) {
        String result = executor.capture("lsof", "-ti:" + port);
        return result != null && !result.isBlank();
    }

    /**
     * Get the PID of the process using a port.
     */
    public String getProcessOnPort(int port) {
        String result = executor.capture("lsof", "-ti:" + port);
        if (result != null && !result.isBlank()) {
            return result.trim().split("\n")[0];
        }
        return null;
    }

    /**
     * Wait for a port to become free.
     */
    public boolean waitForPortFree(int port, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < deadline) {
            if (!isPortInUse(port)) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }
}
