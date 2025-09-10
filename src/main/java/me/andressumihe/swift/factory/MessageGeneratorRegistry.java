package me.andressumihe.swift.factory;

import me.andressumihe.swift.config.Configuration;
import me.andressumihe.swift.generators.MessageGenerator;
import me.andressumihe.swift.model.enums.MessageType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Registry for message generator implementations.
 */
public class MessageGeneratorRegistry {
    
    private final Configuration config;
    private final Map<MessageType, Function<Configuration, MessageGenerator>> generatorRegistry;
    
    public MessageGeneratorRegistry(Configuration config) {
        this.config = config;
        this.generatorRegistry = new HashMap<>();
        registerDefaultGenerators();
    }
    
    private void registerDefaultGenerators() {
        // Using reflection would be even more OCP-compliant, but this is cleaner
        registerGenerator(MessageType.MT101, config -> {
            try {
                Class<?> generatorClass = Class.forName("me.andressumihe.swift.generators.MT101Generator");
                return (MessageGenerator) generatorClass.getConstructor(Configuration.class).newInstance(config);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create MT101Generator", e);
            }
        });
        
        registerGenerator(MessageType.MT103, config -> {
            try {
                Class<?> generatorClass = Class.forName("me.andressumihe.swift.generators.MT103Generator");
                return (MessageGenerator) generatorClass.getConstructor(Configuration.class).newInstance(config);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create MT103Generator", e);
            }
        });
        
        registerGenerator(MessageType.MT202, config -> {
            try {
                Class<?> generatorClass = Class.forName("me.andressumihe.swift.generators.MT202Generator");
                return (MessageGenerator) generatorClass.getConstructor(Configuration.class).newInstance(config);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create MT202Generator", e);
            }
        });
        
        registerGenerator(MessageType.MT940, config -> {
            try {
                Class<?> generatorClass = Class.forName("me.andressumihe.swift.generators.MT940Generator");
                return (MessageGenerator) generatorClass.getConstructor(Configuration.class).newInstance(config);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create MT940Generator", e);
            }
        });
        
        registerGenerator(MessageType.MT950, config -> {
            try {
                Class<?> generatorClass = Class.forName("me.andressumihe.swift.generators.MT950Generator");
                return (MessageGenerator) generatorClass.getConstructor(Configuration.class).newInstance(config);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create MT950Generator", e);
            }
        });
    }
    
    public void registerGenerator(MessageType messageType, Function<Configuration, MessageGenerator> generatorFactory) {
        generatorRegistry.put(messageType, generatorFactory);
    }

    public MessageGenerator createGenerator(MessageType messageType) {
        Function<Configuration, MessageGenerator> factory = generatorRegistry.get(messageType);
        if (factory == null) {
            throw new IllegalArgumentException("No generator registered for message type: " + messageType);
        }
        return factory.apply(config);
    }
    
    public boolean isSupported(MessageType messageType) {
        return generatorRegistry.containsKey(messageType);
    }

    public MessageType[] getSupportedMessageTypes() {
        return generatorRegistry.keySet().toArray(new MessageType[0]);
    }
}
