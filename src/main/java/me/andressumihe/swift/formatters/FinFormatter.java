package me.andressumihe.swift.formatters;

import java.util.HashMap;
import java.util.Map;

import com.prowidesoftware.swift.model.mt.AbstractMT;

import me.andressumihe.swift.exceptions.FormatException;
import me.andressumihe.swift.model.enums.OutputFormat;

/**
 * Formatter for standard SWIFT FIN format messages.
 */
public class FinFormatter extends MessageFormatter {
    
    // Configuration keys
    private static final String CONFIG_INCLUDE_HEADERS = "includeHeaders";
    private static final String CONFIG_INCLUDE_TRAILERS = "includeTrailers";
    private static final String CONFIG_LINE_ENDINGS = "lineEndings";
    private static final String CONFIG_VALIDATE_CHARSET = "validateCharset";
    
    // Default configuration values
    private static final Map<String, Object> DEFAULT_CONFIG = Map.of(
        CONFIG_INCLUDE_HEADERS, true,
        CONFIG_INCLUDE_TRAILERS, true,
        CONFIG_LINE_ENDINGS, "LF",
        CONFIG_VALIDATE_CHARSET, true
    );
    
    /**
     * Constructor with custom configuration
     */
    public FinFormatter(Map<String, Object> configuration) {
        super(OutputFormat.FIN, mergeWithDefaults(configuration));
        logger.info("FinFormatter initialized with configuration: " + this.formatConfiguration);
    }
    
    /**
     * Constructor with default configuration
     */
    public FinFormatter() {
        this(null);
    }
    
    @Override
    protected void validateMessage(Object swiftMessage) throws FormatException {
        if (swiftMessage == null) {
            throw new FormatException("SWIFT message cannot be null");
        }
        
        // Check if message is a supported type
        if (!(swiftMessage instanceof AbstractMT)) {
            throw new FormatException("Message must be an instance of AbstractMT, got: " + 
                swiftMessage.getClass().getSimpleName());
        }
        
        AbstractMT abstractMT = (AbstractMT) swiftMessage;
        
        // Basic message validation
        if (abstractMT == null) {
            throw new FormatException("SWIFT message is null");
        }
        
        // Additional FIN-specific validations
        validateFinSpecificRules(abstractMT);
    }
    
    @Override
    protected String doFormatMessage(Object swiftMessage) throws FormatException {
        AbstractMT abstractMT = (AbstractMT) swiftMessage;
        
        try {
            // Use Prowide Core's native FIN formatting
            String finMessage = abstractMT.message();
            
            if (finMessage == null || finMessage.trim().isEmpty()) {
                throw new FormatException("Generated FIN message is null or empty");
            }
            
            // Apply FIN-specific formatting enhancements
            return enhanceFinFormatting(finMessage);
            
        } catch (Exception e) {
            throw new FormatException("Failed to format message as FIN: " + e.getMessage(), e);
        }
    }
    
    @Override
    protected BatchFormatContext initializeBatchFormatting(int messageCount) throws FormatException {
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("messageCount", messageCount);
        contextData.put("currentIndex", 0);
        contextData.put("batchStartTime", System.currentTimeMillis());
        
        logger.info("Initializing FIN batch formatting for " + messageCount + " messages");
        return new BatchFormatContext(messageCount, contextData);
    }
    
    @Override
    protected void appendMessageToBatch(StringBuilder batchResult, String formattedMessage, 
                                      int messageIndex, BatchFormatContext context) throws FormatException {
        
        // For FIN format, messages are typically separated by line breaks
        if (messageIndex > 0) {
            batchResult.append(getLineEnding());
            batchResult.append(getLineEnding()); // Extra line break between messages
        }
        
        // Add message with optional index comment for batch identification
        if (getConfigurationValue("includeBatchComments", false)) {
            batchResult.append("// Message ").append(messageIndex + 1).append(" of ")
                      .append(context.getTotalMessages()).append(getLineEnding());
        }
        
        batchResult.append(formattedMessage);
        
        // Update context
        context.getContextData().put("currentIndex", messageIndex + 1);
    }
    
    @Override
    protected String finalizeBatchFormatting(String batchContent, BatchFormatContext context) throws FormatException {
        StringBuilder finalContent = new StringBuilder();
        
        // Add batch header if configured
        if (getConfigurationValue("includeBatchHeader", false)) {
            finalContent.append("// SWIFT FIN Format Batch").append(getLineEnding());
            finalContent.append("// Generated: ").append(new java.util.Date()).append(getLineEnding());
            finalContent.append("// Messages: ").append(context.getTotalMessages()).append(getLineEnding());
            finalContent.append("// Processing time: ").append(context.getElapsedTime()).append("ms").append(getLineEnding());
            finalContent.append(getLineEnding());
        }
        
        finalContent.append(batchContent);
        
        // Add batch trailer if configured
        if (getConfigurationValue("includeBatchTrailer", false)) {
            finalContent.append(getLineEnding()).append(getLineEnding());
            finalContent.append("// End of batch - ").append(context.getTotalMessages()).append(" messages processed");
        }
        
        long processingTime = context.getElapsedTime();
        logger.info("FIN batch formatting completed in " + processingTime + "ms");
        
        return finalContent.toString();
    }
    
