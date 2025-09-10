package me.andressumihe.swift.formatters;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.prowidesoftware.swift.model.field.Field;
import com.prowidesoftware.swift.model.mt.AbstractMT;

import me.andressumihe.swift.exceptions.FormatException;
import me.andressumihe.swift.model.enums.OutputFormat;

/**
 * Formatter for SWIFT RJE format messages.
 */
public class RjeFormatter extends MessageFormatter {
    
    // Configuration keys
    private static final String CONFIG_BATCH_DELIMITER = "batchDelimiter";
    private static final String CONFIG_ENCODING = "encoding";
    private static final String CONFIG_VALIDATE_ASCII = "validateAscii";
    
    // Default configuration values
    private static final Map<String, Object> DEFAULT_CONFIG = Map.of(
        CONFIG_BATCH_DELIMITER, "$",
        CONFIG_ENCODING, "ASCII",
        CONFIG_VALIDATE_ASCII, true
    );
    
    // BIC extraction patterns (removed - not needed for RJE)
    // private static final Pattern BIC_PATTERN = Pattern.compile("([A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?)");
    
    /**
     * Constructor with custom configuration
     */
    public RjeFormatter(Map<String, Object> configuration) {
        super(OutputFormat.RJE, mergeWithDefaults(configuration));
        logger.info("RjeFormatter initialized with configuration: " + this.formatConfiguration);
    }
    
    /**
     * Constructor with default configuration
     */
    public RjeFormatter() {
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
        
        // RJE-specific validations
        validateRjeSpecificRules(abstractMT);
    }
    
    @Override
    protected String doFormatMessage(Object swiftMessage) throws FormatException {
        AbstractMT abstractMT = (AbstractMT) swiftMessage;
        
        try {
            // Get base FIN format message
            String finMessage = abstractMT.message();
            
            if (finMessage == null || finMessage.trim().isEmpty()) {
                throw new FormatException("Generated FIN message is null or empty");
            }
            
            // Check if this is an incoming message that needs network formatting
            if (isIncomingNetworkMessage(abstractMT)) {
                // Apply SWIFT network format with F21 headers and Block 5
                finMessage = applyIncomingNetworkFormat(abstractMT);
            }
            
            // For RJE format, we return the FIN message (with incoming headers if applicable)
            // The $ delimiters are handled at the batch level
            return finMessage;
            
        } catch (Exception e) {
            throw new FormatException("Failed to format message as RJE: " + e.getMessage(), e);
        }
    }
    
    @Override
    protected BatchFormatContext initializeBatchFormatting(int messageCount) throws FormatException {
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("messageCount", messageCount);
        contextData.put("currentIndex", 0);
        contextData.put("batchStartTime", System.currentTimeMillis());
        contextData.put("f21HeaderAdded", false);
        
        logger.info("Initializing RJE batch formatting for " + messageCount + " messages");
        return new BatchFormatContext(messageCount, contextData);
    }
    
    @Override
    protected void appendMessageToBatch(StringBuilder batchResult, String formattedMessage, 
                                      int messageIndex, BatchFormatContext context) throws FormatException {
        
        String delimiter = getConfigurationValue(CONFIG_BATCH_DELIMITER, "$");
        int messageCount = (Integer) context.getContextData().get("messageCount");
        
        // For RJE format, messages are separated by delimiters (BETWEEN messages, not after each)
        if (messageIndex > 0) {
            // Add delimiter BETWEEN messages (no newlines!)
            batchResult.append(delimiter);
        }
        
        // Add the SWIFT message content
        batchResult.append(formattedMessage);
        
        // Update context
        context.getContextData().put("currentIndex", messageIndex + 1);
    }

    @Override
    protected String finalizeBatchFormatting(String batchContent, BatchFormatContext context) throws FormatException {
        // For single message: just return the message content (no delimiter needed)
        // For multiple messages: messages are already separated by $ in appendMessageToBatch
        // No additional delimiter at the end - delimiters are SEPARATORS, not terminators
        
        long processingTime = context.getElapsedTime();
        logger.info("RJE batch formatting completed in " + processingTime + "ms");
        
        return batchContent; // Return as-is, no additional delimiters
    }
    
