package me.andressumihe.swift.formatters;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.prowidesoftware.swift.model.mt.AbstractMT;

import me.andressumihe.swift.config.ConfigurationManager;
import me.andressumihe.swift.exceptions.FormatException;
import me.andressumihe.swift.model.enums.OutputFormat;

/**
 * Formatter for SWIFT DOS-PCC format messages.
 */
public class DosPccFormatter extends MessageFormatter {
    
    // Configuration keys
    private static final String CONFIG_SECTOR_SIZE = "sectorSize";
    private static final String CONFIG_START_MARKER = "startMarker";
    private static final String CONFIG_END_MARKER = "endMarker";
    private static final String CONFIG_PADDING_BYTE = "paddingByte";
    private static final String CONFIG_INCLUDE_HEX_DEBUG = "includeHexDebug";
    private static final String CONFIG_ENCODING = "encoding";
    private static final String CONFIG_VALIDATE_BINARY = "validateBinary";
    
    // Default configuration values loaded from application.properties
    private static final Map<String, Object> DEFAULT_CONFIG = createDefaultConfig();
    
    private static Map<String, Object> createDefaultConfig() {
        ConfigurationManager config = ConfigurationManager.getInstance();
        return Map.of(
            CONFIG_SECTOR_SIZE, config.getDosPccSectorSize(),
            CONFIG_START_MARKER, config.getDosPccStartMarker(),
            CONFIG_END_MARKER, config.getDosPccEndMarker(),
            CONFIG_PADDING_BYTE, config.getDosPccPaddingByte(),
            CONFIG_INCLUDE_HEX_DEBUG, false,
            CONFIG_ENCODING, config.getDosPccEncoding(),
            CONFIG_VALIDATE_BINARY, true
        );
    }
    
    /**
     * Constructor with custom configuration
     */
    public DosPccFormatter(Map<String, Object> configuration) {
        super(OutputFormat.DOS_PCC, mergeWithDefaults(configuration));
        logger.info("DosPccFormatter initialized with configuration: " + this.formatConfiguration);
    }
    
    /**
     * Constructor with default configuration
     */
    public DosPccFormatter() {
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
        
        // DOS-PCC specific validations
        validateDosPccSpecificRules(abstractMT);
    }
    
    @Override
    protected String doFormatMessage(Object swiftMessage) throws FormatException {
        AbstractMT abstractMT = (AbstractMT) swiftMessage;
        
        try {
            // Check if this is an incoming message that needs network formatting
            String finMessage;
            if (isIncomingNetworkMessage(abstractMT)) {
                // Apply SWIFT network format with F21 headers and Block 5
                finMessage = applyIncomingNetworkFormat(abstractMT);
            } else {
                // Get base FIN format message
                finMessage = abstractMT.message();
            }
            
            if (finMessage == null || finMessage.trim().isEmpty()) {
                throw new FormatException("Generated FIN message is null or empty");
            }
            
            // Convert to DOS-PCC binary format
            byte[] binaryData = convertToDosPccFormat(finMessage);
            
            // Return as string representation (hex or binary string based on config)
            return formatBinaryOutput(binaryData);
            
        } catch (Exception e) {
            throw new FormatException("Failed to format message as DOS-PCC: " + e.getMessage(), e);
        }
    }
    
    @Override
    protected BatchFormatContext initializeBatchFormatting(int messageCount) throws FormatException {
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("messageCount", messageCount);
        contextData.put("currentIndex", 0);
        contextData.put("batchStartTime", System.currentTimeMillis());
        contextData.put("totalSectors", 0);
        contextData.put("binaryDataSize", 0);
        
        logger.info("Initializing DOS-PCC batch formatting for " + messageCount + " messages");
        return new BatchFormatContext(messageCount, contextData);
    }
    
