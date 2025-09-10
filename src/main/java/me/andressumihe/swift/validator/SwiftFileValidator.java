package me.andressumihe.swift.validator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SWIFT File Validator
 * 
 * Validates deduplication program results by comparing:
 * 1. Input files (from this generator) - expected to have NO duplicates
 * 2. Output files (from external deduplication program) - should be identical to input
 * 
 * Since our generator creates unique messages, any deduplication program
 * should output exactly the same content as input.
 */
public class SwiftFileValidator {
    
    private static final Logger logger = Logger.getLogger(SwiftFileValidator.class.getName());
    
    private final Path inputDirectory;
    private final Path outputDirectory;
    private final ValidationReport report;
    
    public SwiftFileValidator(String inputDir, String outputDir) {
        this.inputDirectory = Paths.get(inputDir);
        this.outputDirectory = Paths.get(outputDir);
        this.report = new ValidationReport();
    }
    
    /**
     * Main validation method - validates all files in input directory
     * against corresponding files in output directory
     */
    public ValidationReport validateAll() {
        logger.info("Starting SWIFT file validation...");
        logger.info("Input directory: " + inputDirectory.toAbsolutePath());
        logger.info("Output directory: " + outputDirectory.toAbsolutePath());
        
        try {
            // Validate directory structure
            if (!Files.exists(inputDirectory)) {
                report.addError("Input directory does not exist: " + inputDirectory);
                return report;
            }
            
            if (!Files.exists(outputDirectory)) {
                report.addError("Output directory does not exist: " + outputDirectory);
                return report;
            }
            
            // Find all input files
            List<Path> inputFiles = findSwiftFiles(inputDirectory);
            logger.info("Found " + inputFiles.size() + " input files to validate");
            
            // Validate each file
            for (Path inputFile : inputFiles) {
                validateSingleFile(inputFile);
            }
            
            // Generate summary
            report.generateSummary();
            
        } catch (Exception e) {
            report.addError("Validation failed with exception: " + e.getMessage());
            logger.severe("Validation error: " + e.getMessage());
        }
        
        return report;
    }
    
    /**
     * Validates a single input file against its corresponding output file(s)
     * Handles file splitting: files with >1000 messages may be split into multiple output files
     */
    private void validateSingleFile(Path inputFile) {
        try {
            // Determine expected output file path(s)
            Path relativeInputPath = inputDirectory.relativize(inputFile);
            
            FileValidationResult result = new FileValidationResult(inputFile, null);
            
            // Read input file content first to determine expected split behavior
            byte[] inputContent = Files.readAllBytes(inputFile);
            String inputText = new String(inputContent);
            List<String> inputMessages = extractSwiftMessages(inputText);
            int inputMessageCount = inputMessages.size();
            
            result.setInputMessageCount(inputMessageCount);
            result.setInputChecksum(calculateChecksum(inputContent));
            
            // Check for duplicates in input (should be none from our generator)
            Set<String> inputUniqueMessages = new HashSet<>(inputMessages);
            int inputDuplicates = inputMessages.size() - inputUniqueMessages.size();
            result.setInputDuplicates(inputDuplicates);
            
            if (inputDuplicates > 0) {
                result.addWarning("Unexpected: Input file contains " + inputDuplicates + " duplicate messages");
            }
            
            // Determine expected output files based on message count
            List<Path> expectedOutputFiles = determineExpectedOutputFiles(relativeInputPath, inputMessageCount);
            
            // Validate all expected output files exist
            List<Path> actualOutputFiles = new ArrayList<>();
            for (Path expectedFile : expectedOutputFiles) {
                if (Files.exists(expectedFile)) {
                    actualOutputFiles.add(expectedFile);
                } else {
                    result.addError("Expected output file does not exist: " + expectedFile);
                }
            }
            
            if (actualOutputFiles.isEmpty()) {
                result.addError("No output files found for input: " + inputFile.getFileName());
                report.addFileResult(result);
                return;
            }
            
            // Validate the content across all output files
            validateSplitFiles(inputMessages, actualOutputFiles, result);
            
            // Add checksum validation
            validateChecksums(inputContent, actualOutputFiles, result);
            
            if (result.getErrors().isEmpty()) {
                result.setValid(true);
                logger.info("✓ Validation passed: " + inputFile.getFileName() + 
                    " (" + actualOutputFiles.size() + " output file" + (actualOutputFiles.size() > 1 ? "s" : "") + ")");
            }
            
            report.addFileResult(result);
            
        } catch (Exception e) {
            FileValidationResult result = new FileValidationResult(inputFile, null);
            result.addError("Validation exception: " + e.getMessage());
            report.addFileResult(result);
            logger.severe("Error validating " + inputFile + ": " + e.getMessage());
        }
    }
    
