package me.andressumihe.swift.model.enums;

/**
 * Supported output formats for SWIFT messages.
 */
public enum OutputFormat {
    
    FIN("FIN", "Standard SWIFT FIN Format", "fin", false),
    
    RJE("RJE", "Remote Job Entry Format", "rje", false),
    
    DOS_PCC("DOS-PCC", "DOS Personal Computer Connection Format", "pcc", true);
    
    private final String code;
    private final String description;
    private final String fileExtension;
    private final boolean binaryFormat;
    
    /**
     * Constructs an OutputFormat with all properties.
     * 
     * @param code The format code identifier
     * @param description Human-readable description
     * @param fileExtension Default file extension for this format
     * @param binaryFormat Whether this format produces binary output
     */
    OutputFormat(String code, String description, String fileExtension, boolean binaryFormat) {
        this.code = code;
        this.description = description;
        this.fileExtension = fileExtension;
        this.binaryFormat = binaryFormat;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
    
    public String getFileExtension() {
        return fileExtension;
    }

    public boolean isBinaryFormat() {
        return binaryFormat;
    }

    public static OutputFormat fromCode(String code) {
        for (OutputFormat format : values()) {
            if (format.code.equalsIgnoreCase(code)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown output format code: " + code);
    }
    
    public static boolean isValidCode(String code) {
        try {
            fromCode(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    @Override
    public String toString() {
        return code + " - " + description;
    }
}
