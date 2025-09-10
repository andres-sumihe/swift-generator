package me.andressumihe.swift.generators;

import com.prowidesoftware.swift.model.field.Field119;
import com.prowidesoftware.swift.model.field.Field20;
import com.prowidesoftware.swift.model.field.Field21;
import com.prowidesoftware.swift.model.field.Field32A;
import com.prowidesoftware.swift.model.field.Field52A;
import com.prowidesoftware.swift.model.field.Field53A;
import com.prowidesoftware.swift.model.field.Field56A;
import com.prowidesoftware.swift.model.field.Field57A;
import com.prowidesoftware.swift.model.field.Field58A;
import com.prowidesoftware.swift.model.field.Field72;
import com.prowidesoftware.swift.model.mt.AbstractMT;
import com.prowidesoftware.swift.model.mt.mt2xx.MT202;
import me.andressumihe.swift.config.Configuration;
import me.andressumihe.swift.config.ConfigurationManager;
import me.andressumihe.swift.model.enums.Direction;
import me.andressumihe.swift.exceptions.MessageGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for MT202Generator.
 * Tests General Financial Institution Transfer functionality.
 */
public class MT202GeneratorTest {

    private MT202Generator generator;
    private Configuration config;

    @BeforeEach
    public void setUp() {
        config = ConfigurationManager.getInstance();
        generator = new MT202Generator(config);
    }

    @Test
    public void testGenerateOutgoingMT202_StructureAndFields() throws MessageGenerationException {
        // Act
        AbstractMT message = generator.generateMessage(Direction.OUTGOING);
        
        // Assert structure
        assertNotNull(message, "Generated message should not be null");
        assertTrue(message instanceof MT202, "Message should be MT202 type");
        assertEquals("202", generator.getSupportedMessageType(), "Message type should be 202");
        
        MT202 mt202 = (MT202) message;
        
        // Assert mandatory fields for MT202
        assertNotNull(mt202.getField20(), "Field 20 (Sender's Reference) should be present");
        assertNotNull(mt202.getField21(), "Field 21 (Related Reference) should be present");
        assertNotNull(mt202.getField32A(), "Field 32A (Value Date, Currency, Amount) should be present");
        assertNotNull(mt202.getField52A(), "Field 52A (Ordering Institution) should be present");
        assertNotNull(mt202.getField58A(), "Field 58A (Beneficiary Institution) should be present");
    }

    @Test
    public void testGenerateOutgoingMT202_FieldValues() throws MessageGenerationException {
        // Act
        MT202 mt202 = (MT202) generator.generateMessage(Direction.OUTGOING);
        
        // Assert field values
        Field20 field20 = mt202.getField20();
        assertTrue(field20.getValue().startsWith("FIT"), "Field 20 should start with FIT prefix");
        assertTrue(field20.getValue().length() == 11, "Field 20 should be 11 characters (FIT + 8 digits)");
        
        Field21 field21 = mt202.getField21();
        assertTrue(field21.getValue().startsWith("REL"), "Field 21 should start with REL prefix");
        assertTrue(field21.getValue().length() == 11, "Field 21 should be 11 characters (REL + 8 digits)");
        
        Field32A field32A = mt202.getField32A();
        String field32AValue = field32A.getValue();
        assertTrue(field32AValue.matches("\\d{6}[A-Z]{3}[\\d,.]+"), 
                  "Field 32A should have format: YYMMDD + Currency + Amount");
        assertTrue(field32AValue.contains(config.getDefaultCurrency()), 
                  "Field 32A should contain default currency");
        
        Field52A field52A = mt202.getField52A();
        assertTrue(field52A.getValue().length() > 0, "Field 52A should contain ordering institution");
        
        Field58A field58A = mt202.getField58A();
        assertTrue(field58A.getValue().length() > 0, "Field 58A should contain beneficiary institution");
    }

    @Test
    public void testGenerateIncomingMT202_NetworkFormatting() throws MessageGenerationException {
        // Act
        AbstractMT message = generator.generateMessage(Direction.INCOMING);
        MT202 mt202 = (MT202) message;
        
        // Assert incoming message has network formatting marker
        String messageContent = message.message();
        assertTrue(messageContent.contains("{5:{TNG:}"), "Incoming message should have Block 5 with TNG marker");
        assertTrue(messageContent.contains("119:NETFMT"), "Incoming message should contain Field 119 NETFMT marker");
        
        // Assert basic structure is preserved
        assertNotNull(mt202.getField20(), "Field 20 should be present in incoming message");
        assertNotNull(mt202.getField21(), "Field 21 should be present in incoming message");
        assertNotNull(mt202.getField32A(), "Field 32A should be present in incoming message");
    }

    @Test
    public void testGenerateMultipleMT202Messages() throws MessageGenerationException {
        // Arrange
        int count = 3;
        
        // Act
        List<AbstractMT> messages = generator.generateMessages(count, Direction.OUTGOING);
        
        // Assert
        assertNotNull(messages, "Messages list should not be null");
        assertEquals(count, messages.size(), "Should generate exactly " + count + " messages");
        
        for (AbstractMT message : messages) {
            assertTrue(message instanceof MT202, "Each message should be MT202 type");
            MT202 mt202 = (MT202) message;
            assertNotNull(mt202.getField20(), "Each message should have Field 20");
            assertNotNull(mt202.getField21(), "Each message should have Field 21");
            assertNotNull(mt202.getField32A(), "Each message should have Field 32A");
        }
        
        // Assert unique references
        String firstRef = ((MT202) messages.get(0)).getField20().getValue();
        String secondRef = ((MT202) messages.get(1)).getField20().getValue();
        assertNotEquals(firstRef, secondRef, "Each message should have unique reference");
    }

