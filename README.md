# SWIFT MT Message Generator

A Java Maven project for generating SWIFT MT messages for testing purposes using the Prowide Core library.

## Overview

This project provides a simple way to generate various SWIFT MT (Message Type) messages that are commonly used in financial institutions for testing and development purposes. It uses the [Prowide Core](https://github.com/prowide/prowide-core) library, which is a comprehensive Java library for SWIFT message processing.

## Features

- Generate MT103 messages (Single Customer Credit Transfer)
- Extensible structure for additional MT message types (MT202, MT940, etc.)
- Unit tests included
- Maven-based build system

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

## Getting Started

### Build the Project

```bash
mvn clean compile
```

### Run Tests

```bash
mvn test
```

### Run the Application

```bash
mvn exec:java -Dexec.mainClass="me.andressumihe.swift.SwiftMessageGenerator"
```

Or compile and run the JAR:

```bash
mvn clean package
java -jar target/swift-generator-1.0-SNAPSHOT.jar
```

## Project Structure

```
swift-generator/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── me/
│   │           └── andressumihe/
│   │               └── swift/
│   │                   └── SwiftMessageGenerator.java
│   └── test/
│       └── java/
│           └── me/
│               └── andressumihe/
│                   └── swift/
│                       └── SwiftMessageGeneratorTest.java
├── pom.xml
└── README.md
```

## Supported Message Types

### MT103 - Single Customer Credit Transfer

The MT103 is a SWIFT message type that is used to send funds between banks on behalf of their customers. The generated message includes:

- Transaction Reference Number (Field 20)
- Bank Operation Code (Field 23B)
- Value Date, Currency Code, Amount (Field 32A)
- Ordering Customer details (Field 50K)
- Account With Institution (Field 57A)
- Beneficiary Customer details (Field 59)
- Remittance Information (Field 70)
- Details of Charges (Field 71A)

### Planned Message Types

- MT202 - General Financial Institution Transfer
- MT940 - Customer Statement Message
- Additional MT message types as needed

## Usage Example

```java
SwiftMessageGenerator generator = new SwiftMessageGenerator();
String mt103Message = generator.generateMT103();
System.out.println(mt103Message);
```

## Dependencies

- **Prowide Core**: The core library for SWIFT message processing
- **JUnit 4**: For unit testing

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is for educational and testing purposes. Please refer to the Prowide Core library license for commercial usage terms.

## Resources

- [Prowide Core Documentation](https://www.prowidesoftware.com/products/prowide-core)
- [SWIFT Standards](https://www.swift.com/standards)
- [Maven Documentation](https://maven.apache.org/guides/)
