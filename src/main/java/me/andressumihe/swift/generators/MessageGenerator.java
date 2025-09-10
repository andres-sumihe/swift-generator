package me.andressumihe.swift.generators;

import com.prowidesoftware.swift.model.mt.AbstractMT;
import me.andressumihe.swift.exceptions.MessageGenerationException;
import me.andressumihe.swift.model.enums.Direction;

import java.util.List;

/**
 * Interface for SWIFT message generators.
 * Follows Open/Closed Principle - open for extension, closed for modification.
 * 
 * Each message type (MT103, MT202, MT940) will have its own implementation.
 * Adding new message types requires implementing this interface, not modifying existing code.
 */
public interface MessageGenerator {
    
    /**
     * Generate a single SWIFT message.
     * 
     * @param direction The message direction (INCOMING or OUTGOING)
     * @return Generated SWIFT message
     * @throws MessageGenerationException if generation fails
     */
    AbstractMT generateMessage(Direction direction) throws MessageGenerationException;
    
    /**
     * Generate multiple SWIFT messages.
     * 
     * @param count The number of messages to generate
     * @param direction The message direction (INCOMING or OUTGOING)
     * @return List of generated SWIFT messages
     * @throws MessageGenerationException if generation fails
     */
    List<AbstractMT> generateMessages(int count, Direction direction) throws MessageGenerationException;
    
    /**
     * Get the message type this generator supports.
     * 
     * @return The supported message type code (e.g., "103", "202", "940")
     */
    String getSupportedMessageType();
}