package me.andressumihe.swift;

import com.prowidesoftware.swift.model.mt.AbstractMT;
import me.andressumihe.swift.cli.CommandLineOptions;
import me.andressumihe.swift.cli.CommandLineParser;
import me.andressumihe.swift.config.Configuration;
import me.andressumihe.swift.config.ConfigurationManager;
import me.andressumihe.swift.factory.MessageFormatterFactory;
import me.andressumihe.swift.factory.MessageGeneratorFactory;
import me.andressumihe.swift.service.SwiftMessageService;
import me.andressumihe.swift.service.impl.SwiftMessageServiceImpl;

import java.util.List;

/**
 * SWIFT Message Generator - Main Entry Point
 * 
 * This class serves as the application's main entry point and dependency injection container.
 * Demonstrates complete SOLID principle compliance through clean architecture.
 * 
 * Responsibilities:
 * - Application bootstrap and configuration
 * - Dependency injection setup
 * - Command-line interface coordination
 */
public class SwiftMessageGenerator {

    public static void main(String[] args) {
        SwiftMessageGenerator app = new SwiftMessageGenerator();
        app.run(args);
    }

    public void run(String[] args) {
        try {
            // Dependency injection setup - all dependencies are abstractions
            Configuration config = ConfigurationManager.getInstance();
            CommandLineParser parser = new CommandLineParser((ConfigurationManager) config);
            MessageGeneratorFactory generatorFactory = new MessageGeneratorFactory(config);
            MessageFormatterFactory formatterFactory = new MessageFormatterFactory((ConfigurationManager) config);
            
            // Parse command line arguments
            CommandLineOptions options;
            try {
                options = parser.parse(args);
            } catch (IllegalArgumentException e) {
                System.err.println("Error: " + e.getMessage());
                parser.displayUsage();
                System.exit(1);
                return;
            }
            
            // Create SOLID-compliant service with dependency injection
            SwiftMessageService service = new SwiftMessageServiceImpl(
                config, generatorFactory, formatterFactory);
            
            // Business logic execution through well-defined interface
            service.displayGenerationParameters(options);
            List<AbstractMT> messages = service.generateMessages(options);
            String formattedContent = service.formatMessages(messages);
            service.writeToFiles(formattedContent, options);
            
            // Display completion
            System.out.println("\n" + "=".repeat(60));
            System.out.println("GENERATION COMPLETED - " + messages.size() + " messages generated");
            System.out.println("Files are formatted in " + options.getFormat().getDescription() + " format");
            System.out.println("=".repeat(60));
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