    @Override
    protected void appendMessageToBatch(StringBuilder batchResult, String formattedMessage, 
                                      int messageIndex, BatchFormatContext context) throws FormatException {
        
        // Get the raw SWIFT message content (without DOS-PCC formatting)
        // The formattedMessage from doFormatMessage contains the 0x01/0x03 markers already
        // But we need to handle sector boundaries properly for the batch
        
        // For DOS-PCC batch, we need to place each message at sector boundaries
        int sectorSize = getConfigurationValue(CONFIG_SECTOR_SIZE, 512);
        int paddingByte = getConfigurationValue(CONFIG_PADDING_BYTE, 0x00);
        
        // Convert the formatted message back to bytes to work with binary data
        byte[] messageBytes = formattedMessage.getBytes(StandardCharsets.ISO_8859_1);
        
        // Calculate current position in batch (based on what's already in batchResult)
        int currentBatchSize = batchResult.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        
        // Calculate next sector boundary
        int currentSector = currentBatchSize / sectorSize;
        int nextSectorStart = (currentSector + 1) * sectorSize;
        
        // If this is not the first message and we're not at a sector boundary, pad to next sector
        if (messageIndex > 0 && currentBatchSize % sectorSize != 0) {
            int paddingNeeded = nextSectorStart - currentBatchSize;
            byte[] padding = new byte[paddingNeeded];
            Arrays.fill(padding, (byte) paddingByte);
            
            // Add padding to reach sector boundary
            batchResult.append(new String(padding, StandardCharsets.ISO_8859_1));
        }
        
        // Add the message (which already has 0x01 prefix and 0x03 suffix)
        batchResult.append(formattedMessage);
        
        // Update context with sector information
        int newBatchSize = batchResult.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        int totalSectors = (newBatchSize + sectorSize - 1) / sectorSize;
        
        context.getContextData().put("currentIndex", messageIndex + 1);
        context.getContextData().put("totalSectors", totalSectors);
        context.getContextData().put("binaryDataSize", newBatchSize);
        
        logger.info(String.format("Message %d added: batch size now %d bytes (%d sectors)", 
            messageIndex + 1, newBatchSize, totalSectors));
    }

    @Override
    protected String finalizeBatchFormatting(String batchContent, BatchFormatContext context) throws FormatException {
        // Ensure the final batch is padded to complete the last sector
        int sectorSize = getConfigurationValue(CONFIG_SECTOR_SIZE, 512);
        int paddingByte = getConfigurationValue(CONFIG_PADDING_BYTE, 0x00);
        
        byte[] batchBytes = batchContent.getBytes(StandardCharsets.ISO_8859_1);
        int currentSize = batchBytes.length;
        int remainder = currentSize % sectorSize;
        
        if (remainder != 0) {
            int paddingNeeded = sectorSize - remainder;
            StringBuilder finalContent = new StringBuilder(batchContent);
            
            byte[] finalPadding = new byte[paddingNeeded];
            Arrays.fill(finalPadding, (byte) paddingByte);
            finalContent.append(new String(finalPadding, StandardCharsets.ISO_8859_1));
            
            int finalSize = finalContent.toString().getBytes(StandardCharsets.ISO_8859_1).length;
            int finalSectors = finalSize / sectorSize;
            
            logger.info(String.format("DOS-PCC batch finalized: %d bytes in %d complete sectors (added %d padding bytes)", 
                finalSize, finalSectors, paddingNeeded));
            
            return finalContent.toString();
        }
        
        int sectors = currentSize / sectorSize;
        logger.info(String.format("DOS-PCC batch finalized: %d bytes in %d complete sectors (no padding needed)", 
            currentSize, sectors));
        
        return batchContent;
    }
    
    @Override
    protected String normalizeLineEndings(String content) {
        // DOS-PCC format should use Windows CRLF line endings (0D 0A) for Windows servers
        return content.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\r\n");
    }
    
