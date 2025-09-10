package me.andressumihe.swift.generators;

import com.prowidesoftware.swift.model.field.Field20;
import com.prowidesoftware.swift.model.field.Field21;
import com.prowidesoftware.swift.model.field.Field23E;
import com.prowidesoftware.swift.model.field.Field26T;
import com.prowidesoftware.swift.model.field.Field30;
import com.prowidesoftware.swift.model.field.Field32B;
import com.prowidesoftware.swift.model.field.Field50H;
import com.prowidesoftware.swift.model.field.Field71A;
import com.prowidesoftware.swift.model.mt.AbstractMT;
import com.prowidesoftware.swift.model.mt.mt1xx.MT101;
import me.andressumihe.swift.config.Configuration;
import me.andressumihe.swift.config.ConfigurationManager;
import me.andressumihe.swift.model.enums.Direction;
import me.andressumihe.swift.exceptions.MessageGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for MT101Generator.
 * Tests Request for Transfer functionality.
 */
public class MT101GeneratorTest {

    private MT101Generator generator;
    private Configuration config;

    @BeforeEach
    public void setUp() {
        config = ConfigurationManager.getInstance();
        generator = new MT101Generator(config);
    }

    @Test
    public void testGenerateOutgoingMT101_StructureAndFields() throws MessageGenerationException {
        // Act
        AbstractMT message = generator.generateMessage(Direction.OUTGOING);
        
        // Assert structure
        assertNotNull(message, "Generated message should not be null");
        assertTrue(message instanceof MT101, "Message should be MT101 type");
        assertEquals("101", generator.getSupportedMessageType(), "Message type should be 101");
        
        MT101 mt101 = (MT101) message;
        
        // Assert mandatory fields for MT101
        assertNotNull(mt101.getField20(), "Field 20 (Sender's Reference) should be present");
        assertNotNull(mt101.getField50H(), "Field 50H (Ordering Customer) should be present");
        assertNotNull(mt101.getField30(), "Field 30 (Requested Execution Date) should be present");
        
        // Check for transaction fields
        assertFalse(mt101.getField32B().isEmpty(), "Field 32B (Currency/Amount) should be present");
    }

    @Test
    public void testGenerateOutgoingMT101_FieldValues() throws MessageGenerationException {
        // Act
        MT101 mt101 = (MT101) generator.generateMessage(Direction.OUTGOING);
        
        // Assert field values
        Field20 field20 = mt101.getField20();
        assertTrue(field20.getValue().startsWith("REQ"), "Field 20 should start with REQ prefix");
        assertTrue(field20.getValue().length() == 11, "Field 20 should be 11 characters (REQ + 8 digits)");
        
        // Field 21: Related Reference (single field per MT101)
        List<Field21> field21List = mt101.getField21();
        assertNotNull(field21List, "Field 21 should be present");
        assertFalse(field21List.isEmpty(), "Field 21 should not be empty");
        Field21 field21 = field21List.get(0);
        assertTrue(field21.getValue().startsWith("TXN"), "Field 21 should start with TXN prefix");
        
        // Field 23E: Instruction Code
        List<Field23E> field23EList = mt101.getField23E();
        assertNotNull(field23EList, "Field 23E should be present");
        assertFalse(field23EList.isEmpty(), "Field 23E should not be empty");
        Field23E field23E = field23EList.get(0);
        String instructionCode = field23E.getValue();
        assertTrue(instructionCode.equals("CRED") || instructionCode.equals("CRDT") || instructionCode.equals("DBIT"),
                  "Field 23E should contain valid instruction code");
        
        // Field 26T: Transaction Type Code  
        // Use tags() method to check if field exists
        String messageText = mt101.message();
        assertTrue(messageText.contains("{26T:001}"), "Field 26T should contain transaction type 001");
        
        // Field 32B: Currency and Amount
        List<Field32B> field32BList = mt101.getField32B();
        assertNotNull(field32BList, "Field 32B should be present");
        assertFalse(field32BList.isEmpty(), "Field 32B should not be empty");
        Field32B field32B = field32BList.get(0);
        String field32BValue = field32B.getValue();
        assertTrue(field32BValue.startsWith(config.getDefaultCurrency()), "Field 32B should start with default currency");
        
        // Field 71A: Details of Charges
        List<Field71A> field71AList = mt101.getField71A();
        if (!field71AList.isEmpty()) {
            Field71A field71A = field71AList.get(0);
            String chargeCode = field71A.getValue();
            assertTrue(chargeCode.equals("OUR") || chargeCode.equals("BEN") || chargeCode.equals("SHA"),
                      "Field 71A should contain valid charge code");
        }
    }

