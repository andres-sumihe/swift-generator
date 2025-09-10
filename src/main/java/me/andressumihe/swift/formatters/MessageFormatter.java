package me.andressumihe.swift.formatters;

import me.andressumihe.swift.model.enums.OutputFormat;
import me.andressumihe.swift.exceptions.FormatException;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Abstract base class for SWIFT message formatters.
 */
public abstract class MessageFormatter {
    
    protected static final Logger logger = Logger.getLogger(MessageFormatter.class.getName());
    
    // Common format settings
    protected final OutputFormat outputFormat;
    protected final Map<String, Object> formatConfiguration;
    
    // Statistics tracking
    protected long formattedMessages = 0;
    protected long totalProcessingTime = 0;
    
    protected MessageFormatter(OutputFormat outputFormat, Map<String, Object> formatConfiguration) {
        this.outputFormat = outputFormat;
        this.formatConfiguration = formatConfiguration != null ? formatConfiguration : Map.of();
        logger.info("Initialized " + outputFormat + " formatter");
    }

    public final String formatMessage(Object swiftMessage) throws FormatException {
        if (swiftMessage == null) {
            throw new FormatException("SWIFT message cannot be null");
        }
        
        long startTime = System.nanoTime();
        try {
            // Validate message before formatting
            validateMessage(swiftMessage);
            
            // Perform format-specific processing
            String formattedMessage = doFormatMessage(swiftMessage);
            
            // Post-process and sanitize
            String result = postProcessMessage(formattedMessage);
            
            // Update statistics
            formattedMessages++;
            totalProcessingTime += (System.nanoTime() - startTime);
            
            return result;
            
        } catch (Exception e) {
            logger.severe("Failed to format message: " + e.getMessage());
            throw new FormatException("Message formatting failed: " + e.getMessage(), e);
        }
    }
    
    public final String formatBatch(List<?> swiftMessages) throws FormatException {
        if (swiftMessages == null || swiftMessages.isEmpty()) {
            throw new FormatException("Message list cannot be null or empty");
        }
        
        long startTime = System.nanoTime();
        try {
            logger.info("Formatting batch of " + swiftMessages.size() + " messages in " + outputFormat + " format");
            
            // Perform batch-specific initialization
            BatchFormatContext context = initializeBatchFormatting(swiftMessages.size());
            
            // Format each message and collect results
            StringBuilder batchResult = new StringBuilder();
            for (int i = 0; i < swiftMessages.size(); i++) {
                Object message = swiftMessages.get(i);
                
                // Format individual message
                String formattedMessage = formatMessage(message);
                
                // Add to batch with format-specific delimiters/processing
                appendMessageToBatch(batchResult, formattedMessage, i, context);
            }
            
            // Finalize batch formatting
            String result = finalizeBatchFormatting(batchResult.toString(), context);
            
            long duration = System.nanoTime() - startTime;
            logger.info("Batch formatting completed in " + (duration / 1_000_000) + "ms");
            
            return result;
            
        } catch (Exception e) {
            logger.severe("Failed to format batch: " + e.getMessage());
            throw new FormatException("Batch formatting failed: " + e.getMessage(), e);
        }
    }
    
    public FormatterStatistics getStatistics() {
        double averageTime = formattedMessages > 0 
            ? (double) totalProcessingTime / formattedMessages / 1_000_000 
            : 0.0;
        
        return new FormatterStatistics(
            outputFormat,
            formattedMessages,
            totalProcessingTime / 1_000_000, // Convert to milliseconds
            averageTime
        );
    }

    public void resetStatistics() {
        formattedMessages = 0;
        totalProcessingTime = 0;
        logger.fine("Statistics reset for " + outputFormat + " formatter");
    }
    
    /**
     * Get the output format supported by this formatter
     */
    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public Map<String, Object> getConfiguration() {
        return formatConfiguration;
    }

    protected abstract void validateMessage(Object swiftMessage) throws FormatException;

    protected abstract String doFormatMessage(Object swiftMessage) throws FormatException;
    
    /**
     * Initialize batch formatting context
     */
    protected abstract BatchFormatContext initializeBatchFormatting(int messageCount) throws FormatException;
    
    protected abstract void appendMessageToBatch(StringBuilder batchResult, String formattedMessage, 
                                               int messageIndex, BatchFormatContext context) throws FormatException;

    protected abstract String finalizeBatchFormatting(String batchContent, BatchFormatContext context) throws FormatException;

    protected String postProcessMessage(String formattedMessage) throws FormatException {
        if (formattedMessage == null || formattedMessage.trim().isEmpty()) {
            throw new FormatException("Formatted message is null or empty");
        }
        
        // Common post-processing steps
        String processed = formattedMessage;
        
        // Remove any null characters
        processed = processed.replace("\0", "");
        
        // Normalize line endings based on format requirements
        processed = normalizeLineEndings(processed);
        
        // Apply format-specific character set validation
        validateCharacterSet(processed);
        
        return processed;
    }

    protected String normalizeLineEndings(String content) {
        return content.replace("\r\n", "\n").replace("\r", "\n");
    }

    protected void validateCharacterSet(String content) throws FormatException {
        if (content.contains("\0")) {
            throw new FormatException("Message contains null characters");
        }
    }
    
    /**
     * Get configuration value with default
     */
    protected <T> T getConfigurationValue(String key, T defaultValue) {
        @SuppressWarnings("unchecked")
        T value = (T) formatConfiguration.get(key);
        return value != null ? value : defaultValue;
    }

    public static class BatchFormatContext {
        private final int totalMessages;
        private final Map<String, Object> contextData;
        private final long startTime;
        
        public BatchFormatContext(int totalMessages, Map<String, Object> contextData) {
            this.totalMessages = totalMessages;
            this.contextData = contextData != null ? contextData : Map.of();
            this.startTime = System.currentTimeMillis();
        }
        
        public int getTotalMessages() { return totalMessages; }
        public Map<String, Object> getContextData() { return contextData; }
        public long getStartTime() { return startTime; }
        public long getElapsedTime() { return System.currentTimeMillis() - startTime; }
        
        public <T> T getContextValue(String key, T defaultValue) {
            @SuppressWarnings("unchecked")
            T value = (T) contextData.get(key);
            return value != null ? value : defaultValue;
        }
    }
    
    /**
     * Formatter statistics container
     */
    public static class FormatterStatistics {
        private final OutputFormat format;
        private final long messagesFormatted;
        private final long totalTimeMs;
        private final double averageTimeMs;
        
        public FormatterStatistics(OutputFormat format, long messagesFormatted, 
                                 long totalTimeMs, double averageTimeMs) {
            this.format = format;
            this.messagesFormatted = messagesFormatted;
            this.totalTimeMs = totalTimeMs;
            this.averageTimeMs = averageTimeMs;
        }
        
        public OutputFormat getFormat() { return format; }
        public long getMessagesFormatted() { return messagesFormatted; }
        public long getTotalTimeMs() { return totalTimeMs; }
        public double getAverageTimeMs() { return averageTimeMs; }
        
        public double getMessagesPerSecond() {
            return totalTimeMs > 0 ? (messagesFormatted * 1000.0) / totalTimeMs : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("FormatterStats{format=%s, messages=%d, totalTime=%dms, avgTime=%.2fms, rate=%.1f msg/s}", 
                format, messagesFormatted, totalTimeMs, averageTimeMs, getMessagesPerSecond());
        }
    }
}
