package me.andressumihe.swift.generators;

import com.prowidesoftware.swift.model.field.Field119;
import com.prowidesoftware.swift.model.field.Field20;
import com.prowidesoftware.swift.model.field.Field23B;
import com.prowidesoftware.swift.model.field.Field32A;
import com.prowidesoftware.swift.model.field.Field50K;
import com.prowidesoftware.swift.model.field.Field57A;
import com.prowidesoftware.swift.model.field.Field59;
import com.prowidesoftware.swift.model.field.Field70;
import com.prowidesoftware.swift.model.field.Field71A;
import com.prowidesoftware.swift.model.mt.AbstractMT;
import com.prowidesoftware.swift.model.mt.mt1xx.MT103;
import me.andressumihe.swift.config.Configuration;
import me.andressumihe.swift.config.ConfigurationManager;
import me.andressumihe.swift.model.enums.Direction;
import me.andressumihe.swift.exceptions.MessageGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for MT103Generator.
 * Tests Single Customer Credit Transfer functionality.
 */
public class MT103GeneratorTest {

    private MT103Generator generator;
    private Configuration config;

    @BeforeEach
    public void setUp() {
        config = ConfigurationManager.getInstance();
        generator = new MT103Generator(config);
    }

    @Test
    public void testGenerateOutgoingMT103_StructureAndFields() throws MessageGenerationException {
        // Act
        AbstractMT message = generator.generateMessage(Direction.OUTGOING);
        
        // Assert structure
        assertNotNull(message, "Generated message should not be null");
        assertTrue(message instanceof MT103, "Message should be MT103 type");
        assertEquals("103", generator.getSupportedMessageType(), "Message type should be 103");
        
        MT103 mt103 = (MT103) message;
        
        // Assert mandatory fields for MT103 (only fields that MT103Generator actually generates)
        assertNotNull(mt103.getField20(), "Field 20 (Sender's Reference) should be present");
        assertNotNull(mt103.getField23B(), "Field 23B (Bank Operation Code) should be present");
        assertNotNull(mt103.getField32A(), "Field 32A (Value Date, Currency, Amount) should be present");
        assertNotNull(mt103.getField50K(), "Field 50K (Ordering Customer) should be present");
        assertNotNull(mt103.getField59(), "Field 59 (Beneficiary Customer) should be present");
        assertNotNull(mt103.getField71A(), "Field 71A (Details of Charges) should be present");
    }

    @Test
    public void testGenerateOutgoingMT103_FieldValues() throws MessageGenerationException {
        // Act
        MT103 mt103 = (MT103) generator.generateMessage(Direction.OUTGOING);
        
        // Assert field values
        Field20 field20 = mt103.getField20();
        assertTrue(field20.getValue().startsWith("TXN"), "Field 20 should start with TXN prefix");
        assertTrue(field20.getValue().length() == 11, "Field 20 should be 11 characters (TXN + 8 digits)");
        
        Field23B field23B = mt103.getField23B();
        assertEquals("CRED", field23B.getValue(), "Field 23B should be CRED for credit transfer");
        
        Field32A field32A = mt103.getField32A();
        String field32AValue = field32A.getValue();
        assertTrue(field32AValue.matches("\\d{6}[A-Z]{3}[\\d,.]+"), 
                  "Field 32A should have format: YYMMDD + Currency + Amount");
        assertTrue(field32AValue.contains(config.getDefaultCurrency()), 
                  "Field 32A should contain default currency");
        
        Field71A field71A = mt103.getField71A();
        String chargeCode = field71A.getValue();
        assertTrue(chargeCode.equals("OUR") || chargeCode.equals("BEN") || chargeCode.equals("SHA"),
                  "Field 71A should contain valid charge code");
    }

