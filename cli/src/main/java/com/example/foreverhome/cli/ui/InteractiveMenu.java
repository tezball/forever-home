package com.example.foreverhome.cli.ui;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.util.List;

/**
 * Service for interactive menu navigation and user prompts.
 */
@Component
@Lazy
public class InteractiveMenu {

    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM = "\033[2m";
    private static final String CYAN = "\033[36m";
    private static final String YELLOW = "\033[33m";
    private static final String GREEN = "\033[32m";

    private final Terminal terminal;
    private final LineReader lineReader;
    private final PrintWriter writer;

    public InteractiveMenu(@Lazy Terminal terminal, @Lazy LineReader lineReader) {
        this.terminal = terminal;
        this.lineReader = lineReader;
        this.writer = terminal.writer();
    }

    /**
     * Display a selection menu and return the selected option.
     *
     * @param prompt the prompt to display
     * @param options list of options to choose from
     * @return the selected option, or null if cancelled
     */
    public String select(String prompt, List<String> options) {
        writer.println();
        writer.println(BOLD + prompt + RESET);
        writer.println();

        for (int i = 0; i < options.size(); i++) {
            writer.printf("  %s%d.%s %s%n", CYAN, i + 1, RESET, options.get(i));
        }

        writer.println();
        writer.print("Enter choice (1-" + options.size() + ") or 'q' to cancel: ");
        writer.flush();

        try {
            String input = lineReader.readLine().trim();

            if (input.equalsIgnoreCase("q") || input.equalsIgnoreCase("quit")) {
                return null;
            }

            int choice = Integer.parseInt(input);
            if (choice >= 1 && choice <= options.size()) {
                return options.get(choice - 1);
            }
        } catch (Exception e) {
            // Invalid input
        }

        writer.println(YELLOW + "Invalid choice" + RESET);
        return null;
    }

    /**
     * Display a selection menu with descriptions.
     */
    public String selectWithDescription(String prompt, List<MenuOption> options) {
        writer.println();
        writer.println(BOLD + prompt + RESET);
        writer.println();

        for (int i = 0; i < options.size(); i++) {
            MenuOption opt = options.get(i);
            writer.printf("  %s%d.%s %s%n", CYAN, i + 1, RESET, opt.label());
            if (opt.description() != null) {
                writer.printf("     %s%s%s%n", DIM, opt.description(), RESET);
            }
        }

        writer.println();
        writer.print("Enter choice (1-" + options.size() + ") or 'q' to cancel: ");
        writer.flush();

        try {
            String input = lineReader.readLine().trim();

            if (input.equalsIgnoreCase("q") || input.equalsIgnoreCase("quit")) {
                return null;
            }

            int choice = Integer.parseInt(input);
            if (choice >= 1 && choice <= options.size()) {
                return options.get(choice - 1).value();
            }
        } catch (Exception e) {
            // Invalid input
        }

        writer.println(YELLOW + "Invalid choice" + RESET);
        return null;
    }

    /**
     * Display a confirmation prompt.
     *
     * @param prompt the prompt to display
     * @return true if confirmed, false otherwise
     */
    public boolean confirm(String prompt) {
        return confirm(prompt, false);
    }

    /**
     * Display a confirmation prompt with a default value.
     */
    public boolean confirm(String prompt, boolean defaultValue) {
        String hint = defaultValue ? "[Y/n]" : "[y/N]";
        writer.print(prompt + " " + hint + ": ");
        writer.flush();

        try {
            String input = lineReader.readLine().trim().toLowerCase();

            if (input.isEmpty()) {
                return defaultValue;
            }

            return input.equals("y") || input.equals("yes");
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Prompt for text input.
     */
    public String prompt(String prompt) {
        return prompt(prompt, null);
    }

    /**
     * Prompt for text input with a default value.
     */
    public String prompt(String prompt, String defaultValue) {
        String hint = defaultValue != null ? " [" + defaultValue + "]" : "";
        writer.print(prompt + hint + ": ");
        writer.flush();

        try {
            String input = lineReader.readLine().trim();

            if (input.isEmpty() && defaultValue != null) {
                return defaultValue;
            }

            return input;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Prompt for a password (hidden input).
     */
    public String promptPassword(String prompt) {
        writer.print(prompt + ": ");
        writer.flush();

        try {
            return lineReader.readLine('*').trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Display an interactive multi-select menu.
     *
     * @param prompt the prompt to display
     * @param options list of options to choose from
     * @return list of selected options
     */
    public List<String> multiSelect(String prompt, List<String> options) {
        boolean[] selected = new boolean[options.size()];

        while (true) {
            writer.println();
            writer.println(BOLD + prompt + RESET);
            writer.println(DIM + "(Enter number to toggle, 'done' when finished, 'q' to cancel)" + RESET);
            writer.println();

            for (int i = 0; i < options.size(); i++) {
                String marker = selected[i] ? GREEN + "[x]" + RESET : "[ ]";
                writer.printf("  %s %s%d.%s %s%n", marker, CYAN, i + 1, RESET, options.get(i));
            }

            writer.println();
            writer.print("Toggle: ");
            writer.flush();

            try {
                String input = lineReader.readLine().trim().toLowerCase();

                if (input.equals("q") || input.equals("quit")) {
                    return List.of();
                }

                if (input.equals("done") || input.equals("d")) {
                    return options.stream()
                        .filter(opt -> selected[options.indexOf(opt)])
                        .toList();
                }

                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= options.size()) {
                    selected[choice - 1] = !selected[choice - 1];
                }
            } catch (NumberFormatException e) {
                // Invalid input, continue
            } catch (Exception e) {
                return List.of();
            }
        }
    }

    /**
     * Wait for user to press Enter to continue.
     */
    public void pressEnterToContinue() {
        pressEnterToContinue("Press Enter to continue...");
    }

    /**
     * Wait for user to press Enter with custom message.
     */
    public void pressEnterToContinue(String message) {
        writer.print(DIM + message + RESET);
        writer.flush();
        try {
            lineReader.readLine();
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * A menu option with label, value, and optional description.
     */
    public record MenuOption(String label, String value, String description) {
        public MenuOption(String label, String value) {
            this(label, value, null);
        }

        public MenuOption(String label) {
            this(label, label, null);
        }
    }
}
