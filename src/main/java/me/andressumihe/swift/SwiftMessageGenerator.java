package me.andressumihe.swift;

import com.prowidesoftware.swift.model.mt.mt1xx.MT103;
import com.prowidesoftware.swift.model.field.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * SWIFT MT Message Generator for testing purposes
 * Uses Prowide Core library to generate various SWIFT MT messages
 */
public class SwiftMessageGenerator {

    public static void main(String[] args) {
        SwiftMessageGenerator generator = new SwiftMessageGenerator();
        
        System.out.println("=== SWIFT MT Message Generator ===");
        System.out.println();
        
        // Generate MT103 - Single Customer Credit Transfer
        String mt103Message = generator.generateMT103();
        System.out.println("Generated MT103 Message:");
        System.out.println(mt103Message);
        System.out.println();
        
        System.out.println("Message generation completed successfully!");
    }

    /**
     * Generates a sample MT103 message (Single Customer Credit Transfer)
     * @return SWIFT MT103 message as string
     */
    public String generateMT103() {
        // Create MT103 message
        MT103 mt103 = new MT103();
        
        // Set basic header information
        mt103.setSender("BANKUSAAXXX");
        mt103.setReceiver("BANKDEFRXXX");
        
        // Field 20: Transaction Reference Number
        mt103.addField(new Field20("REF123456789"));
        
        // Field 23B: Bank Operation Code
        mt103.addField(new Field23B("CRED"));
        
        // Field 32A: Value Date, Currency Code, Amount
        mt103.addField(new Field32A("240822USD1000,00"));
        
        // Field 50K: Ordering Customer
        mt103.addField(new Field50K("/1234567890\nJOHN DOE\n123 MAIN STREET\nNEW YORK NY 10001\nUS"));
        
        // Field 53A: Sender's Correspondent (optional)
        mt103.addField(new Field53A("CHASUS33XXX"));
        
        // Field 57A: Account With Institution
        mt103.addField(new Field57A("BANKDEFRXXX"));
        
        // Field 59: Beneficiary Customer
        mt103.addField(new Field59("/DE89370400440532013000\nJANE SMITH\nBERLIN STRASSE 45\n10115 BERLIN\nDE"));
        
        // Field 70: Remittance Information
        mt103.addField(new Field70("PAYMENT FOR SERVICES\nINVOICE 2024-001"));
        
        // Field 71A: Details of Charges
        mt103.addField(new Field71A("SHA"));
        
        return mt103.message();
    }

    /**
     * Generates a sample MT202 message (General Financial Institution Transfer)
     * @return SWIFT MT202 message as string
     */
    public String generateMT202() {
        // This method can be expanded to generate MT202 messages
        // For now, returning a placeholder
        return "MT202 generation not yet implemented";
    }

    /**
     * Generates a sample MT940 message (Customer Statement Message)
     * @return SWIFT MT940 message as string
     */
    public String generateMT940() {
        // This method can be expanded to generate MT940 messages
        // For now, returning a placeholder
        return "MT940 generation not yet implemented";
    }
}
