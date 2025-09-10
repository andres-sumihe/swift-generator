package me.andressumihe.swift.exceptions;

/**
 * Exception thrown when SWIFT message generation fails.
 */
public class MessageGenerationException extends Exception {
    
    private final String messageType;
    
    public MessageGenerationException(String message) {
        super(message);
        this.messageType = null;
    }
    
    public MessageGenerationException(String message, Throwable cause) {
        super(message, cause);
        this.messageType = null;
    }
    
    public MessageGenerationException(String message, String messageType) {
        super(message);
        this.messageType = messageType;
    }
    
    public MessageGenerationException(String message, String messageType, Throwable cause) {
        super(message, cause);
        this.messageType = messageType;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    @Override
    public String getMessage() {
        if (messageType != null) {
            return String.format("Failed to generate MT%s: %s", messageType, super.getMessage());
        }
        return super.getMessage();
    }
}