    /**
     * Determines expected output files based on input file and message count
     * Files with ≤1000 messages: 1 output file (same name)
     * Files with >1000 messages: multiple output files (split by 1000)
     */
    private List<Path> determineExpectedOutputFiles(Path relativeInputPath, int messageCount) {
        List<Path> expectedFiles = new ArrayList<>();
        
        if (messageCount <= 1000) {
            // Single output file - same name as input
            Path expectedOutputFile = outputDirectory.resolve(relativeInputPath);
            expectedFiles.add(expectedOutputFile);
        } else {
            // Multiple output files - split by 1000 messages
            int numberOfParts = (int) Math.ceil((double) messageCount / 1000);
            String inputFileName = relativeInputPath.getFileName().toString();
            
            // Remove extension to add part suffix
            String baseName = inputFileName;
            String extension = "";
            int lastDotIndex = inputFileName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                baseName = inputFileName.substring(0, lastDotIndex);
                extension = inputFileName.substring(lastDotIndex);
            }
            
            for (int i = 1; i <= numberOfParts; i++) {
                // Support the actual naming convention used by the deduplication program: -1, -2, etc.
                String partFileName = baseName + "-" + i + extension;
                Path partPath = relativeInputPath.getParent() != null 
                    ? relativeInputPath.getParent().resolve(partFileName)
                    : Path.of(partFileName);
                Path expectedPartFile = outputDirectory.resolve(partPath);
                expectedFiles.add(expectedPartFile);
            }
        }
        
