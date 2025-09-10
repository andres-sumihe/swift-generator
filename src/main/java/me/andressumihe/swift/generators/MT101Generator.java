package me.andressumihe.swift.generators;

import com.prowidesoftware.swift.model.field.*;
import com.prowidesoftware.swift.model.mt.AbstractMT;
import com.prowidesoftware.swift.model.mt.mt1xx.MT101;
import me.andressumihe.swift.config.Configuration;
import me.andressumihe.swift.exceptions.MessageGenerationException;

import java.math.BigDecimal;

/**
 * Generator for MT101 (Request for Transfer) messages.
 * 
 * MT101 is used to convey instructions for individual customer credit transfers
 * to be executed by the receiver. This implementation generates single-transaction
 * MT101s to maintain SWIFT compliance (single Field 21 per message).
 * 
 * SWIFT COMPLIANCE NOTE: 
 * - Field 21 appears ONCE per message (not per transaction)
 * - Multi-transaction MT101s require complex sequence handling
 * - This implementation focuses on single transactions for clarity and compliance
 */
public class MT101Generator extends AbstractMessageGenerator {
    
    private static final String[] INSTRUCTION_CODES = {
        "CRED", "CRDT", "DBIT"
    };
    
    private static final String[] CHARGE_CODES = {
        "OUR", "BEN", "SHA"
    };
    
    public MT101Generator(Configuration config) {
        super(config);
    }
    
    @Override
    protected AbstractMT generateOutgoingMessage(String today) throws MessageGenerationException {
        MT101 mt101 = new MT101();
        
        // Configure SWIFT message headers with user's BIC codes
        mt101.setSender(config.getDefaultSenderBic());
        mt101.setReceiver(config.getDefaultReceiverBic());
        
        // Field 20: Transaction Reference Number (Sender's Reference)
        mt101.addField(new Field20(generateTransactionReference("REQ")));
        
        // Field 21: Related Reference (SINGLE field per message - SWIFT compliant)
        mt101.addField(new Field21(generateTransactionReference("TXN")));
        
        // Field 23E: Instruction Code  
        String instructionCode = INSTRUCTION_CODES[random.nextInt(INSTRUCTION_CODES.length)];
        mt101.addField(new Field23E(instructionCode));
        
        // Field 26T: Transaction Type Code
        mt101.addField(new Field26T("001")); // Standard transaction type
        
        // Field 50H: Ordering Customer (Account and Name/Address)
        mt101.addField(new Field50H(config.getDefaultSenderAccount() + "\n" + 
                                   config.getDefaultSenderName() + "\n" + 
                                   config.getDefaultSenderAddress()));
        
        // Field 30: Requested Execution Date
        mt101.addField(new Field30(today));
        
        // Generate SINGLE transaction to maintain SWIFT compliance
        
        // Field 32B: Currency Code and Amount
        BigDecimal amount = generateRandomAmount();
        mt101.addField(new Field32B(config.getDefaultCurrency() + 
                      String.format("%.2f", amount.doubleValue())));
        
        // Field 50F or 50K: Ordering Customer (varies per transaction)
        if (random.nextBoolean()) {
            mt101.addField(new Field50F("/" + generateAccountNumber() + "\n" +
                                      generateCustomerName() + "\n" +
                                      generateCustomerAddress()));
        } else {
            mt101.addField(new Field50K(generateCustomerName() + "\n" + 
                                      generateCustomerAddress()));
        }
        
        // Field 52A/52D: Ordering Institution
        if (random.nextBoolean()) {
            mt101.addField(new Field52A(config.getDefaultSenderBic()));
        } else {
            mt101.addField(new Field52D(config.getDefaultSenderBic() + "\n" + 
                                      config.getDefaultSenderName()));
        }
        
        // Field 57A/57D: Account With Institution
        if (random.nextBoolean()) {
            mt101.addField(new Field57A(config.getDefaultReceiverBic()));
        } else {
            mt101.addField(new Field57D(config.getDefaultReceiverBic() + "\n" + 
                                      "CORRESPONDENT BANK NAME"));
        }
        
        // Field 59F or 59: Beneficiary Customer
        if (random.nextBoolean()) {
            mt101.addField(new Field59F("/" + generateAccountNumber() + "\n" +
                                      config.getDefaultReceiverName() + "\n" +
                                      config.getDefaultReceiverAddress()));
        } else {
            mt101.addField(new Field59("/" + config.getDefaultReceiverAccount() + "\n" +
                                     config.getDefaultReceiverName() + "\n" +
                                     config.getDefaultReceiverAddress()));
        }
        
        // Field 70: Remittance Information
        mt101.addField(new Field70(generateRemittanceInfo()));
        
        // Field 71A: Details of Charges
        String chargeCode = CHARGE_CODES[random.nextInt(CHARGE_CODES.length)];
        mt101.addField(new Field71A(chargeCode));
        
        return mt101;
    }
    
    @Override
    public String getSupportedMessageType() {
        return "101";
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
    
    private String generateCustomerName() {
        String[] names = {
            "ASIA PACIFIC TRADING LTD",
            "GLOBAL SERVICES CORPORATION", 
            "INTERNATIONAL BUSINESS CO",
            "PACIFIC RIM ENTERPRISES",
            "EURO ASIA FINANCIAL GROUP"
        };
        return names[random.nextInt(names.length)];
    }
    
    private String generateCustomerAddress() {
        String[] addresses = {
            "LEVEL 15, TOWER 1, INTERNATIONAL PLAZA\nSINGAPORE 068810, SG",
            "88 PHILLIP STREET, #10-01\nSINGAPORE 048692, SG", 
            "1 RAFFLES PLACE, #20-61\nSINGAPORE 048616, SG",
            "6 BATTERY ROAD, #25-01\nSINGAPORE 049909, SG",
            "80 ROBINSON ROAD, #02-00\nSINGAPORE 068898, SG"
        };
        return addresses[random.nextInt(addresses.length)];
    }
    
    private String generateRemittanceInfo() {
        String[] remittanceInfo = {
            "PAYMENT FOR INVOICE INV-2025-001\nTRADE FINANCE SETTLEMENT",
            "MONTHLY MANAGEMENT FEE\nPROFESSIONAL SERVICES", 
            "SUPPLIER PAYMENT\nGOODS DELIVERY Q4-2025",
            "SALARY TRANSFER\nEMPLOYEE COMPENSATION",
            "UTILITY PAYMENT\nOFFICE EXPENSES"
        };
        return remittanceInfo[random.nextInt(remittanceInfo.length)];
    }
}
