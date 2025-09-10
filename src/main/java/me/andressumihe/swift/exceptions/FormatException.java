package me.andressumihe.swift.exceptions;

import me.andressumihe.swift.model.enums.OutputFormat;

/**
 * Exception thrown when message formatting operations fail.
 */
public class FormatException extends Exception {
    
    private final OutputFormat targetFormat;
    
    public FormatException(String message) {
        super(message);
        this.targetFormat = null;
    }
    
    public FormatException(String message, OutputFormat targetFormat) {
        super(message);
        this.targetFormat = targetFormat;
    }
    
    public FormatException(String message, Throwable cause) {
        super(message, cause);
        this.targetFormat = null;
    }
    
    public FormatException(String message, Throwable cause, OutputFormat targetFormat) {
        super(message, cause);
        this.targetFormat = targetFormat;
    }
    
    public OutputFormat getTargetFormat() {
        return targetFormat;
    }
}
