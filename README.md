# jbroker

A lightweight, TCP-based message broker written in Java that implements a simple publish-subscribe (pub/sub) messaging pattern. This project serves as both a functional message broker for basic messaging needs.

## Features

- **Simple TCP Protocol**: Easy-to-understand text-based protocol for client communication
- **Publish-Subscribe Pattern**: Support for multiple subscribers to receive messages from topics
- **Topic Management**: Dynamic topic creation and management
- **Multi-threaded Architecture**: Thread pool for handling concurrent client connections
- **Message Routing**: Efficient message distribution to subscribed clients
- **Connection Management**: Client connection handling with validation

## Protocol Commands

The broker supports the following commands:

### Connect
Establishes a connection to the broker.
```
Connect {}
```
**Response**: `+OK` on successful connection

### Subscribe (Sub)
Subscribes a client to a topic with a unique subscriber ID.
```
Sub [topic_name] [subscriber_id]
```
**Response**: `Subscribed` on success, error message on failure

### Publish (Pub)
Publishes a message to a topic. The message content is sent on the next line.
```
Pub [topic_name] [message_length]
[message_content]
```
**Response**: Message delivered to all subscribers, or `No topic found` if topic doesn't exist

### Unsubscribe (Unsub)
Unsubscribes a client from a topic using their subscriber ID.
```
Unsub [subscriber_id]
```
**Response**: `Unsubbed` on success, error message on failure

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd jbroker
```

2. Build the project:
```bash
mvn clean package
```

### Running the Broker

#### Using Maven
```bash
mvn exec:java -Dexec.mainClass="com.jbroker.JbrokerApplication"
```

#### Using Java directly
```bash
java -cp target/classes com.jbroker.JbrokerApplication
```

### Configuration

The broker can be configured via `src/main/resources/application.properties`:

```properties
server.address=127.0.0.1
server.port=4222
```

Or via environment variables:
- `SERVER_ADDRESS` - Bind address (default: 127.0.0.1)
- `SERVER_PORT` - Bind port (default: 4222)

Example with custom port:
```bash
export SERVER_PORT=4333
java -cp target/classes com.jbroker.JbrokerApplication
```

## Usage Examples

### Basic Client Communication

1. **Connect to broker:**
```
Connect {}
```
Response: `+OK`

2. **Subscribe to a topic:**
```
Sub news 101
```
Response: `Subscribed`

3. **Publish a message:**
```
Pub news 11
Breaking news
```
Response: All subscribers receive "Breaking news"

4. **Unsubscribe:**
```
Unsub 101
```
Response: `Unsubbed`

### Multiple Subscribers

Multiple clients can subscribe to the same topic and receive all published messages:

**Client 1:**
```
Connect {}
Sub sports 201
```

**Client 2:**
```
Connect {}
Sub sports 202
```

**Publisher:**
```
Connect {}
Pub sports 9
Game starts
```

Both clients 201 and 202 will receive "Game starts".

## Architecture

### Core Components

- **Server**: Main server class that accepts client connections and manages the thread pool
- **ProtocolParser**: Parses incoming client commands and validates protocol syntax
- **Router**: Handles message routing between publishers and subscribers
- **Topic**: Represents a message topic with its subscribers
- **Subscriber**: Individual client subscription to a topic
- **Publisher**: Handles message publishing to topics
- **ThreadPool**: Manages worker threads for concurrent client handling
- **Client**: Represents a connected client

### Thread Safety

The broker uses thread-safe collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`) to handle concurrent access from multiple clients.

## Testing

Run the test suite:
```bash
mvn test
```

The project includes comprehensive test coverage:

- **Unit Tests**: Testing individual components in isolation
- **Integration Tests**: End-to-end testing of the protocol and client-server interactions
  - `IntegrationTest.java` - Basic connection and command testing
  - `PubIntegrationTest.java` - Message delivery, validation, and error handling
  - `SubUnsubIntegration.java`- Integration testing for sub and unsub
- **Property Testing**: Using jqwik for property-based testing to verify system behavior under various conditions

### Test Coverage

The integration tests verify:
- Client connection establishment and validation
- Topic subscription and unsubscription
- Message publishing and delivery to multiple subscribers
- Error handling for invalid commands and unknown topics
- Protocol validation and response formatting
- Concurrent client handling

## Project Structure

```
src/
├── main/java/com/jbroker/
│   ├── Client.java              # Client connection representation
│   ├── JbrokerApplication.java  # Main application entry point
│   ├── Message.java             # Message representation
│   ├── ProtocolParser.java      # Protocol parsing and validation
│   ├── Publisher.java           # Message publishing logic
│   ├── Router.java              # Message routing
│   ├── Server.java              # Main server implementation
│   ├── SendMessage.java         # Message sending functionality
│   ├── Subscriber.java          # Subscription management
│   ├── ThreadPool.java          # Thread pool management
│   ├── Topic.java               # Topic representation
│   └── Worker.java              # Worker thread implementation
├── main/resources/
│   └── application.properties   # Configuration file
└── test/java/com/jbroker/       # Test classes
    ├── IntegrationTest.java     # Basic connection and command testing
    ├── PublishIntegrationTest.java  # Advanced publish-subscribe scenarios
    ├── PubIntegrationTest.java      # Message delivery and validation testing
    ├── SubUnsubIntegration.java     # Subscription/unsubscription testing
    ├── Testbroker.java              # Additional broker testing
    └── TestParser.java              # Protocol parser testing
```

## Development

### Adding New Features

1. Implement the feature in the appropriate component
2. Add protocol command parsing in `ProtocolParser.java`
3. Write tests for the new functionality
4. Update this README if needed

### Code Style

The project follows standard Java conventions and uses:
- Spring Boot for dependency management
- JUnit 5 for testing
- Mockito for mocking in tests
- jqwik for property-based testing

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for your changes
5. Submit a pull request

## License

This project is currently unlicensed. Please add a LICENSE file if you plan to distribute or share this project.

## Troubleshooting

### Common Issues

1. **Port already in use**: Ensure the configured port (default 4222) is not being used by another application
2. **Connection refused**: Verify the broker is running and accessible
3. **Protocol errors**: Check command syntax matches the expected format

### Debugging

The broker includes logging that can help diagnose issues. Check the console output for error messages.

## Future Enhancements

Potential improvements that could be added:
- Message persistence
- Authentication and authorization
- Message acknowledgments
- Quality of Service (QoS) levels
- Topic wildcards and patterns
- Web interface for monitoring
- Metrics and monitoring
- Clustering support

## Performance Considerations

- The current implementation is designed for simplicity and educational purposes
- For production use, consider adding connection limits, message size limits, and enhanced error handling
- The thread pool size is configurable but may need tuning for high-load scenarios