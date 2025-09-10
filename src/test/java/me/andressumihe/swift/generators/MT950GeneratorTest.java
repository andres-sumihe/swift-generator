package me.andressumihe.swift.generators;

import com.prowidesoftware.swift.model.field.Field20;
import com.prowidesoftware.swift.model.field.Field25;
import com.prowidesoftware.swift.model.field.Field28C;
import com.prowidesoftware.swift.model.field.Field60F;
import com.prowidesoftware.swift.model.field.Field61;
import com.prowidesoftware.swift.model.field.Field62F;
import com.prowidesoftware.swift.model.field.Field64;
import com.prowidesoftware.swift.model.mt.AbstractMT;
import com.prowidesoftware.swift.model.mt.mt9xx.MT950;
import me.andressumihe.swift.config.Configuration;
import me.andressumihe.swift.config.ConfigurationManager;
import me.andressumihe.swift.model.enums.Direction;
import me.andressumihe.swift.exceptions.MessageGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for MT950Generator.
 * Tests Customer Statement Message Interday functionality.
 */
public class MT950GeneratorTest {

    private MT950Generator generator;
    private Configuration config;

    @BeforeEach
    public void setUp() {
        config = ConfigurationManager.getInstance();
        generator = new MT950Generator(config);
    }

    @Test
    public void testGenerateOutgoingMT950_StructureAndFields() throws MessageGenerationException {
        // Act
        AbstractMT message = generator.generateMessage(Direction.OUTGOING);
        
        // Assert structure
        assertNotNull(message, "Generated message should not be null");
        assertTrue(message instanceof MT950, "Message should be MT950 type");
        assertEquals("950", generator.getSupportedMessageType(), "Message type should be 950");
        
        MT950 mt950 = (MT950) message;
        
        // Assert mandatory fields for MT950
        assertNotNull(mt950.getField20(), "Field 20 (Transaction Reference) should be present");
        assertNotNull(mt950.getField25(), "Field 25 (Account Identification) should be present");
        assertNotNull(mt950.getField28C(), "Field 28C (Statement Number) should be present");
        assertNotNull(mt950.getField60F(), "Field 60F (Opening Balance) should be present");
        assertNotNull(mt950.getField62F(), "Field 62F (Closing Balance) should be present");
        assertNotNull(mt950.getField64(), "Field 64 (Available Balance) should be present");
    }

    @Test
    public void testGenerateIncomingMT950_NetworkFormatting() throws MessageGenerationException {
        // Act
        AbstractMT message = generator.generateMessage(Direction.INCOMING);
        MT950 mt950 = (MT950) message;
        
        // Assert incoming message has network formatting marker
        String messageContent = message.message();
        assertTrue(messageContent.contains("{5:{TNG:}"), "Incoming message should have Block 5 with TNG marker");
        assertTrue(messageContent.contains("119:NETFMT"), "Incoming message should contain Field 119 NETFMT marker");
        
        // Assert basic structure is preserved
        assertNotNull(mt950.getField20(), "Field 20 should be present in incoming message");
        assertNotNull(mt950.getField25(), "Field 25 should be present in incoming message");
        assertNotNull(mt950.getField60F(), "Field 60F should be present in incoming message");
        assertNotNull(mt950.getField62F(), "Field 62F should be present in incoming message");
    }

    @Test
    public void testGenerateMultipleMT950Messages() throws MessageGenerationException {
        // Arrange
        int count = 3;
        
        // Act
        List<AbstractMT> messages = generator.generateMessages(count, Direction.OUTGOING);
        
        // Assert
        assertNotNull(messages, "Messages list should not be null");
        assertEquals(count, messages.size(), "Should generate exactly " + count + " messages");
        
        for (AbstractMT message : messages) {
            assertTrue(message instanceof MT950, "Each message should be MT950 type");
            MT950 mt950 = (MT950) message;
            assertNotNull(mt950.getField20(), "Each message should have Field 20");
            assertNotNull(mt950.getField25(), "Each message should have Field 25");
            assertNotNull(mt950.getField60F(), "Each message should have opening balance");
            assertNotNull(mt950.getField62F(), "Each message should have closing balance");
        }
        
        // Assert unique references
        String firstRef = ((MT950) messages.get(0)).getField20().getValue();
        String secondRef = ((MT950) messages.get(1)).getField20().getValue();
        assertNotEquals(firstRef, secondRef, "Each statement should have unique reference");
    }

