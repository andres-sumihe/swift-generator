package me.andressumihe.swift.factory;

import me.andressumihe.swift.formatters.MessageFormatter;
import me.andressumihe.swift.model.enums.OutputFormat;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry for message formatter implementations.
 */
public class MessageFormatterRegistry {
    
    private final Map<OutputFormat, Supplier<MessageFormatter>> formatterRegistry;
    
    public MessageFormatterRegistry() {
        this.formatterRegistry = new HashMap<>();
        registerDefaultFormatters();
    }
    
    private void registerDefaultFormatters() {
        registerFormatter(OutputFormat.DOS_PCC, () -> {
            try {
                Class<?> formatterClass = Class.forName("me.andressumihe.swift.formatters.DosPccFormatter");
                return (MessageFormatter) formatterClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create DosPccFormatter", e);
            }
        });
        
        registerFormatter(OutputFormat.FIN, () -> {
            try {
                Class<?> formatterClass = Class.forName("me.andressumihe.swift.formatters.FinFormatter");
                return (MessageFormatter) formatterClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create FinFormatter", e);
            }
        });
        
        registerFormatter(OutputFormat.RJE, () -> {
            try {
                Class<?> formatterClass = Class.forName("me.andressumihe.swift.formatters.RjeFormatter");
                return (MessageFormatter) formatterClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create RjeFormatter", e);
            }
        });
    }
    
    /**
     * Register a formatter supplier for an output format.
     * Allows runtime extension without modifying existing code.
     * 
     * @param format The output format to register
     * @param formatterSupplier Supplier that creates the formatter
     */
    public void registerFormatter(OutputFormat format, Supplier<MessageFormatter> formatterSupplier) {
        formatterRegistry.put(format, formatterSupplier);
    }
    
    public MessageFormatter createFormatter(OutputFormat format) {
        Supplier<MessageFormatter> supplier = formatterRegistry.get(format);
        if (supplier == null) {
            throw new IllegalArgumentException("No formatter registered for output format: " + format);
        }
        return supplier.get();
    }
    
    public boolean isSupported(OutputFormat format) {
        return formatterRegistry.containsKey(format);
    }

    public OutputFormat[] getSupportedFormats() {
        return formatterRegistry.keySet().toArray(new OutputFormat[0]);
    }
}
