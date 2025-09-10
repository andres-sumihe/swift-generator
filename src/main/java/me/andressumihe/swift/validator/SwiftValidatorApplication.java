package me.andressumihe.swift.validator;

import org.apache.commons.cli.*;

import java.util.logging.Logger;

/**
 * SWIFT Message Validator Application
 * 
 * Command-line utility to validate deduplication program results.
 * 
 * Usage examples:
 * java -jar swift-validator.jar -i input/folder -o output/folder
 * java -jar swift-validator.jar --input generated_files --output deduplicated_files --verbose
 */
public class SwiftValidatorApplication {
    
    private static final Logger logger = Logger.getLogger(SwiftValidatorApplication.class.getName());
    
    public static void main(String[] args) {
        Options options = createCommandLineOptions();
        
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            
            if (cmd.hasOption("help")) {
                printHelp(options);
                return;
            }
            
            // Required parameters
            if (!cmd.hasOption("input") || !cmd.hasOption("output")) {
                System.err.println("Error: Both input and output directories are required.");
                printHelp(options);
                System.exit(1);
            }
            
            String inputDir = cmd.getOptionValue("input");
            String outputDir = cmd.getOptionValue("output");
            boolean verbose = cmd.hasOption("verbose");
            
            // Configure logging level
            if (verbose) {
                System.setProperty("java.util.logging.ConsoleHandler.level", "INFO");
                logger.info("Verbose mode enabled");
            }
            
            // Run validation
            runValidation(inputDir, outputDir, verbose);
            
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            printHelp(options);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Validation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Runs the SWIFT file validation process
     */
    private static void runValidation(String inputDir, String outputDir, boolean verbose) {
        System.out.println("SWIFT Message Deduplication Validator");
        System.out.println("=====================================");
        System.out.println("Input Directory:  " + inputDir);
        System.out.println("Output Directory: " + outputDir);
        System.out.println();
        
        // Create validator and run validation
        SwiftFileValidator validator = new SwiftFileValidator(inputDir, outputDir);
        SwiftFileValidator.ValidationReport report = validator.validateAll();
        
        // Print detailed report
        report.printReport();
        
        // Set exit code based on validation result
        if (report.isOverallValid()) {
            System.out.println("\n🎉 All validations PASSED! Your deduplication program works correctly.");
            System.exit(0);
        } else {
            System.out.println("\n❌ Validation FAILED! Please check the errors above.");
            System.exit(1);
        }
    }
    
    /**
     * Creates command line options
     */
    private static Options createCommandLineOptions() {
        Options options = new Options();
        
        Option inputOption = Option.builder("i")
                .longOpt("input")
                .desc("Input directory containing original SWIFT files from generator")
                .hasArg()
                .argName("INPUT_DIR")
                .required()
                .build();
        
        Option outputOption = Option.builder("o")
                .longOpt("output")
                .desc("Output directory containing deduplicated SWIFT files")
                .hasArg()
                .argName("OUTPUT_DIR")
                .required()
                .build();
        
        Option verboseOption = Option.builder("v")
                .longOpt("verbose")
                .desc("Enable verbose logging")
                .build();
        
        Option helpOption = Option.builder("h")
                .longOpt("help")
                .desc("Show this help message")
                .build();
        
        options.addOption(inputOption);
        options.addOption(outputOption);
        options.addOption(verboseOption);
        options.addOption(helpOption);
        
        return options;
    }
    
    /**
     * Prints help message
     */
    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("swift-validator", 
            "\nValidates SWIFT message deduplication results by comparing input and output files.\n" +
            "Since the generator creates unique messages, the output should be identical to input.\n", 
            options, 
            "\nExamples:\n" +
            "  java -jar swift-validator.jar -i output -o deduplicated_output\n" +
            "  java -jar swift-validator.jar --input generated_files --output processed_files --verbose\n" +
            "\nExpected behavior:\n" +
            "- Input files contain NO duplicates (verified)\n" +
            "- Output files should be IDENTICAL to input files\n" +
            "- Any differences indicate deduplication program issues\n");
    }
}
