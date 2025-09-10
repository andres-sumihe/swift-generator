package me.andressumihe.swift.cli;

import me.andressumihe.swift.config.ConfigurationManager;
import me.andressumihe.swift.model.enums.Direction;
import me.andressumihe.swift.model.enums.MessageType;
import me.andressumihe.swift.model.enums.OutputFormat;
import org.apache.commons.cli.*;

/**
 * Command line argument parser for SWIFT message generation.
 */
public class CommandLineParser {
    
    private final ConfigurationManager config;
    private final Options options;
    
    public CommandLineParser(ConfigurationManager config) {
        this.config = config;
        this.options = createOptions();
    }
    
    /**
     * Create CLI options.
     */
    private Options createOptions() {
        Options opts = new Options();
        
        opts.addOption(Option.builder("t")
                .longOpt("type")
                .desc("SWIFT message type (101, 103, 202, 940, 950, MT101, MT103, MT202, MT940, MT950)")
                .hasArg()
                .argName("TYPE")
                .build());
                
        opts.addOption(Option.builder("c")
                .longOpt("count")
                .desc("Number of messages to generate")
                .hasArg()
                .argName("COUNT")
                .build());
                
        opts.addOption(Option.builder("d")
                .longOpt("direction")
                .desc("Message direction: INCOMING, OUTGOING, IN, OUT (default: OUTGOING)")
                .hasArg()
                .argName("DIRECTION")
                .build());
                
        opts.addOption(Option.builder("o")
                .longOpt("output")
                .desc("Output directory (default: ./output)")
                .hasArg()
                .argName("DIRECTORY")
                .build());
                
        opts.addOption(Option.builder("f")
                .longOpt("format")
                .desc("Output format: DOS_PCC, PLAIN (default: DOS_PCC)")
                .hasArg()
                .argName("FORMAT")
                .build());
                
        opts.addOption(Option.builder("ext")
                .longOpt("extension")
                .desc("File extension (txt, inc, fin, rje, pcc, etc.)")
                .hasArg()
                .argName("EXTENSION")
                .build());
                
        opts.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Display help information")
                .build());
                