    @Test
    public void testIntermediaryInstitutions() throws MessageGenerationException {
        // Act
        MT202 mt202 = (MT202) generator.generateMessage(Direction.OUTGOING);
        
        // Check optional intermediary fields
        Field53A field53A = mt202.getField53A(); // Sender's Correspondent
        Field56A field56A = mt202.getField56A(); // Intermediary
        Field57A field57A = mt202.getField57A(); // Account With Institution
        
        // These are optional but if present should be valid
        if (field53A != null) {
            assertTrue(field53A.getValue().length() > 0, "Field 53A should not be empty when present");
        }
        
        if (field56A != null) {
            assertTrue(field56A.getValue().length() > 0, "Field 56A should not be empty when present");
        }
        
        if (field57A != null) {
            assertTrue(field57A.getValue().length() > 0, "Field 57A should not be empty when present");
        }
    }

    @Test
    public void testSwiftComplianceValidation() throws MessageGenerationException {
        // Act
        MT202 mt202 = (MT202) generator.generateMessage(Direction.OUTGOING);
        
        // Assert SWIFT MT202 basic structure compliance
        assertNotNull(mt202.getField20(), "Field 20 should be present");
        assertNotNull(mt202.getField21(), "Field 21 should be present");
        assertNotNull(mt202.getField32A(), "Field 32A should be present");
        
        // Check amount format in Field 32A
        Field32A field32A = mt202.getField32A();
        String value = field32A.getValue();
        assertTrue(value.length() >= 9, "Field 32A should have minimum length for date+currency+amount");
        
        // Check institution fields
        Field52A field52A = mt202.getField52A();
        assertTrue(field52A.getValue().length() > 0, "Field 52A should have ordering institution information");
        
        Field58A field58A = mt202.getField58A();
        assertTrue(field58A.getValue().length() > 0, "Field 58A should have beneficiary institution information");
    }

    @Test
    public void testAmountGeneration() throws MessageGenerationException {
        // Act
        MT202 mt202 = (MT202) generator.generateMessage(Direction.OUTGOING);
        
        // Assert amount is within configured range
        Field32A field32A = mt202.getField32A();
        String field32AValue = field32A.getValue();
        
        // Extract amount (after 6-digit date and 3-letter currency)
        String amountStr = field32AValue.substring(9);
        double amount = Double.parseDouble(amountStr.replace(",", ""));
        
        assertTrue(amount >= 1000.00, "Amount should be at least minimum configured amount for institutional transfers");
        assertTrue(amount <= 1000000.00, "Amount should not exceed maximum configured amount");
    }

    @Test
    public void testReferenceRelationship() throws MessageGenerationException {
        // Act
        MT202 mt202 = (MT202) generator.generateMessage(Direction.OUTGOING);
        
        // Assert reference fields
        Field20 field20 = mt202.getField20();
        Field21 field21 = mt202.getField21();
        
        String senderRef = field20.getValue();
        String relatedRef = field21.getValue();
        
        assertNotEquals(senderRef, relatedRef, "Sender's reference and related reference should be different");
        
        // Both should follow expected patterns
        assertTrue(senderRef.startsWith("FIT"), "Sender's reference should start with FIT");
        assertTrue(relatedRef.startsWith("REL"), "Related reference should start with REL");
    }

    @Test
    public void testOptionalRemittanceInfo() throws MessageGenerationException {
        // Act
        MT202 mt202 = (MT202) generator.generateMessage(Direction.OUTGOING);
        
        // Check optional sender to receiver information
        Field72 field72 = mt202.getField72();
        
        // Field 72 is optional but if present should be valid
        if (field72 != null) {
            assertTrue(field72.getValue().length() > 0, "Field 72 should not be empty when present");
            assertTrue(field72.getValue().length() <= 210, "Field 72 should not exceed 210 characters");
        }
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
        MT202 mt202 = (MT202) generator.generateMessage(Direction.OUTGOING);
        
        // Assert configuration values are used
        Field32A field32A = mt202.getField32A();
        assertTrue(field32A.getValue().contains(config.getDefaultCurrency()), 
                  "Should use configured currency");
        
        Field52A field52A = mt202.getField52A();
        assertTrue(field52A.getValue().contains(config.getDefaultSenderBic()), 
                  "Should use configured sender BIC");
        
        Field58A field58A = mt202.getField58A();
        assertTrue(field58A.getValue().contains(config.getDefaultReceiverBic()), 
                  "Should use configured receiver BIC");
    }

    @Test
    public void testErrorHandling() {
        // Test with null configuration should fail during construction
        assertThrows(NullPointerException.class, 
                    () -> new MT202Generator(null),
                    "Should reject null configuration");
    }

    @Test
    public void testInstitutionalTransferCharacteristics() throws MessageGenerationException {
        // Act
        MT202 mt202 = (MT202) generator.generateMessage(Direction.OUTGOING);
        
        // MT202 is for financial institution transfers
        // Should have higher amounts and proper institutional fields
        Field32A field32A = mt202.getField32A();
        String amountStr = field32A.getValue().substring(9);
        double amount = Double.parseDouble(amountStr.replace(",", ""));
        
        assertTrue(amount >= 1000.00, "Institutional transfers should have substantial amounts");
        
        // Should have proper BIC codes in institutional fields
        Field52A field52A = mt202.getField52A();
        Field58A field58A = mt202.getField58A();
        
        assertTrue(field52A.getValue().length() >= 8, "Ordering institution should have proper BIC");
        assertTrue(field58A.getValue().length() >= 8, "Beneficiary institution should have proper BIC");
    }
}
