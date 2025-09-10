package me.andressumihe.swift.config;

import java.math.BigDecimal;

/**
 * Interface for configuration management.
 */
public interface Configuration {
    
    String getDefaultSenderBic();
    String getDefaultReceiverBic();
    String getDefaultCurrency();
    BigDecimal getMinAmount();
    BigDecimal getMaxAmount();
    int getMaxMessages();
    String getDefaultSenderAccount();
    String getDefaultSenderName();
    String getDefaultSenderAddress();
    String getDefaultReceiverAccount();
    String getDefaultReceiverName();
    String getDefaultReceiverAddress();
}
