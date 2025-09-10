package me.andressumihe.swift.service.impl;

import com.prowidesoftware.swift.model.mt.AbstractMT;
import me.andressumihe.swift.cli.CommandLineOptions;
import me.andressumihe.swift.config.Configuration;
import me.andressumihe.swift.factory.MessageFormatterFactory;
import me.andressumihe.swift.factory.MessageGeneratorFactory;
import me.andressumihe.swift.formatters.MessageFormatter;
import me.andressumihe.swift.exceptions.MessageGenerationException;
import me.andressumihe.swift.generators.MessageGenerator;
import me.andressumihe.swift.service.SwiftMessageService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Default implementation of SwiftMessageService.
 */
public class SwiftMessageServiceImpl implements SwiftMessageService {
    
    private final Configuration config;
    private final MessageGeneratorFactory generatorFactory;
    private final MessageFormatterFactory formatterFactory;
    
    // Current context to maintain state for formatMessages method
    private CommandLineOptions currentOptions;
    
    public SwiftMessageServiceImpl(Configuration config, 
                                   MessageGeneratorFactory generatorFactory,
                                   MessageFormatterFactory formatterFactory) {
        this.config = config;
        this.generatorFactory = generatorFactory;
        this.formatterFactory = formatterFactory;
    }
    
    @Override
    public List<AbstractMT> generateMessages(CommandLineOptions options) throws MessageGenerationException {
        // Store options for later use in formatMessages
        this.currentOptions = options;
        
        MessageGenerator generator = generatorFactory.createGenerator(options.getMessageType());
        
        System.out.println("Generating " + options.getCount() + " " + 
            options.getDirection().name().toLowerCase() + " messages...");
        
        List<AbstractMT> messages = generator.generateMessages(options.getCount(), options.getDirection());
        
        // Display progress
        for (int i = 0; i < messages.size(); i++) {
            if ((i + 1) % 100 == 0 || i == messages.size() - 1) {
                System.out.printf("Generated %d/%d messages (%.1f%%)\n", 
                    i + 1, messages.size(), ((i + 1) * 100.0) / messages.size());
            }
        }
        
        return messages;
    }
    
    @Override
    public String formatMessages(List<AbstractMT> messages) throws Exception {
        if (currentOptions == null) {
            throw new IllegalStateException("No options available. Call generateMessages first or use the options-aware version.");
        }
        
        MessageFormatter formatter = formatterFactory.createFormatter(currentOptions.getFormat());
        return formatter.formatBatch(messages);
    }
    
    @Override
    public String[] writeToFiles(String formattedContent, CommandLineOptions options) throws Exception {
        // Create output directory if it doesn't exist
        Path outputDir = Paths.get(options.getOutputDirectory());
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
        
        String baseFileName = String.format("MT%s_%d_messages_%s_%s", 
            options.getMessageType().getCode(), 
            options.getCount(),
            options.getDirection().name().toLowerCase(),
            options.getFormat().getCode());
        
        // Determine file extension
        String extension;
        if (options.getFileExtension() != null && !options.getFileExtension().trim().isEmpty()) {
            // User specified extension
            extension = options.getFileExtension().trim();
            // Remove leading dot if present
            if (extension.startsWith(".")) {
                extension = extension.substring(1);
            }
        } else {
            // Default behavior - use format-specific extension or fallback to "txt"
            extension = options.getFormat().getFileExtension();
            if (extension == null || extension.trim().isEmpty()) {
                extension = "txt";
            }
        }
        
        // Write single file with specified extension
        Path outputPath = outputDir.resolve(baseFileName + "." + extension);
        Files.write(outputPath, formattedContent.getBytes(), 
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        String[] filePaths = {outputPath.toString()};
        
        System.out.println("File created:");
        System.out.println("  - " + extension.toUpperCase() + " File: " + outputPath);
        
        return filePaths;
    }
    
    @Override
    public void displayGenerationParameters(CommandLineOptions options) {
        System.out.println("=" + "=".repeat(60));
        System.out.println("SWIFT Message Generator");
        System.out.println("=" + "=".repeat(60));
        System.out.println("Message Type: MT" + options.getMessageType().getCode());
        System.out.println("Count: " + options.getCount());
        
        String directionLabel = options.isIncoming() ? "Incoming (with Block 5)" : "Outgoing";
        System.out.println("Direction: " + directionLabel);
        
        System.out.println("Output Directory: " + options.getOutputDirectory());
        
        // Show file extension information
        String extension;
        if (options.getFileExtension() != null && !options.getFileExtension().trim().isEmpty()) {
            extension = options.getFileExtension().trim();
            if (extension.startsWith(".")) {
                extension = extension.substring(1);
            }
            System.out.println("File Extension: ." + extension + " (user-specified)");
        } else {
            extension = options.getFormat().getFileExtension();
            if (extension == null || extension.trim().isEmpty()) {
                extension = "txt";
            }
            System.out.println("File Extension: ." + extension + " (from format: " + options.getFormat().getCode() + ")");
        }
        
        System.out.println("Sender BIC: " + config.getDefaultSenderBic());
        System.out.println("Receiver BIC: " + config.getDefaultReceiverBic());
        System.out.println("Currency: " + config.getDefaultCurrency());
        System.out.println(String.format("Amount Range: %.2f - %.2f", 
            config.getMinAmount().doubleValue(), 
            config.getMaxAmount().doubleValue()));
        System.out.println("=" + "=".repeat(60));
    }
}
