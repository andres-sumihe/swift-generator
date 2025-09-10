package me.andressumihe.swift.model.enums;

/**
 * Supported SWIFT message types.
 */
public enum MessageType {
    
    MT101("101", "Request for Transfer"),
    
    MT103("103", "Single Customer Credit Transfer"),
    
    MT202("202", "General Financial Institution Transfer"),
    
    MT940("940", "Customer Statement Message"),
    
    MT950("950", "Statement Message");
    
    private final String code;
    private final String description;
    
    /**
     * Constructs a MessageType with code and description.
     * 
     * @param code The SWIFT message type code (e.g., "103", "202")
     * @param description Human-readable description of the message type
     */
    MessageType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MessageType fromCode(String code) {
        for (MessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type code: " + code);
    }
    
    public static boolean isValidCode(String code) {
        try {
            fromCode(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String getCategory() {
        return switch (this) {
            case MT101 -> "Customer Payments and Cheques";
            case MT103 -> "Customer Payments and Cheques";
            case MT202 -> "Financial Institution Transfers";
            case MT940 -> "Customer Statement and Reporting Messages";
            case MT950 -> "Customer Statement and Reporting Messages";
        };
    }
    
    @Override
    public String toString() {
        return code + " - " + description;
    }
}
