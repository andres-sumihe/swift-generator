package me.andressumihe.swift.generators;

import com.prowidesoftware.swift.model.field.*;
import com.prowidesoftware.swift.model.mt.AbstractMT;
import com.prowidesoftware.swift.model.mt.mt9xx.MT950;
import me.andressumihe.swift.config.Configuration;
import me.andressumihe.swift.exceptions.MessageGenerationException;
import me.andressumihe.swift.model.enums.Direction;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generator for MT950 (Statement Message) messages.
 * Follows Single Responsibility Principle - only generates MT950 messages.
 * 
 * Supports both outgoing and incoming message generation:
 * - Outgoing: Standard SWIFT format for sending from our system
 * - Incoming: Enhanced format with Block 5 for messages received from SWIFT
 */
public class MT950Generator implements MessageGenerator {
    
    private final Configuration config;
    private final Random random = new Random();
    
    private static final String[] TRANSACTION_CODES = {
        "NTRF", "NMSC", "NFEX", "NCHG", "NDIV", "NINT", "NSEC", "NTRA"
    };
    
    private static final String[] DEBIT_CREDIT_INDICATORS = {"C", "D"};
    
    public MT950Generator(Configuration config) {
        this.config = config;
    }
    
    @Override
    public AbstractMT generateMessage(Direction direction) throws MessageGenerationException {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
            
            if (direction == Direction.INCOMING) {
                return generateIncomingMT950(today);
            } else {
                return generateOutgoingMT950(today);
            }
        } catch (Exception e) {
            throw new MessageGenerationException("Failed to generate MT950 message", "950", e);
        }
    }
    
    @Override
    public List<AbstractMT> generateMessages(int count, Direction direction) throws MessageGenerationException {
        List<AbstractMT> messages = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            messages.add(generateMessage(direction));
        }
        
        return messages;
    }
    
    @Override
    public String getSupportedMessageType() {
        return "950";
    }
    
    /**
     * Generate outgoing MT950 message (standard format).
     */
    private AbstractMT generateOutgoingMT950(String today) {
        MT950 mt950 = new MT950();
        
        // Configure SWIFT message headers with user's BIC codes
        mt950.setSender(config.getDefaultSenderBic());
        mt950.setReceiver(config.getDefaultReceiverBic());
        
        // Field 20: Transaction Reference Number
        mt950.addField(new Field20(String.format("VOSS%s/%05d", today, random.nextInt(100000))));
        
        // Field 25: Account Identification
        String accountNumber = String.format("205007%04d", random.nextInt(10000));
        mt950.addField(new Field25(accountNumber));
        
        // Field 28C: Statement Number/Sequence Number
        int statementNumber = random.nextInt(9999) + 1;
        mt950.addField(new Field28C(String.format("%05d/00001", statementNumber)));
        
        // Generate opening balance
        BigDecimal openingBalance = generateRandomBalance();
        String openingBalanceIndicator = random.nextBoolean() ? "C" : "D";
        
        // Field 60F: Opening Balance
        mt950.addField(new Field60F(String.format("%s%s%s%.2f", 
            openingBalanceIndicator, today, config.getDefaultCurrency(), openingBalance.doubleValue())));
        
        // Generate random number of transactions (1-10)
        int transactionCount = random.nextInt(10) + 1;
        BigDecimal runningBalance = openingBalance;
        
        for (int i = 0; i < transactionCount; i++) {
            BigDecimal transactionAmount = generateRandomTransactionAmount();
            String dcIndicator = DEBIT_CREDIT_INDICATORS[random.nextInt(DEBIT_CREDIT_INDICATORS.length)];
            String transactionCode = TRANSACTION_CODES[random.nextInt(TRANSACTION_CODES.length)];
            
            // Update running balance
            if ("C".equals(dcIndicator)) {
                runningBalance = runningBalance.add(transactionAmount);
            } else {
                runningBalance = runningBalance.subtract(transactionAmount);
            }
            
            // Field 61: Statement Line
            String reference = String.format("NONREF");
            if (random.nextBoolean()) {
                reference = String.format("256803013006%04d//BCA%s%07d", 
                    random.nextInt(10000), today, random.nextInt(10000000));
            }
            
            String statementLine = String.format("%s%s%s%.2f%s%s", 
                today, today.substring(2), dcIndicator, transactionAmount.doubleValue(), 
                transactionCode, reference);
            
            mt950.addField(new Field61(statementLine));
        }
        
        // Field 62F: Closing Balance
        String closingBalanceIndicator = runningBalance.compareTo(BigDecimal.ZERO) >= 0 ? "C" : "D";
        BigDecimal absClosingBalance = runningBalance.abs();
        mt950.addField(new Field62F(String.format("%s%s%s%.2f", 
            closingBalanceIndicator, today, config.getDefaultCurrency(), absClosingBalance.doubleValue())));
        
        // Field 64: Closing Available Balance
        mt950.addField(new Field64(String.format("%s%s%s%.2f", 
            closingBalanceIndicator, today, config.getDefaultCurrency(), absClosingBalance.doubleValue())));
        
        return mt950;
    }
    
    /**
     * Generate incoming MT950 message (with Block 5).
     */
    private AbstractMT generateIncomingMT950(String today) throws MessageGenerationException {
        try {
            AbstractMT outgoingMT950 = generateOutgoingMT950(today);
            MT950 mt950 = (MT950) outgoingMT950;
            
            // Add marker field to indicate network formatting required
            mt950.addField(new Field119("NETFMT"));
            
            return mt950;
            
        } catch (Exception e) {
            throw new MessageGenerationException("Failed to generate incoming MT950", "950", e);
        }
    }
    
    private BigDecimal generateRandomBalance() {
        BigDecimal min = BigDecimal.valueOf(1000);
        BigDecimal max = BigDecimal.valueOf(1000000000);
        BigDecimal range = max.subtract(min);
        return min.add(range.multiply(BigDecimal.valueOf(random.nextDouble())));
    }
    
    private BigDecimal generateRandomTransactionAmount() {
        BigDecimal min = BigDecimal.valueOf(1000);
        BigDecimal max = BigDecimal.valueOf(100000000);
        BigDecimal range = max.subtract(min);
        return min.add(range.multiply(BigDecimal.valueOf(random.nextDouble())));
    }
}