    @Override
    protected void validateCharacterSet(String content) throws FormatException {
        if (!getConfigurationValue(CONFIG_VALIDATE_BINARY, true)) {
            return; // Skip validation if disabled
        }
        
        super.validateCharacterSet(content);
        
        // For DOS-PCC, we need to ensure content can be properly encoded
        String encoding = getConfigurationValue(CONFIG_ENCODING, "ASCII");
        
        try {
            byte[] encoded = content.getBytes(encoding);
            String decoded = new String(encoded, encoding);
            
            if (!content.equals(decoded)) {
                throw new FormatException("Content contains characters not compatible with " + encoding + " encoding");
            }
        } catch (Exception e) {
            throw new FormatException("Character encoding validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate DOS-PCC specific business rules
     */
    private void validateDosPccSpecificRules(AbstractMT message) throws FormatException {
        // Check message type
        String messageType = message.getMessageType();
        if (messageType == null || messageType.trim().isEmpty()) {
            throw new FormatException("Message type is required for DOS-PCC format");
        }
        
        // Validate sector size configuration
        int sectorSize = getConfigurationValue(CONFIG_SECTOR_SIZE, 512);
        if (sectorSize <= 0 || sectorSize > 65536) {
            throw new FormatException("Invalid sector size: " + sectorSize + " (must be 1-65536)");
        }
        
        // Validate marker configuration
        int startMarker = getConfigurationValue(CONFIG_START_MARKER, 0x01);
        int endMarker = getConfigurationValue(CONFIG_END_MARKER, 0x03);
        
        if (startMarker < 0 || startMarker > 255) {
            throw new FormatException("Invalid start marker: " + startMarker + " (must be 0-255)");
        }
        if (endMarker < 0 || endMarker > 255) {
            throw new FormatException("Invalid end marker: " + endMarker + " (must be 0-255)");
        }
        if (startMarker == endMarker) {
            throw new FormatException("Start and end markers cannot be the same");
        }
    }
    
    /**
     * Convert FIN message to DOS-PCC binary format
     */
    private byte[] convertToDosPccFormat(String finMessage) throws FormatException {
        String encoding = getConfigurationValue(CONFIG_ENCODING, "ASCII");
        int startMarker = getConfigurationValue(CONFIG_START_MARKER, 0x01);
        int endMarker = getConfigurationValue(CONFIG_END_MARKER, 0x03);
        int sectorSize = getConfigurationValue(CONFIG_SECTOR_SIZE, 512);
        int paddingByte = getConfigurationValue(CONFIG_PADDING_BYTE, 0x00);
        
        try {
            // Convert message content to bytes
            byte[] messageBytes = finMessage.getBytes(encoding);
            
            // Calculate total size: start marker + content + end marker
            int contentSize = 1 + messageBytes.length + 1;
            
            // Calculate sectors needed and total size with padding
            int sectorsNeeded = (contentSize + sectorSize - 1) / sectorSize; // Ceiling division
            int totalSize = sectorsNeeded * sectorSize;
            
            // Create binary data array
            byte[] binaryData = new byte[totalSize];
            Arrays.fill(binaryData, (byte) paddingByte); // Initialize with padding
            
            int offset = 0;
            
            // Add start marker
            binaryData[offset++] = (byte) startMarker;
            
            // Add message content
            System.arraycopy(messageBytes, 0, binaryData, offset, messageBytes.length);
            offset += messageBytes.length;
            
            // Add end marker
            binaryData[offset] = (byte) endMarker;
            
            // Remaining bytes are already filled with padding
            
            logger.info(String.format("DOS-PCC binary data created: %d bytes in %d sectors (content: %d bytes, padding: %d bytes)", 
                totalSize, sectorsNeeded, contentSize, totalSize - contentSize));
            return binaryData;
            
        } catch (Exception e) {
            throw new FormatException("Failed to convert to DOS-PCC binary format: " + e.getMessage(), e);
        }
    }
    
    /**
     * Format binary output based on configuration
     */
    private String formatBinaryOutput(byte[] binaryData) {
        boolean includeHexDebug = getConfigurationValue(CONFIG_INCLUDE_HEX_DEBUG, false);
        
        if (includeHexDebug) {
            return formatAsHexDump(binaryData);
        } else {
            // Return as raw binary string (ISO-8859-1 preserves all byte values)
            // This allows the binary data to pass through text processing without corruption
            return new String(binaryData, StandardCharsets.ISO_8859_1);
        }
    }
    
    /**
     * Format binary data as hexadecimal dump with ASCII representation
     */
    private String formatAsHexDump(byte[] binaryData) {
        StringBuilder hexDump = new StringBuilder();
        int sectorSize = getConfigurationValue(CONFIG_SECTOR_SIZE, 512);
        
        for (int i = 0; i < binaryData.length; i += 16) {
            // Add sector boundary markers
            if (i % sectorSize == 0) {
                hexDump.append(String.format("\n// Sector %d (offset 0x%04X)\n", i / sectorSize, i));
            }
            
            // Address
            hexDump.append(String.format("%04X: ", i));
            
            // Hex bytes
            for (int j = 0; j < 16; j++) {
                if (i + j < binaryData.length) {
                    hexDump.append(String.format("%02X ", binaryData[i + j] & 0xFF));
                } else {
                    hexDump.append("   ");
                }
            }
            
            // ASCII representation
            hexDump.append(" |");
            for (int j = 0; j < 16; j++) {
                if (i + j < binaryData.length) {
                    byte b = binaryData[i + j];
                    if (b >= 32 && b <= 126) {
                        hexDump.append((char) b);
                    } else {
                        hexDump.append('.');
                    }
                } else {
                    hexDump.append(' ');
                }
            }
            hexDump.append("|\n");
        }
        
        return hexDump.toString();
    }
    
    /**
     * Calculate binary size from formatted message string
     */
    private int calculateBinarySize(String formattedMessage) {
        // If it's base64, decode and get actual size
        if (!getConfigurationValue(CONFIG_INCLUDE_HEX_DEBUG, false)) {
            try {
                byte[] decoded = java.util.Base64.getDecoder().decode(formattedMessage);
                return decoded.length;
            } catch (Exception e) {
                // Fallback to string length estimation
                return formattedMessage.length();
            }
        } else {
            // For hex dump, estimate based on content
            // This is approximate since hex dump includes formatting
            return formattedMessage.length() / 3; // Rough estimate
        }
    }
    
    /**
     * Get binary data from formatted output (utility method)
     */
    public byte[] getBinaryData(String formattedOutput) throws FormatException {
        boolean includeHexDebug = getConfigurationValue(CONFIG_INCLUDE_HEX_DEBUG, false);
        
        if (includeHexDebug) {
            throw new FormatException("Cannot extract binary data from hex dump format");
        } else {
            try {
                return java.util.Base64.getDecoder().decode(formattedOutput);
            } catch (Exception e) {
                throw new FormatException("Failed to decode binary data: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Write binary data to file (utility method)
     */
    public void writeBinaryToFile(String formattedOutput, String filename) throws FormatException {
        try {
            byte[] binaryData = getBinaryData(formattedOutput);
            java.nio.file.Files.write(java.nio.file.Paths.get(filename), binaryData);
            logger.info("DOS-PCC binary data written to: " + filename + " (" + binaryData.length + " bytes)");
        } catch (Exception e) {
            throw new FormatException("Failed to write binary file: " + e.getMessage(), e);
        }
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
            // Maintain consistent CRLF line endings to avoid mixed line ending issues
            baseMessage = baseMessage.replaceAll("\\r+\\r?\\n-\\}", "\r\n-}");
            
            // Apply the network formatting that was originally in createIncomingMessageWithBlock5
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
     * Extract specific block from SWIFT message
     */
    private String extractBlock(String message, String blockNumber) {
        try {
            // Use proper balanced bracket matching for nested structures like Block 3
            String blockStart = "{" + blockNumber + ":";
            int startIndex = message.indexOf(blockStart);
            if (startIndex == -1) {
                return null;
            }
            
            // Start counting brackets from after the block start pattern
            int openBrackets = 1; // We've seen the opening bracket of this block
            int currentIndex = startIndex + blockStart.length();
            
            while (currentIndex < message.length() && openBrackets > 0) {
                char c = message.charAt(currentIndex);
                if (c == '{') {
                    openBrackets++;
                } else if (c == '}') {
                    openBrackets--;
                }
                currentIndex++;
            }
            
            if (openBrackets == 0) {
                // Found the complete block with properly balanced brackets
                return message.substring(startIndex, currentIndex);
            }
            
        } catch (Exception e) {
            // Ignore extraction errors
        }
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
