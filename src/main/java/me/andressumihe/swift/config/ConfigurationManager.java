package me.andressumihe.swift.config;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuration manager that loads settings from application.properties.
 */
public class ConfigurationManager implements Configuration {
    
    private static final Logger logger = Logger.getLogger(ConfigurationManager.class.getName());
    private static final String PROPERTIES_FILE = "application.properties";
    private static ConfigurationManager instance;
    
    private final Properties properties;
    
    private ConfigurationManager() {
        this.properties = new Properties();
        loadProperties();
    }
    
    public static synchronized ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }
    
    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                logger.warning("Unable to find " + PROPERTIES_FILE + ", using default values");
                return;
            }
            
            properties.load(input);
            logger.info("Loaded configuration from " + PROPERTIES_FILE);
            
        } catch (IOException e) {
            logger.severe("Failed to load " + PROPERTIES_FILE + ": " + e.getMessage());
        }
    }
    
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warning("Invalid integer value for property " + key + ": " + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            logger.warning("Invalid decimal value for property " + key + ": " + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        
        return Boolean.parseBoolean(value.trim());
    }
    
    public String getDefaultSenderBic() {
        String value = getString("swift.generator.default.sender.bic", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.default.sender.bic' not found in application.properties");
        }
        return value;
    }
    
    public String getDefaultReceiverBic() {
        String value = getString("swift.generator.default.receiver.bic", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.default.receiver.bic' not found in application.properties");
        }
        return value;
    }
    
    public String getDefaultSessionNumber() {
        String value = getString("swift.generator.default.session.number", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.default.session.number' not found in application.properties");
        }
        return value;
    }
    
    public String getDefaultSequenceNumber() {
        String value = getString("swift.generator.default.sequence.number", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.default.sequence.number' not found in application.properties");
        }
        return value;
    }
    
    public String getDefaultCurrency() {
        String value = getString("swift.generator.default.currency", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.default.currency' not found in application.properties");
        }
        return value;
    }
    
    public String getDefaultSenderName() {
        String value = getString("swift.generator.default.sender.name", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.default.sender.name' not found in application.properties");
        }
        return value;
    }
    
    public String getDefaultSenderAddress() {
        String value = getString("swift.generator.default.sender.address1", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.default.sender.address1' not found in application.properties");
        }
        return value;
    }
    
    public String getDefaultSenderAccount() {
        String value = getString("swift.generator.default.sender.account", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.default.sender.account' not found in application.properties");
        }
        return value;
    }
    
    public String getDefaultReceiverName() {
        String value = getString("swift.generator.default.receiver.name", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.default.receiver.name' not found in application.properties");
        }
        return value;
    }
    
    public String getDefaultReceiverAddress() {
        String value = getString("swift.generator.default.receiver.address1", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.default.receiver.address1' not found in application.properties");
        }
        return value;
    }
    
    public String getDefaultReceiverAccount() {
        String value = getString("swift.generator.default.receiver.account", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.default.receiver.account' not found in application.properties");
        }
        return value;
    }
    
    public BigDecimal getMinAmount() {
        String value = properties.getProperty("swift.generator.test.data.amount.min");
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.test.data.amount.min' not found in application.properties");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid decimal value for 'swift.generator.test.data.amount.min': " + value);
        }
    }
    
    public BigDecimal getMaxAmount() {
        String value = properties.getProperty("swift.generator.test.data.amount.max");
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.test.data.amount.max' not found in application.properties");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid decimal value for 'swift.generator.test.data.amount.max': " + value);
        }
    }
    
    public int getMaxMessages() {
        String value = properties.getProperty("swift.generator.max.messages");
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.max.messages' not found in application.properties");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer value for 'swift.generator.max.messages': " + value);
        }
    }
    
    public int getDosPccSectorSize() {
        String value = properties.getProperty("swift.generator.format.dospcc.sector.size");
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.format.dospcc.sector.size' not found in application.properties");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer value for 'swift.generator.format.dospcc.sector.size': " + value);
        }
    }
    
    public int getDosPccStartMarker() {
        String value = properties.getProperty("swift.generator.format.dospcc.start.marker");
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.format.dospcc.start.marker' not found in application.properties");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer value for 'swift.generator.format.dospcc.start.marker': " + value);
        }
    }
    
    public int getDosPccEndMarker() {
        String value = properties.getProperty("swift.generator.format.dospcc.end.marker");
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.format.dospcc.end.marker' not found in application.properties");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer value for 'swift.generator.format.dospcc.end.marker': " + value);
        }
    }
    
    public int getDosPccPaddingByte() {
        String value = properties.getProperty("swift.generator.format.dospcc.padding.byte");
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.format.dospcc.padding.byte' not found in application.properties");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer value for 'swift.generator.format.dospcc.padding.byte': " + value);
        }
    }
    
    public String getDosPccEncoding() {
        String value = getString("swift.generator.format.dospcc.encoding", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.format.dospcc.encoding' not found in application.properties");
        }
        return value;
    }
    
    public String getRjeBatchDelimiter() {
        String value = getString("swift.generator.format.rje.batch.delimiter", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.format.rje.batch.delimiter' not found in application.properties");
        }
        return value;
    }
    
    public String getRjeEncoding() {
        String value = getString("swift.generator.format.rje.encoding", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.format.rje.encoding' not found in application.properties");
        }
        return value;
    }
    
    public String getFinEncoding() {
        String value = getString("swift.generator.format.fin.encoding", null);
        if (value == null) {
            throw new RuntimeException("Required configuration 'swift.generator.format.fin.encoding' not found in application.properties");
        }
        return value;
    }
}
