package me.andressumihe.swift.generators;

import com.prowidesoftware.swift.model.field.*;
import com.prowidesoftware.swift.model.mt.AbstractMT;
import com.prowidesoftware.swift.model.mt.mt2xx.MT202;
import me.andressumihe.swift.config.Configuration;
import me.andressumihe.swift.exceptions.MessageGenerationException;

import java.math.BigDecimal;

/**
 * Generator for MT202 (General Financial Institution Transfer) messages.
 * 
 * MT202 is used for financial institution to financial institution transfers,
 * typically for correspondent banking relationships.
 */
public class MT202Generator extends AbstractMessageGenerator {
    
    private static final double MIN_TRANSFER_AMOUNT = 1000.0;
    private static final double MAX_TRANSFER_AMOUNT = 50000.0;
    
    public MT202Generator(Configuration config) {
        super(config);
    }
    
    @Override
    protected AbstractMT generateOutgoingMessage(String today) throws MessageGenerationException {
        MT202 mt202 = new MT202();
        
        // Configure SWIFT message headers with user's BIC codes
        mt202.setSender(config.getDefaultSenderBic());
        mt202.setReceiver(config.getDefaultReceiverBic());
        
        // Field 20: Sender's Reference
        mt202.addField(new Field20(generateTransactionReference("COV")));
        
        // Field 21: Related Reference
        mt202.addField(new Field21("NONREF"));
        
        // Field 32A: Value Date, Currency Code, and Amount
        BigDecimal amount = generateRandomInstitutionalAmount();
        mt202.addField(new Field32A(today + config.getDefaultCurrency() + 
                      String.format("%.2f", amount.doubleValue())));
        
        // Field 52A: Ordering Institution  
        mt202.addField(new Field52A(config.getDefaultSenderBic()));
        
        // Field 58A: Beneficiary Institution
        mt202.addField(new Field58A(config.getDefaultReceiverBic()));
        
        return mt202;
    }
    
    @Override
    public String getSupportedMessageType() {
        return "202";
    }
    
    /**
     * Generate random amount suitable for institutional transfers.
     * MT202 typically involves larger amounts than customer transfers.
     */
    private BigDecimal generateRandomInstitutionalAmount() {
        double randomAmount = MIN_TRANSFER_AMOUNT + random.nextDouble() * 
                             (MAX_TRANSFER_AMOUNT - MIN_TRANSFER_AMOUNT);
        return BigDecimal.valueOf(randomAmount).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}