    @Test
    public void testMT950FieldValues() throws MessageGenerationException {
        // Act
        MT950 mt950 = (MT950) generator.generateMessage(Direction.OUTGOING);
        
        // Assert field values
        Field20 field20 = mt950.getField20();
        assertTrue(field20.getValue().startsWith("VOSS"), "Field 20 should start with VOSS prefix");
        assertTrue(field20.getValue().length() == 12, "Field 20 should be 12 characters (VOSS + 8 digits)");
        
        Field25 field25 = mt950.getField25();
        String accountId = field25.getValue();
        assertTrue(accountId.length() > 0, "Field 25 should contain account identification");
        assertTrue(accountId.contains("/"), "Field 25 should have BIC/Account format");
        
        Field28C field28C = mt950.getField28C();
        String statementNumber = field28C.getValue();
        assertTrue(statementNumber.matches("\\d+/\\d+"), "Field 28C should have format: statement/sequence");
    }

    @Test
    public void testMT950BalanceConsistency() throws MessageGenerationException {
        // Act
        MT950 mt950 = (MT950) generator.generateMessage(Direction.OUTGOING);
        
        // Extract balance fields
        Field60F field60F = mt950.getField60F();
        Field62F field62F = mt950.getField62F();
        Field64 field64 = mt950.getField64();
        
        String openingBalance = field60F.getValue();
        String closingBalance = field62F.getValue();
        String availableBalance = field64.getValue();
        
        // Basic format validation
        assertTrue(openingBalance.matches("[CD]\\d{6}[A-Z]{3}[\\d,.]+"), 
                  "Opening balance should have proper format: C/D + Date + Currency + Amount");
        assertTrue(closingBalance.matches("[CD]\\d{6}[A-Z]{3}[\\d,.]+"), 
                  "Closing balance should have proper format: C/D + Date + Currency + Amount");
        assertTrue(availableBalance.matches("[CD]\\d{6}[A-Z]{3}[\\d,.]+"), 
                  "Available balance should have proper format: C/D + Date + Currency + Amount");
        
        // Currency consistency
        String openingCurrency = openingBalance.substring(7, 10);
        String closingCurrency = closingBalance.substring(7, 10);
        String availableCurrency = availableBalance.substring(7, 10);
        
        assertEquals(openingCurrency, closingCurrency, "Opening and closing balances should use same currency");
        assertEquals(closingCurrency, availableCurrency, "Closing and available balances should use same currency");
        
        // In MT950, closing balance equals available balance (end-of-day statement)
        assertEquals(closingBalance, availableBalance, "Closing and available balances should be identical in MT950");
    }

    @Test
    public void testTransactionEntries() throws MessageGenerationException {
        // Act
        MT950 mt950 = (MT950) generator.generateMessage(Direction.OUTGOING);
        
        // Assert transaction structure
        List<Field61> transactions = mt950.getField61();
        assertNotNull(transactions, "Statement should have transaction entries");
        assertFalse(transactions.isEmpty(), "Statement should have at least one transaction");
        assertTrue(transactions.size() <= 10, "Statement should not have more than 10 transactions");
        
        for (Field61 transaction : transactions) {
            String transValue = transaction.getValue();
            
            // Field 61 format: YYMMDD[MMDD]C/D[3!c]15d[//16x][34x]
            assertTrue(transValue.length() >= 10, "Transaction should have minimum required length");
            assertTrue(transValue.matches("\\d{6}.*[CD].*"), 
                      "Transaction should have date and debit/credit indicator");
        }
    }