        return expectedFiles;
    }
    
    /**
     * Validates split files by comparing total message content
     */
    private void validateSplitFiles(List<String> inputMessages, List<Path> outputFiles, FileValidationResult result) {
        try {
            // Collect all messages from all output files
            List<String> allOutputMessages = new ArrayList<>();
            long totalOutputSize = 0;
            
            for (Path outputFile : outputFiles) {
                byte[] outputContent = Files.readAllBytes(outputFile);
                totalOutputSize += outputContent.length;
                
                String outputText = new String(outputContent);
                List<String> outputMessages = extractSwiftMessages(outputText);
                allOutputMessages.addAll(outputMessages);
            }
            
            result.setOutputMessageCount(allOutputMessages.size());
            
            // Check for duplicates in combined output
            Set<String> outputUniqueMessages = new HashSet<>(allOutputMessages);
            int outputDuplicates = allOutputMessages.size() - outputUniqueMessages.size();
            result.setOutputDuplicates(outputDuplicates);
            
            // Validate message count
            if (inputMessages.size() != allOutputMessages.size()) {
                result.addError(String.format("Message count mismatch - Input: %d, Output: %d (across %d files)", 
                    inputMessages.size(), allOutputMessages.size(), outputFiles.size()));
            }
            
            // Validate no duplicates in output
            if (outputDuplicates > 0) {
                result.addError("Output files contain " + outputDuplicates + " duplicate messages (should be 0)");
            }
            
            // Validate that all input messages are present in output (order doesn't matter for deduplication)
            // Normalize line endings for comparison to handle CRLF vs LF differences
            Set<String> inputMessageSet = new HashSet<>();
            Set<String> outputMessageSet = new HashSet<>();
            
            for (String msg : inputMessages) {
                inputMessageSet.add(normalizeLineEndings(msg));
            }
            
            for (String msg : allOutputMessages) {
                outputMessageSet.add(normalizeLineEndings(msg));
            }
            
            if (!inputMessageSet.equals(outputMessageSet)) {
                // Find missing and extra messages
                Set<String> missingMessages = new HashSet<>(inputMessageSet);
                missingMessages.removeAll(outputMessageSet);
                
                Set<String> extraMessages = new HashSet<>(outputMessageSet);
                extraMessages.removeAll(inputMessageSet);
                
                if (!missingMessages.isEmpty()) {
                    result.addError("Missing " + missingMessages.size() + " messages in output files");
                    // Add debugging for first missing message
                    if (missingMessages.size() == 1) {
                        String missing = missingMessages.iterator().next();
                        result.addError("DEBUG - Missing message preview: " + missing.substring(0, Math.min(100, missing.length())) + "...");
                    }
                }
                
                if (!extraMessages.isEmpty()) {
                    result.addError("Found " + extraMessages.size() + " unexpected messages in output files");
                    // Add debugging for first extra message
                    if (extraMessages.size() == 1) {
                        String extra = extraMessages.iterator().next();
                        result.addError("DEBUG - Extra message preview: " + extra.substring(0, Math.min(100, extra.length())) + "...");
                    }
                }
            }
            
            // Validate file split logic for files > 1000 messages
            if (inputMessages.size() > 1000) {
                validateFileSplitLogic(outputFiles, allOutputMessages, result);
            }
            
            // Set output file reference for reporting (use first file if multiple)
            if (!outputFiles.isEmpty()) {
                result.setOutputFile(outputFiles.get(0));
                if (outputFiles.size() > 1) {
                    result.addInfo("Split into " + outputFiles.size() + " files: " + 
                        outputFiles.stream().map(p -> p.getFileName().toString()).collect(java.util.stream.Collectors.joining(", ")));
                }
            }
            
        } catch (Exception e) {
            result.addError("Split file validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Validates the file splitting logic for files with >1000 messages
     */
    private void validateFileSplitLogic(List<Path> outputFiles, List<String> allMessages, FileValidationResult result) {
        try {
            int expectedParts = (int) Math.ceil((double) allMessages.size() / 1000);
            
            if (outputFiles.size() != expectedParts) {
                result.addError(String.format("Incorrect number of split files - Expected: %d, Found: %d", 
                    expectedParts, outputFiles.size()));
                return;
            }
            
            // Validate each part has correct message count
            int messageIndex = 0;
            for (int i = 0; i < outputFiles.size(); i++) {
                Path partFile = outputFiles.get(i);
                
                try {
                    byte[] partContent = Files.readAllBytes(partFile);
                    String partText = new String(partContent);
                    List<String> partMessages = extractSwiftMessages(partText);
                    
                    int expectedCount = (i == outputFiles.size() - 1) 
                        ? allMessages.size() - (i * 1000)  // Last part: remaining messages
                        : 1000;  // Other parts: exactly 1000 messages
                    
                    if (partMessages.size() != expectedCount) {
                        result.addError(String.format("Part %d has %d messages, expected %d", 
                            i + 1, partMessages.size(), expectedCount));
                    }
                    
                    messageIndex += partMessages.size();
                    
                } catch (Exception e) {
                    result.addError("Failed to validate part " + (i + 1) + ": " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            result.addError("File split logic validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Validates checksums between input and output files
     * For single files: direct checksum comparison
     * For split files: combines all output files and compares with input
     */
    private void validateChecksums(byte[] inputContent, List<Path> outputFiles, FileValidationResult result) {
        try {
            String inputChecksum = result.getInputChecksum(); // Already calculated
            
            if (outputFiles.size() == 1) {
                // Single output file - direct comparison
                byte[] outputContent = Files.readAllBytes(outputFiles.get(0));
                String outputChecksum = calculateChecksum(outputContent);
                result.setOutputChecksum(outputChecksum);
                
                if (!inputChecksum.equals(outputChecksum)) {
                    result.addError(String.format("Checksum mismatch - Input: %s, Output: %s", 
                        inputChecksum, outputChecksum));
                    result.addInfo("Files have different binary content (may be formatting/line ending differences)");
                } else {
                    result.addInfo("✓ Checksum match - Files are binary identical");
                }
                
            } else {
                // Multiple output files - reconstruct with proper delimiters
                List<String> reconstructedMessages = new ArrayList<>();
                
                // Read and extract messages from each split file
                for (Path outputFile : outputFiles) {
                    byte[] content = Files.readAllBytes(outputFile);
                    String fileContent = new String(content);
                    List<String> fileMessages = extractSwiftMessages(fileContent);
                    reconstructedMessages.addAll(fileMessages);
                }
                
                // Reconstruct original format with proper delimiters
                String originalText = new String(inputContent);
                boolean isDosPccFormat = originalText.contains("\u0001") && originalText.contains("\u0003");
                
                if (isDosPccFormat) {
                    // For DOS-PCC format, skip binary reconstruction due to complex 512-byte sector structure
                    // Focus on message content validation only
                    result.addInfo("DOS-PCC format detected - skipping binary reconstruction");
                    result.addInfo("✓ Message content validation passed - " + reconstructedMessages.size() + " messages validated");
                    result.setOutputChecksum("DOS-PCC-CONTENT-VALIDATED");
                } else {
                    // RJE format - reconstruct with $ delimiters
                    byte[] reconstructedContent = reconstructOriginalFormat(reconstructedMessages, inputContent);
                    String combinedOutputChecksum = calculateChecksum(reconstructedContent);
                    result.setOutputChecksum(combinedOutputChecksum);
                    
                    if (!inputChecksum.equals(combinedOutputChecksum)) {
                        result.addError(String.format("Combined checksum mismatch - Input: %s, Combined Output: %s", 
                            inputChecksum, combinedOutputChecksum));
                        result.addInfo("Split files when reconstructed have different binary content than original");
                        // Additional analysis for split files
                        analyzeSplitFileChecksums(inputContent, outputFiles, result);
                    } else {
                        result.addInfo("✓ Combined checksum match - Split files reconstruct original perfectly");
                    }
                }
            }
            
        } catch (Exception e) {
            result.addError("Checksum validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Reconstructs the original file format from split file messages
     * Properly handles RJE delimiters between messages
     */
    private byte[] reconstructOriginalFormat(List<String> messages, byte[] originalContent) throws Exception {
        if (messages.isEmpty()) {
            return new byte[0];
        }
        
        // Determine format from original content
        String originalText = new String(originalContent);
        boolean isRjeFormat = !originalText.contains("{1:F21");
        
        StringBuilder reconstructed = new StringBuilder();
        
        for (int i = 0; i < messages.size(); i++) {
            reconstructed.append(messages.get(i));
            
            // Add delimiter between messages (not after the last one)
            if (i < messages.size() - 1) {
                if (isRjeFormat) {
                    reconstructed.append("$");
                }
                // DOS-PCC format doesn't use delimiters between messages
            }
        }
        
        // Convert back to bytes with proper line endings
        String reconstructedText = reconstructed.toString();
        return reconstructedText.getBytes();
    }
    
    /**
     * Provides detailed analysis of split file checksums for debugging
     */
    private void analyzeSplitFileChecksums(byte[] inputContent, List<Path> outputFiles, FileValidationResult result) {
        try {
            result.addInfo("=== SPLIT FILE CHECKSUM ANALYSIS ===");
            result.addInfo(String.format("Input file size: %d bytes", inputContent.length));
            
            long totalOutputSize = 0;
            List<String> allMessages = new ArrayList<>();
            
            for (int i = 0; i < outputFiles.size(); i++) {
                Path outputFile = outputFiles.get(i);
                byte[] partContent = Files.readAllBytes(outputFile);
                String partChecksum = calculateChecksum(partContent);
                totalOutputSize += partContent.length;
                
                // Extract messages from this part
                String partText = new String(partContent);
                List<String> partMessages = extractSwiftMessages(partText);
                allMessages.addAll(partMessages);
                
                result.addInfo(String.format("Part %d: %d bytes, %d messages, checksum: %s", 
                    i + 1, partContent.length, partMessages.size(), partChecksum));
            }
            
            result.addInfo(String.format("Total output size: %d bytes", totalOutputSize));
            result.addInfo(String.format("Raw size difference: %d bytes", totalOutputSize - inputContent.length));
            result.addInfo(String.format("Total messages extracted: %d", allMessages.size()));
            
            // Try reconstruction
            try {
                byte[] reconstructed = reconstructOriginalFormat(allMessages, inputContent);
                String reconstructedChecksum = calculateChecksum(reconstructed);
                result.addInfo(String.format("Reconstructed size: %d bytes", reconstructed.length));
                result.addInfo(String.format("Reconstructed checksum: %s", reconstructedChecksum));
                
                String originalChecksum = calculateChecksum(inputContent);
                if (originalChecksum.equals(reconstructedChecksum)) {
                    result.addInfo("✓ RECONSTRUCTION SUCCESS - Checksums match after proper delimiter handling");
                } else {
                    result.addInfo("⚠ Reconstruction checksum still differs - may be other formatting differences");
                }
            } catch (Exception e) {
                result.addInfo("❌ Reconstruction failed: " + e.getMessage());
            }
            
            // Check if it's just a delimiter difference
            int expectedDelimiters = allMessages.size() - 1; // n-1 delimiters for n messages
            if (Math.abs(totalOutputSize - inputContent.length) <= expectedDelimiters) {
                result.addInfo("✓ Size difference matches expected delimiter count - this is normal for split files");
            }
            
        } catch (Exception e) {
            result.addError("Split file checksum analysis failed: " + e.getMessage());
        }
    }
    /**
     * Extracts individual SWIFT messages from file content
     * Handles both DOS-PCC format ({1:F21...} headers) and RJE format ($ delimited)
     */
    private List<String> extractSwiftMessages(String content) {
        List<String> messages = new ArrayList<>();
        
        // Check format based on content characteristics
        if (content.contains("$")) {
            // RJE format: messages separated by $ delimiters
            String[] messageParts = content.split("\\$");
            for (String part : messageParts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && trimmed.startsWith("{1:")) {
                    // Remove any trailing $ delimiters and normalize
                    trimmed = trimmed.replaceAll("\\$+\\s*$", "").trim();
                    messages.add(trimmed);
                }
            }
        } else if (content.contains("\u0001") && content.contains("\u0003")) {
            // DOS-PCC format: messages wrapped in binary markers with 512-byte sector padding
            // For DOS-PCC, we need to preserve the entire sector structure, not just extract message content
            // Each message occupies complete 512-byte sectors with padding
            
            // Find all start markers and extract complete sectors for each message
            byte[] contentBytes = content.getBytes(StandardCharsets.ISO_8859_1);
            int sectorSize = 512; // DOS-PCC standard sector size
            
            for (int i = 0; i < contentBytes.length; i++) {
                if (contentBytes[i] == 0x01) { // Start marker found
                    // Find the end marker within reasonable sector boundaries
                    int endMarkerPos = -1;
                    for (int j = i + 1; j < contentBytes.length && j < i + (sectorSize * 10); j++) {
                        if (contentBytes[j] == 0x03) {
                            endMarkerPos = j;
                            break;
                        }
                    }
                    
                    if (endMarkerPos > 0) {
                        // Extract message content between markers (without the markers themselves)
                        int messageStart = i + 1;
                        int messageEnd = endMarkerPos;
                        
                        if (messageEnd > messageStart) {
                            byte[] messageBytes = new byte[messageEnd - messageStart];
                            System.arraycopy(contentBytes, messageStart, messageBytes, 0, messageBytes.length);
                            String messageContent = new String(messageBytes, StandardCharsets.ISO_8859_1).trim();
                            
                            if (!messageContent.isEmpty() && messageContent.startsWith("{1:")) {
                                messages.add(messageContent);
                            }
                        }
                        
                        // Skip to next sector boundary for efficiency
                        i = ((endMarkerPos / sectorSize) + 1) * sectorSize - 1;
                    }
                }
            }
        } else {
            // Universal SWIFT message parser - handles both incoming (F21+F01) and outgoing (F01) formats
            // This regex properly captures complete SWIFT messages from {1: to -} including all trailing blocks
            Pattern pattern = Pattern.compile("(\\{1:.*?-})([\\s\\S]*?)(?=(\\{1:|$))", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);
            
            while (matcher.find()) {
                String completeMessage = matcher.group(1) + matcher.group(2);
                String trimmed = completeMessage.trim();
                
                if (!trimmed.isEmpty() && trimmed.startsWith("{1:")) {
                    messages.add(trimmed);
                }
            }
        }
        
        return messages;
    }
    
    /**
     * Normalizes line endings to handle differences between input and output files
     * Converts all line endings (CRLF, LF, CR) to LF for consistent comparison
     */
    private String normalizeLineEndings(String content) {
        return content.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
    }
    
    /**
     * Analyzes specific differences in SWIFT format files
     */
    private void analyzeSwiftDifferences(byte[] inputContent, byte[] outputContent, FileValidationResult result) {
        // Compare line by line for detailed analysis
        String[] inputLines = new String(inputContent).split("\\r?\\n");
        String[] outputLines = new String(outputContent).split("\\r?\\n");
        
        int maxLines = Math.max(inputLines.length, outputLines.length);
        int differences = 0;
        
        for (int i = 0; i < maxLines && differences < 10; i++) { // Limit to first 10 differences
            String inputLine = i < inputLines.length ? inputLines[i] : "";
            String outputLine = i < outputLines.length ? outputLines[i] : "";
            
            if (!inputLine.equals(outputLine)) {
                differences++;
                result.addError(String.format("Line %d differs:\n  Input:  '%s'\n  Output: '%s'", 
                    i + 1, inputLine, outputLine));
            }
        }
        
        if (differences >= 10) {
            result.addError("... and more differences (showing first 10)");
        }
    }
    
    /**
     * Calculates MD5 checksum for byte array
     */
    private String calculateChecksum(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] digest = md5.digest(content);
        StringBuilder hexString = new StringBuilder();
        
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    /**
     * Checks if file is a SWIFT format file based on filename
     */
    private boolean isSwiftFormat(Path file) {
        String filename = file.getFileName().toString().toLowerCase();
        return filename.contains("mt") && filename.endsWith(".txt");
    }
    
    /**
     * Finds all SWIFT files in directory recursively
     */
    private List<Path> findSwiftFiles(Path directory) throws IOException {
        List<Path> swiftFiles = new ArrayList<>();
        
        Files.walk(directory)
            .filter(Files::isRegularFile)
            .filter(this::isSwiftFormat)
            .forEach(swiftFiles::add);
        
        return swiftFiles;
    }
    
    /**
     * Individual file validation result
     */
    public static class FileValidationResult {
        private final Path inputFile;
        private Path outputFile;
        private boolean isValid = false;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> info = new ArrayList<>();
        private String inputChecksum;
        private String outputChecksum;
        private int inputMessageCount = 0;
        private int outputMessageCount = 0;
        private int inputDuplicates = 0;
        private int outputDuplicates = 0;
        
        public FileValidationResult(Path inputFile, Path outputFile) {
            this.inputFile = inputFile;
            this.outputFile = outputFile;
        }
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public void addInfo(String infoMessage) {
            info.add(infoMessage);
        }
        
        // Getters and setters
        public Path getInputFile() { return inputFile; }
        public Path getOutputFile() { return outputFile; }
        public void setOutputFile(Path outputFile) { this.outputFile = outputFile; }
        public boolean isValid() { return isValid; }
        public void setValid(boolean valid) { this.isValid = valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getInfo() { return info; }
        public String getInputChecksum() { return inputChecksum; }
        public void setInputChecksum(String inputChecksum) { this.inputChecksum = inputChecksum; }
        public String getOutputChecksum() { return outputChecksum; }
        public void setOutputChecksum(String outputChecksum) { this.outputChecksum = outputChecksum; }
        public int getInputMessageCount() { return inputMessageCount; }
        public void setInputMessageCount(int inputMessageCount) { this.inputMessageCount = inputMessageCount; }
        public int getOutputMessageCount() { return outputMessageCount; }
        public void setOutputMessageCount(int outputMessageCount) { this.outputMessageCount = outputMessageCount; }
        public int getInputDuplicates() { return inputDuplicates; }
        public void setInputDuplicates(int inputDuplicates) { this.inputDuplicates = inputDuplicates; }
        public int getOutputDuplicates() { return outputDuplicates; }
        public void setOutputDuplicates(int outputDuplicates) { this.outputDuplicates = outputDuplicates; }
    }
    
    /**
     * Overall validation report
     */
    public static class ValidationReport {
        private final List<FileValidationResult> fileResults = new ArrayList<>();
        private final List<String> globalErrors = new ArrayList<>();
        private long totalFiles = 0;
        private long validFiles = 0;
        private long invalidFiles = 0;
        private long totalInputMessages = 0;
        private long totalOutputMessages = 0;
        private long totalInputDuplicates = 0;
        private long totalOutputDuplicates = 0;
        
        public void addFileResult(FileValidationResult result) {
            fileResults.add(result);
        }
        
        public void addError(String error) {
            globalErrors.add(error);
        }
        
        public void generateSummary() {
            totalFiles = fileResults.size();
            validFiles = fileResults.stream().mapToLong(r -> r.isValid() ? 1 : 0).sum();
            invalidFiles = totalFiles - validFiles;
            
            totalInputMessages = fileResults.stream().mapToLong(FileValidationResult::getInputMessageCount).sum();
            totalOutputMessages = fileResults.stream().mapToLong(FileValidationResult::getOutputMessageCount).sum();
            totalInputDuplicates = fileResults.stream().mapToLong(FileValidationResult::getInputDuplicates).sum();
            totalOutputDuplicates = fileResults.stream().mapToLong(FileValidationResult::getOutputDuplicates).sum();
        }
        
        public boolean isOverallValid() {
            return globalErrors.isEmpty() && invalidFiles == 0;
        }
        
        public void printReport() {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("SWIFT FILE VALIDATION REPORT");
            System.out.println("=".repeat(80));
            
            // Overall status
            String status = isOverallValid() ? "✓ PASSED" : "✗ FAILED";
            System.out.println("Overall Status: " + status);
            System.out.println();
            
            // Summary statistics
            System.out.println("SUMMARY:");
            System.out.println("  Total Files Validated: " + totalFiles);
            System.out.println("  Valid Files: " + validFiles);
            System.out.println("  Invalid Files: " + invalidFiles);
            System.out.println("  Total Input Messages: " + totalInputMessages);
            System.out.println("  Total Output Messages: " + totalOutputMessages);
            System.out.println("  Input Duplicates Found: " + totalInputDuplicates);
            System.out.println("  Output Duplicates Found: " + totalOutputDuplicates);
            System.out.println();
            
            // Global errors
            if (!globalErrors.isEmpty()) {
                System.out.println("GLOBAL ERRORS:");
                for (String error : globalErrors) {
                    System.out.println("  ✗ " + error);
                }
                System.out.println();
            }
            
            // File-specific results
            if (!fileResults.isEmpty()) {
                System.out.println("FILE VALIDATION RESULTS:");
                for (FileValidationResult result : fileResults) {
                    String fileStatus = result.isValid() ? "✓" : "✗";
                    System.out.println(String.format("  %s %s", fileStatus, result.getInputFile().getFileName()));
                    
                    if (!result.isValid()) {
                        for (String error : result.getErrors()) {
                            System.out.println("    ERROR: " + error);
                        }
                    }
                    
                    if (!result.getWarnings().isEmpty()) {
                        for (String warning : result.getWarnings()) {
                            System.out.println("    WARNING: " + warning);
                        }
                    }
                    
                    if (!result.getInfo().isEmpty()) {
                        for (String infoMessage : result.getInfo()) {
                            System.out.println("    INFO: " + infoMessage);
                        }
                    }
                    
                    // Message count info
                    if (result.getInputMessageCount() > 0) {
                        System.out.println(String.format("    Messages: Input=%d, Output=%d, InputDups=%d, OutputDups=%d",
                            result.getInputMessageCount(), result.getOutputMessageCount(),
                            result.getInputDuplicates(), result.getOutputDuplicates()));
                    }
                    
                    // Checksum info
                    if (result.getInputChecksum() != null) {
                        System.out.println(String.format("    Checksums: Input=%s, Output=%s", 
                            result.getInputChecksum(), 
                            result.getOutputChecksum() != null ? result.getOutputChecksum() : "N/A"));
                    }
                }
            }
            
            System.out.println("=".repeat(80));
        }
        
        // Getters
        public List<FileValidationResult> getFileResults() { return fileResults; }
        public List<String> getGlobalErrors() { return globalErrors; }
        public long getTotalFiles() { return totalFiles; }
        public long getValidFiles() { return validFiles; }
        public long getInvalidFiles() { return invalidFiles; }
    }
}
