package me.andressumihe.swift.generators;

import com.prowidesoftware.swift.model.field.*;
import com.prowidesoftware.swift.model.mt.AbstractMT;
import com.prowidesoftware.swift.model.mt.mt1xx.MT103;
import me.andressumihe.swift.config.Configuration;
import me.andressumihe.swift.exceptions.MessageGenerationException;

import java.math.BigDecimal;

/**
 * Generator for MT103 (Single Customer Credit Transfer) messages.
 * 
 * Follows SOLID principles:
 * - Single Responsibility: Only generates MT103 messages
 * - Open/Closed: Extensible via inheritance
 * - Liskov Substitution: Can replace AbstractMessageGenerator
 * - Interface Segregation: Implements focused MessageGenerator interface
 * - Dependency Inversion: Depends on Configuration abstraction
 */
public class MT103Generator extends AbstractMessageGenerator {
    
    public MT103Generator(Configuration config) {
        super(config);
    }
    
    @Override
    protected AbstractMT generateOutgoingMessage(String today) throws MessageGenerationException {
        MT103 mt103 = new MT103();
        
        // Configure SWIFT message headers with user's BIC codes
        mt103.setSender(config.getDefaultSenderBic());
        mt103.setReceiver(config.getDefaultReceiverBic());
        
        // Field 20: Sender's Reference
        mt103.addField(new Field20(generateTransactionReference("TXN")));
        
        // Field 23B: Bank Operation Code
        mt103.addField(new Field23B("CRED"));
        
        // Field 32A: Value Date, Currency Code, and Amount
        BigDecimal amount = generateRandomAmount();
        mt103.addField(new Field32A(today + config.getDefaultCurrency() + 
                      String.format("%.2f", amount.doubleValue())));
        
        // Field 50K: Ordering Customer
        mt103.addField(new Field50K(config.getDefaultSenderAccount() + "\n" + 
                                  config.getDefaultSenderName() + "\n" + 
                                  config.getDefaultSenderAddress()));
        
        // Field 59: Beneficiary Customer  
        mt103.addField(new Field59(config.getDefaultReceiverAccount() + "\n" + 
                                 config.getDefaultReceiverName() + "\n" + 
                                 config.getDefaultReceiverAddress()));
        
        // Field 71A: Details of Charges
        mt103.addField(new Field71A("OUR"));
        
        return mt103;
    }
    
    @Override
    public String getSupportedMessageType() {
        return "103";
    }
    
    /**
     * Generate random amount within configured range.
     */
    private BigDecimal generateRandomAmount() {
        BigDecimal minAmount = config.getMinAmount();
        BigDecimal maxAmount = config.getMaxAmount();
        BigDecimal range = maxAmount.subtract(minAmount);
        return minAmount.add(range.multiply(BigDecimal.valueOf(random.nextDouble())));
    }
}