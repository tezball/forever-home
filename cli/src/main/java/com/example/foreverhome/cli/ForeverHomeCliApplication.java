package com.example.foreverhome.cli;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.shell.command.annotation.CommandScan;

/**
 * Forever Home CLI - A unified command-line tool for development, testing, and deployment.
 *
 * Supports both interactive REPL mode and single-command execution:
 * - Interactive: `fh` (enters shell)
 * - Single command: `fh dev start`, `fh docs search "pet status"`
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@CommandScan
public class ForeverHomeCliApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ForeverHomeCliApplication.class)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false)
                .run(args);
    }
}
