package me.andressumihe.swift.model.enums;

/**
 * SWIFT message direction for block 2 formatting.
 */
public enum Direction {
    
    OUTGOING("I", "Input"),
    
    INCOMING("O", "Output");
    
    private final String blockPrefix;
    private final String description;
    
    Direction(String blockPrefix, String description) {
        this.blockPrefix = blockPrefix;
        this.description = description;
    }

    public String getBlockPrefix() {
        return blockPrefix;
    }

    public String getDescription() {
        return description;
    }

    public static Direction fromString(String directionStr) {
        if (directionStr == null || directionStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Direction cannot be null or empty");
        }
        
        String normalized = directionStr.trim().toUpperCase();
        
        switch (normalized) {
            case "OUTGOING":
            case "OUT":
            case "I":
            case "INPUT":
                return OUTGOING;
                
            case "INCOMING":
            case "IN":
            case "O":
            case "OUTPUT":
                return INCOMING;
                
            default:
                throw new IllegalArgumentException("Invalid direction: " + directionStr + 
                    ". Valid values are: OUTGOING, INCOMING, I, O, INPUT, OUTPUT");
        }
    }
    
    @Override
    public String toString() {
        return name() + "(" + blockPrefix + ")";
    }
}
