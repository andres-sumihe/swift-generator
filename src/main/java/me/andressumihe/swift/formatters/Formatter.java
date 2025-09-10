package me.andressumihe.swift.formatters;

import com.prowidesoftware.swift.model.mt.AbstractMT;
import me.andressumihe.swift.exceptions.FormatException;

import java.util.List;

/**
 * Interface for SWIFT message formatters.
 */
public interface Formatter {
    
    String formatMessage(AbstractMT swiftMessage) throws FormatException;

    String formatBatch(List<AbstractMT> swiftMessages) throws FormatException;
}
