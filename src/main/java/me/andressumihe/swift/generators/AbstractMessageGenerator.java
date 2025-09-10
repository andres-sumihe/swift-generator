package me.andressumihe.swift.generators;

import com.prowidesoftware.swift.model.field.Field119;
import com.prowidesoftware.swift.model.mt.AbstractMT;
import me.andressumihe.swift.config.Configuration;
import me.andressumihe.swift.exceptions.MessageGenerationException;
import me.andressumihe.swift.model.enums.Direction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Abstract base class for all SWIFT message generators.
 * 
 * Provides common functionality and enforces consistent patterns:
 * - Input validation
 * - Error handling 
 * - Date formatting
 * - Network message handling
 * - Configuration management
 * 
 * Follows Template Method pattern for consistent message generation flow.
 */
public abstract class AbstractMessageGenerator implements MessageGenerator {
    
    protected final Configuration config;
    protected final Random random;
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    
    /**
     * Constructor with mandatory configuration validation.
     * 
     * @param config Configuration object - must not be null
     * @throws IllegalArgumentException if config is null
     */
    protected AbstractMessageGenerator(Configuration config) {
        this.config = Objects.requireNonNull(config, "Configuration cannot be null");
        this.random = new Random();
    }
    
    @Override
    public final AbstractMT generateMessage(Direction direction) throws MessageGenerationException {
        validateDirection(direction);
        
        try {
            String today = LocalDate.now().format(DATE_FORMATTER);
            
            if (direction == Direction.INCOMING) {
                return generateIncomingMessage(today);
            } else {
                return generateOutgoingMessage(today);
            }
        } catch (Exception e) {
            throw new MessageGenerationException(
                String.format("Failed to generate %s message", getSupportedMessageType()), 
                getSupportedMessageType(), 
                e
            );
        }
    }
    
    @Override
    public final List<AbstractMT> generateMessages(int count, Direction direction) throws MessageGenerationException {
        validateCount(count);
        validateDirection(direction);
        
        List<AbstractMT> messages = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            messages.add(generateMessage(direction));
        }
        
        return messages;
    }
    
    /**
     * Generate incoming message with network formatting marker.
     * Template method that handles common network formatting logic.
     */
    protected final AbstractMT generateIncomingMessage(String today) throws MessageGenerationException {
        try {
            AbstractMT outgoingMessage = generateOutgoingMessage(today);
            
            // Add marker field to indicate network formatting required
            // This is processed by NetworkWrappedMessage during formatting
            outgoingMessage.addField(new Field119("NETFMT"));
            
            return outgoingMessage;
            
        } catch (Exception e) {
            throw new MessageGenerationException(
                String.format("Failed to generate incoming %s", getSupportedMessageType()), 
                getSupportedMessageType(), 
                e
            );
        }
    }
    
    /**
     * Generate outgoing message - implemented by concrete generators.
     * 
     * @param today Today's date in yyMMdd format
     * @return Generated outgoing message
     * @throws MessageGenerationException if generation fails
     */
    protected abstract AbstractMT generateOutgoingMessage(String today) throws MessageGenerationException;
    
    /**
     * Validate direction parameter.
     */
    private void validateDirection(Direction direction) {
        Objects.requireNonNull(direction, "Direction cannot be null");
    }
    
    /**
     * Validate count parameter.
     */
    private void validateCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive, got: " + count);
        }
        if (count > 10000) {
            throw new IllegalArgumentException("Count exceeds maximum limit of 10000, got: " + count);
        }
    }
    
    /**
     * Generate random account number.
     */
    protected String generateAccountNumber() {
        return String.format("%010d", random.nextInt(1000000000));
    }
    
    /**
     * Get today's date in SWIFT format.
     */
    protected String getTodaySwiftFormat() {
        return LocalDate.now().format(DATE_FORMATTER);
    }
    
    /**
     * Generate random transaction reference.
     */
    protected String generateTransactionReference(String prefix) {
        return String.format("%s%08d", prefix, random.nextInt(100000000));
    }
}