    @Override
    protected String normalizeLineEndings(String content) {
        String lineEndingStyle = getConfigurationValue(CONFIG_LINE_ENDINGS, "LF");
        
        switch (lineEndingStyle.toUpperCase()) {
            case "CRLF":
                return content.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\r\n");
            case "LF":
            default:
                return content.replace("\r\n", "\n").replace("\r", "\n");
        }
    }
    
    @Override
    protected void validateCharacterSet(String content) throws FormatException {
        if (!getConfigurationValue(CONFIG_VALIDATE_CHARSET, true)) {
            return; // Skip validation if disabled
        }
        
        super.validateCharacterSet(content);
        
        // FIN-specific character set validation
        // SWIFT FIN uses a specific subset of ASCII characters
        for (char c : content.toCharArray()) {
            if (!isValidFinCharacter(c)) {
                throw new FormatException("Invalid character in FIN format: '" + c + "' (0x" + 
                    Integer.toHexString(c) + ")");
            }
        }
    }
    
    /**
     * Validate FIN-specific business rules
     */
    private void validateFinSpecificRules(AbstractMT message) throws FormatException {
        // Check message type is supported
        String messageType = message.getMessageType();
        if (messageType == null || messageType.trim().isEmpty()) {
            throw new FormatException("Message type is required for FIN format");
        }
        
        // Validate message type format (should be like "103", "202", etc.)
        if (!messageType.matches("\\d{3}")) {
            throw new FormatException("Invalid message type format: " + messageType);
        }
        
        // Check for required fields based on message type
        validateRequiredFields(message);
    }
    
    /**
     * Validate required fields for the message
     */
    private void validateRequiredFields(AbstractMT message) throws FormatException {
        // Basic validation - override in subclasses for message-type specific validation
        if (message.getSwiftMessage() == null) {
            throw new FormatException("Message content is null");
        }
        
        if (message.getSwiftMessage().getBlock4() == null || 
            message.getSwiftMessage().getBlock4().isEmpty()) {
            throw new FormatException("Message body (Block 4) is empty");
        }
    }
    
    /**
     * Enhance FIN formatting with additional processing
     */
    private String enhanceFinFormatting(String finMessage) {
        String enhanced = finMessage;
        
        // Add headers if configured
        if (getConfigurationValue(CONFIG_INCLUDE_HEADERS, true)) {
            enhanced = addFinHeaders(enhanced);
        }
        
        // Add trailers if configured
        if (getConfigurationValue(CONFIG_INCLUDE_TRAILERS, true)) {
            enhanced = addFinTrailers(enhanced);
        }
        
        return enhanced;
    }
    
    /**
     * Add FIN headers if not already present
     */
    private String addFinHeaders(String finMessage) {
        // Check if headers are already present
        if (finMessage.startsWith("{1:") || finMessage.startsWith("{")) {
            return finMessage; // Headers already present
        }
        
        // For testing purposes, we'll keep the message as-is since 
        // Prowide Core already handles header formatting
        return finMessage;
    }
    
    /**
     * Add FIN trailers if not already present
     */
    private String addFinTrailers(String finMessage) {
        // Similar to headers, Prowide Core handles trailers
        return finMessage;
    }
    
    /**
     * Check if character is valid in SWIFT FIN format
     */
    private boolean isValidFinCharacter(char c) {
        // SWIFT FIN character set includes:
        // - Letters A-Z
        // - Digits 0-9
        // - Special characters: space, ., ,, /, -, ?, :, (, ), +, ', {, }
        // - Control characters: CR, LF
        
        if (c >= 'A' && c <= 'Z') return true;
        if (c >= '0' && c <= '9') return true;
        if (c == ' ' || c == '.' || c == ',' || c == '/' || c == '-') return true;
        if (c == '?' || c == ':' || c == '(' || c == ')' || c == '+') return true;
        if (c == '\'' || c == '{' || c == '}') return true;
        if (c == '\r' || c == '\n') return true;
        
        return false;
    }
    
    /**
     * Get line ending based on configuration
     */
    private String getLineEnding() {
        String lineEndingStyle = getConfigurationValue(CONFIG_LINE_ENDINGS, "LF");
        return "CRLF".equalsIgnoreCase(lineEndingStyle) ? "\r\n" : "\n";
    }
    
    /**
     * Merge custom configuration with defaults
     */
    private static Map<String, Object> mergeWithDefaults(Map<String, Object> customConfig) {
        Map<String, Object> merged = new HashMap<>(DEFAULT_CONFIG);
        if (customConfig != null) {
            merged.putAll(customConfig);
        }
        return merged;
    }
}
