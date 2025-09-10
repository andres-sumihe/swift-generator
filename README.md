# SWIFT MT Message Generator

Professional-grade SWIFT MT message generator built with Java 21 and Maven. Designed for financial institutions requiring high-volume, compliant SWIFT message generation for testing and development.

## Features

- **Multi-Message Support**: MT101, MT103, MT202, MT940, MT950
- **Bidirectional Flow**: Generate both INCOMING and OUTGOING messages
- **Enterprise Formatting**: DOS-PCC format with binary output support
- **High Performance**: Batch generation with configurable volume (1-10,000+ messages)
- **SWIFT Compliance**: Full adherence to SWIFT MT standards
- **Extensible Architecture**: Factory pattern with pluggable message generators

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.6+

### Build & Run
```bash
# Build standalone JAR
mvn clean package

# Generate 100 outgoing MT103 messages
java -jar target/swift-generator-standalone.jar -t MT103 -c 100 -d OUTGOING

# Generate with custom output directory
java -jar target/swift-generator-standalone.jar -t MT940 -c 50 -d INCOMING -o custom/path
```

### Command Line Options
```
-t, --type       Message type (MT101|MT103|MT202|MT940|MT950)
-c, --count      Number of messages (1-10000)
-d, --direction  Direction (INCOMING|OUTGOING)
-f, --format     Output format (DOS_PCC) [default]
-o, --output     Output directory [default: ./output]
```

## Message Types

| Type | Description | Fields |
|------|-------------|---------|
| **MT101** | Request for Transfer | Customer payment orders |
| **MT103** | Single Customer Credit Transfer | Standard wire transfers |
| **MT202** | General Financial Institution Transfer | Bank-to-bank transfers |
| **MT940** | Customer Statement Message | Account statements |
| **MT950** | Statement Message | Detailed account reporting |

## Output Formats

### DOS-PCC Format
- Binary sector-based format (512-byte sectors)
- Suitable for legacy mainframe systems
### RJE Format
- Dollar Separated Remote Job Entry for Alliance Access format batch input

## Architecture

```
SwiftMessageGenerator
├── MessageGeneratorFactory    # Factory pattern for message creation
├── MessageFormatterFactory    # Factory pattern for output formatting
├── AbstractMessageGenerator   # Template method base class
├── MT{Type}Generator          # Specific message generators
└── ConfigurationManager       # Centralized configuration
```

## Dependencies

- **[Prowide Core SRU2024](https://www.prowidesoftware.com/)** - SWIFT message processing
- **Apache Commons CLI** - Command line interface
- **JUnit 4** - Unit testing framework

## Development

```bash
# Run tests
mvn test

# Compile only
mvn compile

# Generate with debug logging
java -jar target/swift-generator-standalone.jar -t MT103 -c 1 -d OUTGOING
```

## License

Educational and testing purposes. See [Prowide Core License](https://www.prowidesoftware.com/products/prowide-core) for commercial usage terms.