    @Test
    public void testSwiftComplianceValidation() throws MessageGenerationException {
        // Act
        MT950 mt950 = (MT950) generator.generateMessage(Direction.OUTGOING);
        
        // Assert SWIFT MT950 basic structure compliance
        assertNotNull(mt950.getField20(), "Field 20 should be present");
        assertNotNull(mt950.getField25(), "Field 25 should be present");
        assertNotNull(mt950.getField60F(), "Field 60F should be present");
        assertNotNull(mt950.getField62F(), "Field 62F should be present");
        
        // Check statement number format
        Field28C field28C = mt950.getField28C();
        String statementNum = field28C.getValue();
        assertTrue(statementNum.matches("\\d+/\\d+"), 
                  "Statement number should follow required format");
        
        // Check account identification
        Field25 field25 = mt950.getField25();
        assertTrue(field25.getValue().length() <= 35, 
                  "Account identification should not exceed 35 characters");
        
        // Check balance date consistency
        Field60F openingBalance = mt950.getField60F();
        Field62F closingBalance = mt950.getField62F();
        
        String openingDate = openingBalance.getValue().substring(1, 7);
        String closingDate = closingBalance.getValue().substring(1, 7);
        
        assertTrue(openingDate.matches("\\d{6}"), "Opening balance date should be valid YYMMDD");
        assertTrue(closingDate.matches("\\d{6}"), "Closing balance date should be valid YYMMDD");
    }

    @Test
    public void testInputValidation() {
        // Test null direction
        assertThrows(MessageGenerationException.class, 
                    () -> generator.generateMessage(null),
                    "Should throw exception for null direction");
        
        // Test invalid counts
        assertThrows(IllegalArgumentException.class, 
                    () -> generator.generateMessages(0, Direction.OUTGOING),
                    "Should throw exception for zero count");
        
        assertThrows(IllegalArgumentException.class, 
                    () -> generator.generateMessages(-1, Direction.OUTGOING),
                    "Should throw exception for negative count");
    }

    @Test
    public void testConfigurationUsage() throws MessageGenerationException {
        // Act
        MT950 mt950 = (MT950) generator.generateMessage(Direction.OUTGOING);
        
        // Assert configuration values are used
        Field60F openingBalance = mt950.getField60F();
        assertTrue(openingBalance.getValue().contains(config.getDefaultCurrency()), 
                  "Should use configured currency in opening balance");
        
        Field62F closingBalance = mt950.getField62F();
        assertTrue(closingBalance.getValue().contains(config.getDefaultCurrency()), 
                  "Should use configured currency in closing balance");
        
        Field25 accountId = mt950.getField25();
        assertTrue(accountId.getValue().contains(config.getDefaultSenderBic()), 
                  "Should use configured sender BIC in account identification");
    }

    @Test
    public void testErrorHandling() {
        // Test with null configuration should fail during construction
        assertThrows(NullPointerException.class, 
                    () -> new MT950Generator(null),
                    "Should reject null configuration");
    }

    @Test
    public void testSupportedMessageType() {
        assertEquals("950", generator.getSupportedMessageType(), "Supported message type should be 950");
    }

    @Test
    public void testInterdayStatementCharacteristics() throws MessageGenerationException {
        // Act
        MT950 mt950 = (MT950) generator.generateMessage(Direction.OUTGOING);
        
        // MT950 is an interday statement (end-of-day snapshot)
        // Available balance should equal closing balance
        Field62F closingBalance = mt950.getField62F();
        Field64 availableBalance = mt950.getField64();
        
        assertEquals(closingBalance.getValue(), availableBalance.getValue(), 
                    "In MT950 (interday), closing balance should equal available balance");
        
        // Should have proper statement reference for interday reporting
        Field20 field20 = mt950.getField20();
        assertTrue(field20.getValue().startsWith("VOSS"), 
                  "Interday statement should have VOSS reference prefix");
    }
}