        return opts;
    }
    
    /**
     * Parse command line arguments into CommandLineOptions.
     * 
     * @param args Command line arguments
     * @return Parsed and validated command line options
     * @throws IllegalArgumentException if arguments are invalid
     */
    public CommandLineOptions parse(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("No arguments provided. Use --help for usage information.");
        }
        
        if (isUsingFlags(args)) {
            return parseFlagBased(args);
        } else {
            return parsePositional(args);
        }
    }
    
    /**
     * Determine if arguments use flag-based format.
     */
    private boolean isUsingFlags(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--") || (arg.startsWith("-") && arg.length() == 2)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Parse flag-based arguments.
     */
    private CommandLineOptions parseFlagBased(String[] args) {
        org.apache.commons.cli.CommandLineParser cliParser = new DefaultParser();
        
        try {
            CommandLine cmd = cliParser.parse(options, args);
            
            if (cmd.hasOption("help")) {
                displayUsage();
                System.exit(0);
            }
            
            MessageType messageType = parseFlagMessageType(cmd);
            int count = parseFlagCount(cmd);
            
            Direction direction = parseFlagDirection(cmd);
            String outputDirectory = cmd.getOptionValue("output", "./output");
            OutputFormat format = parseFlagFormat(cmd);
            String fileExtension = cmd.getOptionValue("extension");
            
            validateConstraints(count);
            
            return new CommandLineOptions(messageType, count, direction, outputDirectory, format, fileExtension);
            
        } catch (ParseException e) {
            throw new IllegalArgumentException("Error parsing command line: " + e.getMessage() + 
                "\nUse --help for usage information.");
        }
    }
    
    /**
     * Parse positional arguments.
     */
    private CommandLineOptions parsePositional(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Insufficient arguments. Use: <messageType> <count> [direction] [outputDirectory]");
        }
        
        MessageType messageType = parseMessageType(args[0]);
        
        int count = parseCount(args[1]);
        
        Direction direction = Direction.OUTGOING;
        String outputDirectory = "./output";
        
        if (args.length >= 3) {
            String thirdArg = args[2].trim();
            if ("in".equalsIgnoreCase(thirdArg) || "out".equalsIgnoreCase(thirdArg)) {
                direction = "in".equalsIgnoreCase(thirdArg) ? Direction.INCOMING : Direction.OUTGOING;
                
                // If direction is specified, check for output directory in 4th argument
                if (args.length >= 4) {
                    outputDirectory = args[3];
                }
            } else {
                outputDirectory = thirdArg;
            }
        }
        
        validateConstraints(count);
        
        return new CommandLineOptions(messageType, count, direction, outputDirectory);
    }
    
    
    /**
     * Parse message type from flag-based arguments.
     */
    private MessageType parseFlagMessageType(CommandLine cmd) {
        if (!cmd.hasOption("type")) {
            throw new IllegalArgumentException("Message type is required. Use --type or -t with value (101, 103, 202, 940, 950, MT101, MT103, MT202, MT940, MT950)");
        }
        return parseMessageType(cmd.getOptionValue("type"));
    }
    
    /**
     * Parse count from flag-based arguments.
     */
    private int parseFlagCount(CommandLine cmd) {
        if (!cmd.hasOption("count")) {
            throw new IllegalArgumentException("Count is required. Use --count or -c with a positive integer");
        }
        return parseCount(cmd.getOptionValue("count"));
    }
    
    /**
     * Parse direction from flag-based arguments.
     */
    private Direction parseFlagDirection(CommandLine cmd) {
        if (!cmd.hasOption("direction")) {
            return Direction.OUTGOING; // Default
        }
        
        String directionStr = cmd.getOptionValue("direction").trim().toUpperCase();
        switch (directionStr) {
            case "INCOMING":
            case "IN":
                return Direction.INCOMING;
            case "OUTGOING":
            case "OUT":
                return Direction.OUTGOING;
            default:
                throw new IllegalArgumentException("Invalid direction: " + directionStr + 
                    ". Supported values: INCOMING, OUTGOING, IN, OUT");
        }
    }
    
    /**
     * Parse format from flag-based arguments.
     */
    private OutputFormat parseFlagFormat(CommandLine cmd) {
        if (!cmd.hasOption("format")) {
            return OutputFormat.DOS_PCC; // Default
        }
        
        String formatStr = cmd.getOptionValue("format").trim().toUpperCase();
        switch (formatStr) {
            case "DOS_PCC":
            case "DOS-PCC":
            case "PCC":
                return OutputFormat.DOS_PCC;
            case "PLAIN":
            case "FIN":
                return OutputFormat.FIN;
            case "RJE":
                return OutputFormat.RJE;
            default:
                throw new IllegalArgumentException("Invalid format: " + formatStr + 
                    ". Supported values: DOS_PCC, PLAIN, FIN, RJE");
        }
    }
    
    /**
     * Display command line usage information.
     */
    public void displayUsage() {
        HelpFormatter formatter = new HelpFormatter();
        
        System.out.println("SWIFT Message Generator - Command Line Usage");
        System.out.println("===========================================");
        System.out.println();
        
        System.out.println("Usage:");
        formatter.printHelp("java -jar swift-generator.jar", options, true);
        System.out.println();
        
        System.out.println("Positional Usage:");
        System.out.println("java -jar swift-generator.jar <messageType> <count> [direction] [outputDirectory]");
        System.out.println();
        System.out.println("Parameters:");
        System.out.println("  messageType     - SWIFT message type (101, 103, 202, 940, or 950)");
        System.out.println("  count          - Number of messages to generate");
        System.out.println("  direction      - Message direction: 'out' (default) or 'in' (optional)");
        System.out.println("  outputDirectory - Output directory (optional, default: ./output)");
        System.out.println();
        
        System.out.println("Examples:");
        System.out.println("Flag Examples:");
        System.out.println("  java -jar swift-generator.jar --type MT940 --count 100 --direction INCOMING");
        System.out.println("  java -jar swift-generator.jar -t 103 -c 50 -d OUT -o /output");
        System.out.println("  java -jar swift-generator.jar --type 202 --count 25 --output C:\\temp");
        System.out.println("  java -jar swift-generator.jar -t MT103 -c 100 --ext txt        # Generate only .txt file");
        System.out.println("  java -jar swift-generator.jar -t MT940 -c 50 --ext fin         # Generate only .fin file");
        System.out.println("  java -jar swift-generator.jar -t MT202 -c 25 --ext pcc         # Generate only .pcc file");
        System.out.println();
        System.out.println("Positional Examples:");
        System.out.println("  java -jar swift-generator.jar 103 100              # Outgoing messages");
        System.out.println("  java -jar swift-generator.jar 103 100 in          # Incoming messages with Block 5");
        System.out.println("  java -jar swift-generator.jar 202 50 out /output  # Outgoing to specific directory");
        System.out.println("  java -jar swift-generator.jar 940 25 in C:\\temp   # Incoming with Block 5");
        System.out.println();
        System.out.println("Message Directions:");
        System.out.println("  out/OUTGOING - Messages from our system to SWIFT (default)");
        System.out.println("  in/INCOMING  - Messages from SWIFT to our backend (includes Block 5)");
        System.out.println();
        System.out.println("Supported Message Types:");
        System.out.println("  MT101 - Request for Transfer");
        System.out.println("  MT103 - Customer Credit Transfer");
        System.out.println("  MT202 - Financial Institution Transfer");
        System.out.println("  MT940 - Customer Statement Message");
        System.out.println("  MT950 - Statement Message");
    }
    
    private MessageType parseMessageType(String messageTypeStr) {
        String normalizedType = messageTypeStr.trim().toUpperCase();
        
        if (normalizedType.startsWith("MT")) {
            normalizedType = normalizedType.substring(2);
        }
        
        switch (normalizedType) {
            case "101":
                return MessageType.MT101;
            case "103":
                return MessageType.MT103;
            case "202":
                return MessageType.MT202;
            case "940":
                return MessageType.MT940;
            case "950":
                return MessageType.MT950;
            default:
                throw new IllegalArgumentException("Unsupported message type: " + messageTypeStr + 
                    ". Supported types: 101, 103, 202, 940, 950");
        }
    }
    
    private int parseCount(String countStr) {
        try {
            int count = Integer.parseInt(countStr.trim());
            if (count <= 0) {
                throw new IllegalArgumentException("Count must be positive");
            }
            return count;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid count: " + countStr + ". Must be a positive integer.");
        }
    }
    
    private void validateConstraints(int count) {
        if (count > config.getMaxMessages()) {
            throw new IllegalArgumentException(
                String.format("Message count %d exceeds maximum allowed %d", 
                    count, config.getMaxMessages()));
        }
    }
}