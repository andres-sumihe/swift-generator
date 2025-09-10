package me.andressumihe.swift.service;

import com.prowidesoftware.swift.model.mt.AbstractMT;
import me.andressumihe.swift.cli.CommandLineOptions;
import me.andressumihe.swift.exceptions.MessageGenerationException;

import java.util.List;

/**
 * Service interface for SWIFT message generation operations.
 */
public interface SwiftMessageService {
    
    List<AbstractMT> generateMessages(CommandLineOptions options) throws MessageGenerationException;

    String formatMessages(List<AbstractMT> messages) throws Exception;

    String[] writeToFiles(String formattedContent, CommandLineOptions options) throws Exception;

    void displayGenerationParameters(CommandLineOptions options);
}