# JBroker

JBroker is a small message broker that follows the Nats protocol.

> JBroker is currently intended for learning, local experiments, and controlled
> tests. It does not provide persistence, acknowledgements, authentication,
> replay, clustering, or production-grade connection handling.

## Workflow


### 1. Start the broker

- For running the broker as a container directly,here are the steps you should follow.

```powershell
docker pull ghcr.io/saptarshi2001/jbroker:latest

docker run -d --name jbroker -p 4222:4222 ghcr.io/saptarshi2001/jbroker:latest
```

Wait for `System started`. The checked-in configuration uses TCP port `4222`.

- For running the source directly.
```powershell
git clone https://github.com/Saptarshi2001/JBroker.git
cd jbroker

mvn test
docker build -t jbroker:local .
docker run -d --name jbroker -p 4222:4222 jbroker:local
```
On macOS/Linux, ./mvnw test can replace mvn test.

### 2. Connect a subscriber

Open a TCP client such as netcat:

```bash
nc 127.0.0.1 4222
```

The broker sends a six-line banner, including a final blank line. Then enter:

```text
Connect {}
Sub demo 1
```

The responses are:

```text
+OK
Subscribed
```

### 3. Publish from another connection

Open a second TCP client, consume its banner, and enter:

```text
Connect {}
Pub demo 5
hello
```

The subscriber receives `hello`. The publisher receives no publish
acknowledgement.

### 4. Unsubscribe

On the subscriber connection:

```text
Unsub 1
```

The response is `Unsubbed`. Close both TCP connections and stop the broker with
`Ctrl+C`.

### For using the python sdks use this guide - [Python client SDK](sdks/python/README.md).
## How-to guides

### Build and test

```powershell
mvn clean test
docker build -t jbroker:local .
```
Execute the test with
```powershell
docker run -d --name jbroker -p 4222:4222 jbroker:local
```
The tests use port the `4222`.

### Configure the broker

Configuration is loaded from
`src/main/resources/application.properties`:

```properties
server.address=0.0.0.0
server.port=4222
```

`SERVER_ADDRESS` and `SERVER_PORT` are fallbacks only when their corresponding
classpath properties are absent. Because both properties are checked in,
environment variables alone do not override them.



### Common failures that might occur when you are using the broker

- **Address already in use:** stop the process occupying port `4222` before
  starting the broker or its integration tests.
- **Connection refused:** confirm `System started` appears and that the client
  uses the configured port.
- **Client hangs after connecting:** consume all six banner lines, terminate
  commands with a newline, and flush the client writer.
- **Message not delivered:** create the topic with `Sub` before publishing,
  keep the subscriber connected, and use a whitespace-free, single-line body.
- **Fifth connection appears idle:** four long-lived worker tasks process
  connections; later connections wait in a queue until a worker becomes free.


## Reference

### Wire protocol

The protocol is case-sensitive, newline-delimited TCP text. Use ASCII for
commands because the server uses the JVM platform-default character set.

| Command | Syntax | Success/result |
| --- | --- | --- |
| Connect | `Connect {}` | `+OK` |
| Subscribe | `Sub <topic> <subscriber-id>` | `Subscribed` |
| Publish | `Pub <topic> <length>` followed by a body line | Body sent to current subscribers 
| Unsubscribe | `Unsub <subscriber-id>` | `Unsubbed` |

Rules:

- subscribe topics accept letters, digits, `.`, and `*`;
- publish topics accept letters, digits, and `.`;
- subscriber IDs are globally unique within the running broker;
- topic matching is exact and case-sensitive; `*` is not a wildcard;
- topics are created by subscribing, not publishing;


Common responses:

| Condition | Response |
| --- | --- |
| Subscribe or publish before handshake | `First use Connect {}` |
| Duplicate subscriber ID | `Id already present` |
| Unknown unsubscribe ID | `Subscription not found` |
| Invalid connect | `Wrong Connect !!! Please type Connect{}` |
| Invalid subscribe | `Invalid sub message !!! Please type Sub [topic_name] [subscriber_id]` |
| Invalid publish | `Invalid pub message !!! Please type Pub [Topic_name][message_length]` |
| Unknown command | `Invalid message.Choose Connect,Sub,Pub,Unsub !!!` |

Publishing to an unknown topic produces no wire response. Although the internal
publisher returns `No topic found`, the parser does not send it to the client.

### Configuration and runtime

| Item | Current value |
| --- | --- |
| Main class | `com.jbroker.JbrokerApplication` |
| Java version | 21 |
| Worker threads | 4, not runtime configurable |
| Parser task queue | 10, not runtime configurable |
| State storage | In memory |
| Default port | `4222` |

### Main Java components

- `JbrokerApplication`: loads configuration and starts the server.
- `Server`: accepts sockets and owns shared topics and subscription IDs.
- `Parser`: validates and dispatches protocol lines for one connection.
- `Subscriber`: creates subscriptions and removes subscriber IDs.
- `Publisher`: finds an exact topic and fans out a message.
- `Topic`: stores the topic name and client delivery list.
- `Router`: writes newline-terminated messages to client sockets.
- `ThreadPool` and `Worker`: process connection parser tasks.
- `Client` and `Message`: carry connection and message state.