    @Test
    public void testGenerateIncomingMT103_NetworkFormatting() throws MessageGenerationException {
        // Act
        AbstractMT message = generator.generateMessage(Direction.INCOMING);
        MT103 mt103 = (MT103) message;
        
        // Assert incoming message has network formatting marker (Field 119 with NETFMT)
        // Use getTags() to check for Field 119 since specific methods may not exist
        String messageText = mt103.message();
        assertTrue(messageText.contains("{119:NETFMT}"), "Incoming message should have Field 119 with NETFMT marker");
        
        // Assert basic structure is preserved
        assertNotNull(mt103.getField20(), "Field 20 should be present in incoming message");
        assertNotNull(mt103.getField23B(), "Field 23B should be present in incoming message");
        assertNotNull(mt103.getField32A(), "Field 32A should be present in incoming message");
    }

    @Test
    public void testGenerateMultipleMT103Messages() throws MessageGenerationException {
        // Arrange
        int count = 3;
        
        // Act
        List<AbstractMT> messages = generator.generateMessages(count, Direction.OUTGOING);
        
        // Assert
        assertNotNull(messages, "Messages list should not be null");
        assertEquals(count, messages.size(), "Should generate exactly " + count + " messages");
        
        for (AbstractMT message : messages) {
            assertTrue(message instanceof MT103, "Each message should be MT103 type");
            MT103 mt103 = (MT103) message;
            assertNotNull(mt103.getField20(), "Each message should have Field 20");
            assertNotNull(mt103.getField32A(), "Each message should have Field 32A");
        }
        
        // Assert unique references
        String firstRef = ((MT103) messages.get(0)).getField20().getValue();
        String secondRef = ((MT103) messages.get(1)).getField20().getValue();
        assertNotEquals(firstRef, secondRef, "Each message should have unique reference");
    }

    @Test
    public void testSwiftComplianceValidation() throws MessageGenerationException {
        // Act
        MT103 mt103 = (MT103) generator.generateMessage(Direction.OUTGOING);
        
        // Assert SWIFT MT103 basic structure compliance
        assertNotNull(mt103.getField20(), "Field 20 should be present");
        assertNotNull(mt103.getField23B(), "Field 23B should be present");
        assertNotNull(mt103.getField32A(), "Field 32A should be present");
        
        // Check amount format in Field 32A
        Field32A field32A = mt103.getField32A();
        String value = field32A.getValue();
        assertTrue(value.length() >= 9, "Field 32A should have minimum length for date+currency+amount");
        
        // Check proper customer fields
        Field50K field50K = mt103.getField50K();
        assertTrue(field50K.getValue().length() > 0, "Field 50K should have ordering customer information");
        
        Field59 field59 = mt103.getField59();
        assertTrue(field59.getValue().length() > 0, "Field 59 should have beneficiary information");
    }

    @Test
    public void testAmountGeneration() throws MessageGenerationException {
        // Act
        MT103 mt103 = (MT103) generator.generateMessage(Direction.OUTGOING);
        
        // Assert amount is within configured range
        Field32A field32A = mt103.getField32A();
        String field32AValue = field32A.getValue();
        
        // Extract amount (after 6-digit date and 3-letter currency)
        String amountStr = field32AValue.substring(9);
        double amount = Double.parseDouble(amountStr.replace(",", ""));
        
        assertTrue(amount >= 100.00, "Amount should be at least minimum configured amount");
        assertTrue(amount <= 100000.00, "Amount should not exceed maximum configured amount");
    }

    @Test
    public void testInputValidation() {
        // Test null direction - AbstractMessageGenerator throws NullPointerException, not MessageGenerationException
        assertThrows(NullPointerException.class, 
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
        MT103 mt103 = (MT103) generator.generateMessage(Direction.OUTGOING);
        
        // Assert configuration values are used
        Field32A field32A = mt103.getField32A();
        assertTrue(field32A.getValue().contains(config.getDefaultCurrency()), 
                  "Should use configured currency");
        
        Field50K field50K = mt103.getField50K();
        assertTrue(field50K.getValue().contains(config.getDefaultSenderAccount()), 
                  "Should use configured sender account");
        
        Field59 field59 = mt103.getField59();
        assertTrue(field59.getValue().contains(config.getDefaultReceiverAccount()), 
                  "Should use configured receiver account");
    }

    @Test
    public void testErrorHandling() {
        // Test with null configuration should fail during construction
        assertThrows(NullPointerException.class, 
                    () -> new MT103Generator(null),
                    "Should reject null configuration");
    }
}