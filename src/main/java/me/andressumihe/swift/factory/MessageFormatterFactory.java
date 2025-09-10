package me.andressumihe.swift.factory;

import me.andressumihe.swift.config.ConfigurationManager;
import me.andressumihe.swift.formatters.MessageFormatter;
import me.andressumihe.swift.model.enums.OutputFormat;

/**
 * Factory for creating message formatters based on output format.
 */
public class MessageFormatterFactory {
    
    private final MessageFormatterRegistry registry;
    
    public MessageFormatterFactory(ConfigurationManager config) {
        this.registry = new MessageFormatterRegistry();
    }
    
    public MessageFormatter createFormatter(OutputFormat format) {
        return registry.createFormatter(format);
    }
    
    public boolean isSupported(OutputFormat format) {
        return registry.isSupported(format);
    }

    public OutputFormat[] getSupportedFormats() {
        return registry.getSupportedFormats();
    }
}