    @Override
    protected String normalizeLineEndings(String content) {
        // RJE format should use Windows CRLF line endings (0D 0A) for Windows servers
        return content.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\r\n");
    }
    
    @Override
    protected void validateCharacterSet(String content) throws FormatException {
        if (!getConfigurationValue(CONFIG_VALIDATE_ASCII, true)) {
            return; // Skip validation if disabled
        }
        
        super.validateCharacterSet(content);
        
        // ASCII validation for RJE format
        byte[] bytes = content.getBytes(StandardCharsets.US_ASCII);
        String reconstructed = new String(bytes, StandardCharsets.US_ASCII);
        
        if (!content.equals(reconstructed)) {
            throw new FormatException("Content contains non-ASCII characters not suitable for RJE format");
        }
    }
    
    /**
    /**
     * Validate RJE-specific business rules
     */
    private void validateRjeSpecificRules(AbstractMT message) throws FormatException {
        // Check message type
        String messageType = message.getMessageType();
        if (messageType == null || messageType.trim().isEmpty()) {
            throw new FormatException("Message type is required for RJE format");
        }
        
        // Basic message structure validation
        if (message.getSwiftMessage() == null || message.getSwiftMessage().getBlock4() == null) {
            throw new FormatException("Message must have valid SWIFT structure for RJE format");
        }
    }
    
    /**
     * Process FIN content for RJE compatibility
     */
    private String processFinContentForRje(String finMessage) {
        String processed = finMessage;
        
        // Ensure ASCII compatibility
        processed = ensureAsciiCompatibility(processed);
        
        // Normalize line endings
        processed = normalizeLineEndings(processed);
        
        // Remove any problematic characters for RJE transmission
        processed = sanitizeForRje(processed);
        
        return processed;
    }
    
    /**
     * Ensure content is ASCII compatible
     */
    private String ensureAsciiCompatibility(String content) {
        StringBuilder ascii = new StringBuilder();
        
        for (char c : content.toCharArray()) {
            if (c <= 127) { // ASCII character
                ascii.append(c);
            } else {
                // Replace non-ASCII characters with closest ASCII equivalent
                ascii.append(getAsciiReplacement(c));
            }
        }
        
        return ascii.toString();
    }
    
    /**
     * Get ASCII replacement for non-ASCII character
     */
    private char getAsciiReplacement(char nonAscii) {
        // Simple replacements for common extended characters
        switch (nonAscii) {
            case 'ä': case 'à': case 'á': case 'â': case 'ã': return 'a';
            case 'Ä': case 'À': case 'Á': case 'Â': case 'Ã': return 'A';
            case 'ë': case 'è': case 'é': case 'ê': return 'e';
            case 'Ë': case 'È': case 'É': case 'Ê': return 'E';
            case 'ï': case 'ì': case 'í': case 'î': return 'i';
            case 'Ï': case 'Ì': case 'Í': case 'Î': return 'I';
            case 'ö': case 'ò': case 'ó': case 'ô': case 'õ': return 'o';
            case 'Ö': case 'Ò': case 'Ó': case 'Ô': case 'Õ': return 'O';
            case 'ü': case 'ù': case 'ú': case 'û': return 'u';
            case 'Ü': case 'Ù': case 'Ú': case 'Û': return 'U';
            case 'ß': return 's';
            case 'ç': return 'c';
            case 'Ç': return 'C';
            case 'ñ': return 'n';
            case 'Ñ': return 'N';
            default: return '?'; // Unknown character
        }
    }
    
    /**
     * Sanitize content for RJE transmission
     */
    private String sanitizeForRje(String content) {
        String sanitized = content;
        
        // Ensure no embedded null characters
        sanitized = sanitized.replace("\0", "");
        
        return sanitized;
    }
    
