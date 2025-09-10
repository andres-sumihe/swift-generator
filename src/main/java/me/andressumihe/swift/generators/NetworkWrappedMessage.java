package me.andressumihe.swift.generators;

import com.prowidesoftware.swift.model.mt.AbstractMT;
import com.prowidesoftware.swift.model.field.Field;
import com.prowidesoftware.swift.model.SwiftMessage;
import com.prowidesoftware.swift.model.MtId;

import java.util.List;

/**
 * Generic wrapper class for incoming SWIFT messages that preserves the business message type
 * while containing the full SWIFT network format with F21 headers and Block 5.
 * 
 * This solves the issue where network-wrapped messages are parsed as ServiceMessage21
 * but we need them to validate as their business message type (MT101, MT103, MT202, MT940, MT950)
 * for formatters and processors.
 * 
 * @param <T> The business message type (MT101, MT103, MT202, MT940, MT950, etc.)
 */
public class NetworkWrappedMessage extends AbstractMT {
    
    private final AbstractMT networkMessage;
    private final AbstractMT businessMessage;
    
    /**
     * Creates a network-wrapped message that preserves business message type identity.
     * 
     * @param networkMessage The full network message (including F21 headers and Block 5)
     * @param businessMessage The extracted business message (MT101, MT103, MT202, MT940, MT950, etc.)
     */
    public NetworkWrappedMessage(AbstractMT networkMessage, AbstractMT businessMessage) {
        this.networkMessage = networkMessage;
        this.businessMessage = businessMessage;
    }
    
    @Override
    public String getMessageType() {
        // Return the business message type (101, 103, 202, 940, 950, etc.) for formatter validation
        return businessMessage.getMessageType();
    }
    
    @Override
    public String message() {
        // Return the full network format for output
        return networkMessage.message();
    }
    
    @Override
    public List<Field> getFields() {
        // Return business message fields for processing
        return businessMessage.getFields();
    }
    
    @Override
    public SwiftMessage getSwiftMessage() {
        // Return the network message SwiftMessage object
        return networkMessage.getSwiftMessage();
    }
    
    @Override
    public MtId getMtId() {
        return businessMessage.getMtId();
    }
    
    /**
     * Get the original network message for advanced processing.
     * Contains the complete F21 wrapper with Block 5.
     */
    public AbstractMT getNetworkMessage() {
        return networkMessage;
    }
    
    /**
     * Get the business message for field-level processing.
     * This is the actual MT message (MT101, MT103, MT202, MT940, MT950, etc.)
     */
    public AbstractMT getBusinessMessage() {
        return businessMessage;
    }
    
    /**
     * Factory method to create a network-wrapped message.
     * 
     * @param networkMessage The complete network message
     * @param businessMessage The extracted business message
     * @return A new NetworkWrappedMessage instance
     */
    public static NetworkWrappedMessage wrap(AbstractMT networkMessage, AbstractMT businessMessage) {
        return new NetworkWrappedMessage(networkMessage, businessMessage);
    }
}
