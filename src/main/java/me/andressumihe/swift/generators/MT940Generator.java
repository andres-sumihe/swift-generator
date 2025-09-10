package me.andressumihe.swift.generators;

import com.prowidesoftware.swift.model.field.*;
import com.prowidesoftware.swift.model.mt.AbstractMT;
import com.prowidesoftware.swift.model.mt.mt9xx.MT940;
import me.andressumihe.swift.config.Configuration;
import me.andressumihe.swift.exceptions.MessageGenerationException;
import me.andressumihe.swift.model.enums.Direction;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generator for MT940 (Customer Statement Message) messages.
 * Creates bank statements with variable transaction counts and proper balance flows.
 */
public class MT940Generator implements MessageGenerator {
    
    private final Configuration config;
    private final Random random = new Random();
    
    private static final String[] TRANSACTION_CODES = {
        "NTRF", "NMSC", "NFEX", "NCHG", "NDIV", "NINT", "NSEC", "NTRA"
    };
    
    private static final String[] COMPANY_NAMES = {
        "SELAMETK AutoDb-PL", "MUBAROKN AutoDb-PL", "WENI FEBRIANI", "PUAD BAWAZIR",
        "BENNYMUD AutoDb-PL", "ZAITU ASRI", "HAERUNNISA", "ARI MEILANNY", 
        "LITA INDARTINI", "ANA RAUDATUL JANN", "NIRMAL SINGH", "EMBASSY OF REPUB",
        "EKAWATI TANTAWI", "SULIKAH", "RENY LIANG", "SRI FINI JL. PUNC",
        "ALICE", "CV SILKY JAYA", "SRI UTAMI", "AGUS HIDAYAT", "INDA WIYANA",
        "NI WAYAN MURIANTI", "LINA", "JUDIANTI MAKATITA", "TERRY PRICILIA W",
        "CHAO YIT GIOK", "DAVID WIJAYA JALA", "SABARINA TARIGAN", "DESAK NYOMAN SUAN",
        "HARTONO KARTAWIDJ", "AISHA ADIRA", "INDOSARANA BALI W", "KENNETH ROSS SNEL",
        "WENY WIDYAWATI", "NI KETUT SISI", "VERANICA AMELLIA", "FROM TREASURY"
    };
    
    private static final String[] REFERENCE_PREFIXES = {
        "BCA", "TX", "TRS", "TT", "BAJJ"
    };
    
    private static final String[] ACCOUNT_PATTERNS = {
        "2050071424", "2050076001", "2050078003", "2050093002", "3193109973",
        "7490070090", "0353002462", "0353022188", "1234567890", "9876543210"
    };
    
    public MT940Generator(Configuration config) {
        this.config = config;
    }
    
    @Override
    public AbstractMT generateMessage(Direction direction) throws MessageGenerationException {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
            
            if (direction == Direction.INCOMING) {
                return generateIncomingMT940(today);
            } else {
                return generateOutgoingMT940(today);
            }
        } catch (Exception e) {
            throw new MessageGenerationException("Failed to generate MT940 message", "940", e);
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
        return "940";
    }
    
    /**
     * Generate outgoing MT940 with variable transactions and proper balance flow.
     */
    private AbstractMT generateOutgoingMT940(String today) throws MessageGenerationException {
        try {
            MT940 mt940 = new MT940();
            
            // Configure SWIFT message headers with user's BIC codes
            mt940.setSender(config.getDefaultSenderBic());
            mt940.setReceiver(config.getDefaultReceiverBic());
            
            // Generate basic statement info
            String statementRef = String.format("VOSS%s/%05d", today, random.nextInt(100000));
            String accountNumber = ACCOUNT_PATTERNS[random.nextInt(ACCOUNT_PATTERNS.length)];
            String statementNumber = String.format("%05d/%05d", random.nextInt(10000) + 1, 1);
            
            mt940.addField(new Field20(statementRef));
            mt940.addField(new Field25(accountNumber));
            mt940.addField(new Field28C(statementNumber));
            
            // Generate opening balance (random between 100K and 10M)
            BigDecimal openingBalance = BigDecimal.valueOf(100000 + random.nextDouble() * 9900000)
                    .setScale(2, RoundingMode.HALF_UP);
            
            String creditDebitMark = random.nextBoolean() ? "C" : "D";
            mt940.addField(new Field60F(creditDebitMark + today + config.getDefaultCurrency() + 
                    openingBalance.toPlainString().replace(".", ",")));
            
            // Generate variable number of transactions (2-20 per statement)
            int transactionCount = 2 + random.nextInt(19); // 2-20 transactions
            BigDecimal runningBalance = openingBalance;
            if (creditDebitMark.equals("D")) {
                runningBalance = runningBalance.negate();
            }
            
            for (int i = 0; i < transactionCount; i++) {
                TransactionData txn = generateTransaction(today, runningBalance);
                mt940.addField(new Field61(txn.toField61String()));
                runningBalance = txn.newBalance;
            }
            
            // Add closing balance
            String closingCreditDebitMark = runningBalance.compareTo(BigDecimal.ZERO) >= 0 ? "C" : "D";
            mt940.addField(new Field62F(closingCreditDebitMark + today + config.getDefaultCurrency() + 
                    runningBalance.abs().toPlainString().replace(".", ",")));
                    
            // Add available balance (same as closing for simplicity)
            mt940.addField(new Field64(closingCreditDebitMark + today + config.getDefaultCurrency() + 
                    runningBalance.abs().toPlainString().replace(".", ",")));
            
            return mt940;
            
        } catch (Exception e) {
            throw new MessageGenerationException("Failed to generate outgoing MT940", "940", e);
        }
    }
    
