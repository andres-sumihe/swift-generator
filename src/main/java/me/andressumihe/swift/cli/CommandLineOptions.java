package me.andressumihe.swift.cli;

import me.andressumihe.swift.model.enums.Direction;
import me.andressumihe.swift.model.enums.MessageType;
import me.andressumihe.swift.model.enums.OutputFormat;

/**
 * Value object representing parsed command line arguments.
 */
public class CommandLineOptions {
    
    private final MessageType messageType;
    private final int count;
    private final Direction direction;
    private final String outputDirectory;
    private final OutputFormat format;
    private final String fileExtension;
    
    public CommandLineOptions(MessageType messageType, int count, Direction direction, String outputDirectory, OutputFormat format, String fileExtension) {
        this.messageType = messageType;
        this.count = count;
        this.direction = direction;
        this.outputDirectory = outputDirectory;
        this.format = format;
        this.fileExtension = fileExtension;
    }
    
    public CommandLineOptions(MessageType messageType, int count, Direction direction, String outputDirectory, OutputFormat format) {
        this(messageType, count, direction, outputDirectory, format, null);
    }
    
    public CommandLineOptions(MessageType messageType, int count, Direction direction, String outputDirectory) {
        this(messageType, count, direction, outputDirectory, OutputFormat.DOS_PCC, null);
    }
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public int getCount() {
        return count;
    }
    
    public Direction getDirection() {
        return direction;
    }
    
    public String getOutputDirectory() {
        return outputDirectory;
    }
    
    public OutputFormat getFormat() {
        return format;
    }
    
    public String getFileExtension() {
        return fileExtension;
    }
    
    public boolean isIncoming() {
        return direction == Direction.INCOMING;
    }
    
    public boolean isOutgoing() {
        return direction == Direction.OUTGOING;
    }
    
    @Override
    public String toString() {
        return String.format("CommandLineOptions{messageType=%s, count=%d, direction=%s, outputDirectory='%s'}", 
            messageType, count, direction, outputDirectory);
    }
}