    /**
     * Check if this message requires incoming network formatting
     */
    private boolean isIncomingNetworkMessage(AbstractMT message) {
        try {
            // Look for the special marker field 119 with value "NETFMT"
            return message.getFields().stream()
                .anyMatch(field -> "119".equals(field.getName()) && "NETFMT".equals(field.getValue()));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Apply SWIFT incoming network format with F21 headers and Block 5
     */
    private String applyIncomingNetworkFormat(AbstractMT message) {
        try {
            // Get the base message and remove the marker field
            String baseMessage = message.message();
            
            // Remove the marker field from the output (including the newline before it)
            baseMessage = baseMessage.replaceAll("\\n:119:NETFMT", "");
            
            // Clean up any trailing carriage returns and newlines left behind
            // Maintain consistent line endings for RJE format
            baseMessage = baseMessage.replaceAll("\\r+\\r?\\n-\\}", "\n-}");
            
            // Apply the network formatting
            return createIncomingMessageWithBlock5(baseMessage, message.getMessageType());
            
        } catch (Exception e) {
            // Fallback to regular message if network formatting fails
            return message.message();
        }
    }
    
    /**
     * Create incoming message with proper SWIFT network format
     */
    private String createIncomingMessageWithBlock5(String baseMessage, String messageType) {
        StringBuilder incomingMsg = new StringBuilder();
        
        String block1 = extractBlock(baseMessage, "1");
        String block2 = extractBlock(baseMessage, "2");
        String block3 = extractBlock(baseMessage, "3");
        String block4 = extractBlock(baseMessage, "4");
        
        // Create network header (F21 format)
        String sessionNumber = String.format("%010d", System.currentTimeMillis() % 10000000000L);
        String networkHeader = String.format("{1:F21CENAIDJ0AXXX%s}", sessionNumber);
        
        // Create network information block with proper timestamp
        String timestamp = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd")) + "1107";
        String networkInfoBlock = String.format("{4:{177:%s}{451:0}}", timestamp);
        
        // Application header (standard Block 1) 
        String applicationHeader = block1;
        
        // Application header Block 2 (incoming format)
        String applicationBlock2 = createIncomingBlock2(block2, messageType);
        
        // Optional Block 3 (if present)
        String optionalBlock3 = block3 != null ? block3 : "";
        
        // Text Block 4 with message content
        String textBlock4 = block4;
        
        // Block 5 with trailer
        String block5 = "{5:{TNG:}}";
        
        // System Block S
        String blockS = "{S:{SAC:}{COP:P}}";
        
        // Assemble the complete incoming message
        incomingMsg.append(networkHeader)
                  .append(networkInfoBlock)
                  .append(applicationHeader)
                  .append(applicationBlock2)
                  .append(optionalBlock3)
                  .append(textBlock4)
                  .append(block5)
                  .append(blockS);
                  
        return incomingMsg.toString();
    }
    
    /**
     * Create incoming Block 2 format
     */
    private String createIncomingBlock2(String originalBlock2, String messageType) {
        if (originalBlock2 != null && originalBlock2.startsWith("{2:O")) {
            return originalBlock2.replace("{2:O", "{2:I");
        }
        return originalBlock2;
    }
    
    /**
     * Extract a specific SWIFT block from message content using balanced bracket counting
     */
    private String extractBlock(String message, String blockNumber) {
        String blockStart = "{" + blockNumber + ":";
        int startIndex = message.indexOf(blockStart);
        
        if (startIndex == -1) {
            return null;
        }
        
        // Use balanced bracket counting to find the end
        int bracketCount = 0;
        int currentIndex = startIndex;
        
        while (currentIndex < message.length()) {
            char currentChar = message.charAt(currentIndex);
            
            if (currentChar == '{') {
                bracketCount++;
            } else if (currentChar == '}') {
                bracketCount--;
                
                if (bracketCount == 0) {
                    // Found the closing bracket for this block
                    return message.substring(startIndex, currentIndex + 1);
                }
            }
            
            currentIndex++;
        }
        
        // If we get here, brackets were not balanced
        logger.warning("Unbalanced brackets found while extracting block " + blockNumber);
        return null;
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
