package me.andressumihe.swift.model.enums;

/**
 * SWIFT message output formats.
 */
public enum Format {
    
    RJE("RJE", "Remote Job Entry", "rje"),
    
    FIN("FIN", "Financial Application", "fin"),
    
    DOS_PCC("DOS_PCC", "DOS Personal Computer Card", "dos");
    
    private final String code;
    private final String description;
    private final String fileExtension;
    
    Format(String code, String description, String fileExtension) {
        this.code = code;
        this.description = description;
        this.fileExtension = fileExtension;
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

    public static Format fromString(String formatStr) {
        if (formatStr == null || formatStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Format cannot be null or empty");
        }
        
        String normalized = formatStr.trim().toUpperCase();
        
        switch (normalized) {
            case "RJE":
            case "REMOTE_JOB_ENTRY":
            case "BATCH":
                return RJE;
                
            case "FIN":
            case "FINANCIAL":
            case "STANDARD":
            case "SWIFT":
                return FIN;
                
            case "DOS_PCC":
            case "DOS":
            case "PCC":
            case "PC":
                return DOS_PCC;
                
            default:
                throw new IllegalArgumentException("Invalid format: " + formatStr + 
                    ". Valid values are: RJE, FIN, DOS_PCC");
        }
    }

    public static String getSupportedFormats() {
        return "RJE, FIN, DOS_PCC";
    }
    
    @Override
    public String toString() {
        return code;
    }
}
