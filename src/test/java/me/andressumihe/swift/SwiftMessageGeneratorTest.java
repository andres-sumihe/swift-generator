package me.andressumihe.swift;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for SwiftMessageGenerator
 */
public class SwiftMessageGeneratorTest {

    private SwiftMessageGenerator generator = new SwiftMessageGenerator();

    @Test
    public void testGenerateMT103() {
        String mt103Message = generator.generateMT103();
        
        // Verify the message is not null or empty
        assertNotNull("MT103 message should not be null", mt103Message);
        assertFalse("MT103 message should not be empty", mt103Message.isEmpty());
        
        // Verify it contains expected SWIFT message elements
        assertTrue("Should contain transaction reference", mt103Message.contains(":20:"));
        assertTrue("Should contain bank operation code", mt103Message.contains(":23B:"));
        assertTrue("Should contain amount", mt103Message.contains(":32A:"));
        assertTrue("Should contain ordering customer", mt103Message.contains(":50K:"));
        assertTrue("Should contain beneficiary", mt103Message.contains(":59:"));
        
        System.out.println("Generated MT103 for testing:");
        System.out.println(mt103Message);
    }

    @Test
    public void testMT103ContainsValidFields() {
        String mt103Message = generator.generateMT103();
        
        // Test specific field values
        assertTrue("Should contain reference REF123456789", mt103Message.contains("REF123456789"));
        assertTrue("Should contain CRED operation code", mt103Message.contains("CRED"));
        assertTrue("Should contain USD currency", mt103Message.contains("USD"));
        assertTrue("Should contain ordering customer JOHN DOE", mt103Message.contains("JOHN DOE"));
        assertTrue("Should contain beneficiary JANE SMITH", mt103Message.contains("JANE SMITH"));
    }
}