    /**
     * Generate incoming MT940 with SWIFT network format markers.
     */
    private AbstractMT generateIncomingMT940(String today) throws MessageGenerationException {
        try {
            AbstractMT outgoingMT940 = generateOutgoingMT940(today);
            MT940 mt940 = (MT940) outgoingMT940;
            
            // Add marker field to indicate network formatting required
            mt940.addField(new Field119("NETFMT"));
            
            return mt940;
            
        } catch (Exception e) {
            throw new MessageGenerationException("Failed to generate incoming MT940", "940", e);
        }
    }
    
    /**
     * Generate a transaction with proper amounts and descriptions.
     */
    private TransactionData generateTransaction(String valueDate, BigDecimal currentBalance) {
        // Determine if this is a credit or debit (60% debit, 40% credit)
        boolean isDebit = random.nextDouble() < 0.6;
        
        // Generate amounts based on transaction type
        BigDecimal amount;
        if (isDebit) {
            // Debit amounts: 1,000 to 500,000,000
            double baseAmount = 1000 + random.nextDouble() * 499999000;
            amount = BigDecimal.valueOf(baseAmount).setScale(2, RoundingMode.HALF_UP);
        } else {
            // Credit amounts: 10,000 to 1,000,000,000
            double baseAmount = 10000 + random.nextDouble() * 999990000;
            amount = BigDecimal.valueOf(baseAmount).setScale(2, RoundingMode.HALF_UP);
        }
        
        // Calculate new balance
        BigDecimal newBalance = isDebit ? 
                currentBalance.subtract(amount) : 
                currentBalance.add(amount);
        
        // Generate transaction code and reference
        String transactionCode = TRANSACTION_CODES[random.nextInt(TRANSACTION_CODES.length)];
        String reference = generateTransactionReference();
        String description = COMPANY_NAMES[random.nextInt(COMPANY_NAMES.length)];
        
        return new TransactionData(valueDate, isDebit, amount, transactionCode, reference, description, newBalance);
    }
    
    /**
     * Generate transaction reference.
     */
    private String generateTransactionReference() {
        String prefix = REFERENCE_PREFIXES[random.nextInt(REFERENCE_PREFIXES.length)];
        
        switch (prefix) {
            case "BCA":
                return String.format("%s%s%08d", prefix, 
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")), 
                        random.nextInt(100000000));
            case "TX":
                return String.format("%s%06d", prefix, random.nextInt(1000000));
            case "TRS":
                return String.format("%s%s%04d", prefix, 
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), 
                        random.nextInt(10000));
            case "TT":
                return String.format("%s%s%s%07d", prefix, 
                        random.nextInt(10) < 5 ? "30X" : "30Y",
                        random.nextInt(10000000),
                        random.nextInt(10000000));
            default:
                return String.format("%s%08d%08d", prefix, 
                        random.nextInt(100000000), 
                        random.nextInt(100000000));
        }
    }
    
    /**
     * Data class to hold transaction information.
     */
    private static class TransactionData {
        final String valueDate;
        final boolean isDebit;
        final BigDecimal amount;
        final String transactionCode;
        final String reference;
        final String description;
        final BigDecimal newBalance;
        
        TransactionData(String valueDate, boolean isDebit, BigDecimal amount, 
                       String transactionCode, String reference, String description, BigDecimal newBalance) {
            this.valueDate = valueDate;
            this.isDebit = isDebit;
            this.amount = amount;
            this.transactionCode = transactionCode;
            this.reference = reference;
            this.description = description;
            this.newBalance = newBalance;
        }
        
        /**
         * Convert to Field 61 format: :61:YYMMDDMMDD[CR|DR]amount[transaction code][reference]//[description]
         */
        String toField61String() {
            String creditDebit = isDebit ? "DR" : "CR";
            String amountStr = amount.toPlainString().replace(".", ",");
            
            return String.format("%s%s%s%s%s%s//%s", 
                    valueDate, valueDate, creditDebit, amountStr, 
                    transactionCode, reference, description);
        }
    }
}
