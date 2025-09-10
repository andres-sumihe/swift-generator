package me.andressumihe.swift.factory;

import me.andressumihe.swift.config.Configuration;
import me.andressumihe.swift.generators.*;
import me.andressumihe.swift.model.enums.MessageType;

/**
 * Factory for creating MessageGenerator instances.
 */
public class MessageGeneratorFactory {
    
    private final MessageGeneratorRegistry registry;
    
    public MessageGeneratorFactory(Configuration config) {
        this.registry = new MessageGeneratorRegistry(config);
    }
    
    public MessageGenerator createGenerator(MessageType messageType) {
        return registry.createGenerator(messageType);
    }

    public MessageType[] getSupportedMessageTypes() {
        return registry.getSupportedMessageTypes();
    }
    
    public boolean isSupported(MessageType messageType) {
        return registry.isSupported(messageType);
    }
}