    @Test
    public void testGenerateIncomingMT101_NetworkFormatting() throws MessageGenerationException {
        // Act
        AbstractMT message = generator.generateMessage(Direction.INCOMING);
        MT101 mt101 = (MT101) message;
        
        // Assert incoming message has network formatting marker
        String messageContent = message.message();
        assertTrue(messageContent.contains("{5:{TNG:}"), "Incoming message should have Block 5 with TNG marker");
        assertTrue(messageContent.contains("119:NETFMT"), "Incoming message should contain Field 119 NETFMT marker");
        
        // Assert basic structure is preserved
        assertNotNull(mt101.getField20(), "Field 20 should be present in incoming message");
        assertNotNull(mt101.getField50H(), "Field 50H should be present in incoming message");
    }

    @Test
    public void testGenerateMultipleMT101Messages() throws MessageGenerationException {
        // Arrange
        int count = 3;
        
        // Act
        List<AbstractMT> messages = generator.generateMessages(count, Direction.OUTGOING);
        
        // Assert
        assertNotNull(messages, "Messages list should not be null");
        assertEquals(count, messages.size(), "Should generate exactly " + count + " messages");
        
        for (AbstractMT message : messages) {
            assertTrue(message instanceof MT101, "Each message should be MT101 type");
            MT101 mt101 = (MT101) message;
            assertNotNull(mt101.getField20(), "Each message should have Field 20");
            assertFalse(mt101.getField32B().isEmpty(), "Each message should have Field 32B");
        }
        
        // Assert unique references
        String firstRef = ((MT101) messages.get(0)).getField20().getValue();
        String secondRef = ((MT101) messages.get(1)).getField20().getValue();
        assertNotEquals(firstRef, secondRef, "Each message should have unique reference");
    }

    @Test
    public void testSWIFTCompliance_SingleField21() throws MessageGenerationException {
        // Act
        MT101 mt101 = (MT101) generator.generateMessage(Direction.OUTGOING);
        
        // Assert SWIFT compliance - Field 21 should appear only once per message
        List<Field21> field21List = mt101.getField21();
        assertEquals(1, field21List.size(), "Field 21 should appear exactly once per MT101 message (SWIFT compliance)");
        
        // Check that the single Field 21 has proper format
        Field21 field21 = field21List.get(0);
        assertTrue(field21.getValue().length() > 0, "Field 21 should not be empty");
        assertTrue(field21.getValue().startsWith("TXN"), "Field 21 should have TXN prefix");
    }

    @Test
    public void testAmountGeneration() throws MessageGenerationException {
        // Act
        MT101 mt101 = (MT101) generator.generateMessage(Direction.OUTGOING);
        
        // Assert amount is within configured range
        List<Field32B> field32BList = mt101.getField32B();
        assertFalse(field32BList.isEmpty(), "Should have at least one amount field");
        
        Field32B field32B = field32BList.get(0);
        String field32BValue = field32B.getValue();
        
        // Extract amount (after 3-letter currency)
        String amountStr = field32BValue.substring(3);
        double amount = Double.parseDouble(amountStr);
        
        assertTrue(amount >= 100.00, "Amount should be at least minimum configured amount");
        assertTrue(amount <= 100000.00, "Amount should not exceed maximum configured amount");
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
        MT101 mt101 = (MT101) generator.generateMessage(Direction.OUTGOING);
        
        // Assert configuration values are used
        List<Field32B> field32BList = mt101.getField32B();
        assertFalse(field32BList.isEmpty(), "Should have amount field");
        
        Field32B field32B = field32BList.get(0);
        assertTrue(field32B.getValue().startsWith(config.getDefaultCurrency()), 
                  "Should use configured currency");
        
        // Field 50H: Check via message content since specific getter might not exist
        String messageText = mt101.message();
        assertTrue(messageText.contains(config.getDefaultSenderName()), 
                  "Should use configured sender name");
    }

    @Test
    public void testErrorHandling() {
        // Test with null configuration should fail during construction
        assertThrows(NullPointerException.class, 
                    () -> new MT101Generator(null),
                    "Should reject null configuration");
    }

    @Test
    public void testTemplateMethodPattern() throws MessageGenerationException {
        // Test that Template Method pattern is working properly
        
        // Test outgoing message generation
        AbstractMT outgoingMessage = generator.generateMessage(Direction.OUTGOING);
        assertNotNull(outgoingMessage, "Template Method should generate outgoing message");
        
        // Test incoming message generation  
        AbstractMT incomingMessage = generator.generateMessage(Direction.INCOMING);
        assertNotNull(incomingMessage, "Template Method should generate incoming message");
        
        // Incoming messages should have different structure (network formatting)
        String outgoingContent = outgoingMessage.message();
        String incomingContent = incomingMessage.message();
        
        assertNotEquals(outgoingContent, incomingContent, "Outgoing and incoming messages should differ");
        assertTrue(incomingContent.contains("119:NETFMT"), "Incoming should have network formatting marker");
    }
}
