package com.example.foreverhome.cli.ui;

import org.jline.terminal.Terminal;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;

/**
 * Service for formatted terminal output with ANSI colors and styling.
 */
@Component
public class TerminalOutput {

    // ANSI color codes
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM = "\033[2m";
    private static final String UNDERLINE = "\033[4m";

    private static final String BLACK = "\033[30m";
    private static final String RED = "\033[31m";
    private static final String GREEN = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String BLUE = "\033[34m";
    private static final String MAGENTA = "\033[35m";
    private static final String CYAN = "\033[36m";
    private static final String WHITE = "\033[37m";

    private final Terminal terminal;
    private final PrintWriter writer;

    public TerminalOutput(Terminal terminal) {
        this.terminal = terminal;
        this.writer = terminal.writer();
    }

    // Basic output methods

    public void print(String format, Object... args) {
        writer.printf(format, args);
        writer.flush();
    }

    public void println(String text) {
        writer.println(text);
        writer.flush();
    }

    public void println() {
        writer.println();
        writer.flush();
    }

    public void newLine() {
        println();
    }

    // Styled output methods

    public void header(String text) {
        println();
        println(cyan("=== " + text + " ==="));
    }

    public void subheader(String text) {
        println(bold(text));
    }

    public void info(String format, Object... args) {
        println(blue("[INFO] ") + String.format(format, args));
    }

    public void success(String format, Object... args) {
        println(green("[OK] ") + String.format(format, args));
    }

    public void warning(String format, Object... args) {
        println(yellow("[WARN] ") + String.format(format, args));
    }

    public void error(String format, Object... args) {
        println(red("[ERROR] ") + String.format(format, args));
    }

    public void debug(String format, Object... args) {
        println(dim(String.format(format, args)));
    }

    public void item(String text) {
        println("  - " + text);
    }

    public void numberedItem(int number, String text) {
        println(String.format("  %d. %s", number, text));
    }

    // Color helper methods (return styled strings)

    public String bold(String s) {
        return BOLD + s + RESET;
    }

    public String dim(String s) {
        return DIM + s + RESET;
    }

    public String underline(String s) {
        return UNDERLINE + s + RESET;
    }

    public String black(String s) {
        return BLACK + s + RESET;
    }

    public String red(String s) {
        return RED + s + RESET;
    }

    public String green(String s) {
        return GREEN + s + RESET;
    }

    public String yellow(String s) {
        return YELLOW + s + RESET;
    }

    public String blue(String s) {
        return BLUE + s + RESET;
    }

    public String magenta(String s) {
        return MAGENTA + s + RESET;
    }

    public String cyan(String s) {
        return CYAN + s + RESET;
    }

    public String white(String s) {
        return WHITE + s + RESET;
    }

    // Status indicators

    public String statusRunning() {
        return green("Running");
    }

    public String statusStopped() {
        return red("Stopped");
    }

    public String statusPending() {
        return yellow("Pending");
    }

    // Table formatting

    public void table(String[] headers, String[][] rows) {
        // Calculate column widths
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length && i < widths.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }

        // Print header
        StringBuilder headerLine = new StringBuilder();
        StringBuilder separator = new StringBuilder();
        for (int i = 0; i < headers.length; i++) {
            headerLine.append(String.format("%-" + widths[i] + "s", headers[i]));
            separator.append("-".repeat(widths[i]));
            if (i < headers.length - 1) {
                headerLine.append("  ");
                separator.append("--");
            }
        }
        println(bold(headerLine.toString()));
        println(separator.toString());

        // Print rows
        for (String[] row : rows) {
            StringBuilder rowLine = new StringBuilder();
            for (int i = 0; i < row.length && i < widths.length; i++) {
                rowLine.append(String.format("%-" + widths[i] + "s", row[i]));
                if (i < row.length - 1) {
                    rowLine.append("  ");
                }
            }
            println(rowLine.toString());
        }
    }

    // Paged output for long content

    public void paged(String content) {
        int termHeight = terminal.getHeight();
        if (termHeight <= 0) {
            termHeight = 24; // Default if terminal height unknown
        }

        String[] lines = content.split("\n");

        if (lines.length <= termHeight - 2) {
            // Content fits, print directly
            print(content);
        } else {
            // Paginate
            for (int i = 0; i < lines.length; i++) {
                println(lines[i]);

                if ((i + 1) % (termHeight - 1) == 0 && i < lines.length - 1) {
                    print(dim("-- Press Enter for more, 'q' to quit --"));
                    writer.flush();
                    try {
                        int ch = terminal.reader().read();
                        // Clear the prompt line
                        print("\r" + " ".repeat(40) + "\r");
                        if (ch == 'q' || ch == 'Q') {
                            break;
                        }
                    } catch (Exception e) {
                        break;
                    }
                }
            }
        }
        writer.flush();
    }

    // Progress bar

    public void progressBar(String label, int percent) {
        int width = 30;
        int filled = (int) (width * percent / 100.0);

        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 0; i < width; i++) {
            if (i < filled) bar.append("=");
            else if (i == filled) bar.append(">");
            else bar.append(" ");
        }
        bar.append("] ").append(percent).append("%");

        print("\r%s %s", label, bar);
        writer.flush();
    }

    public void clearLine() {
        print("\r" + " ".repeat(80) + "\r");
    }

    // Box drawing

    public void box(String title, String content) {
        String[] lines = content.split("\n");
        int maxWidth = title.length();
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, line.length());
        }

        String horizontal = "─".repeat(maxWidth + 2);
        println("┌" + horizontal + "┐");
        println("│ " + bold(String.format("%-" + maxWidth + "s", title)) + " │");
        println("├" + horizontal + "┤");
        for (String line : lines) {
            println("│ " + String.format("%-" + maxWidth + "s", line) + " │");
        }
        println("└" + horizontal + "┘");
    }
}
