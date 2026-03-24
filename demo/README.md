# jbroker Demo

This folder contains a demo project to test the jbroker message broker.

## Protocol (TCP, port 4222)

- **Connect:** `Connect {}` → broker prints `+OK`
- **Subscribe:** `Sub <topic> <subscription_id>` (e.g. `Sub news 1`)
- **Publish:** `Pub <topic> <subid> <length>` then on the next line send exactly `length` characters of payload
- **Unsubscribe:** `unsub <subscription_id>`

One connection can send multiple commands (line by line).

## Option 1: Interactive demo (DemoClient)

1. **Start the broker** (in one terminal):
   ```bash
   mvn spring-boot:run
   ```
   Or run the main class: `com.jbroker.JbrokerApplication`

2. **Start the demo client** (in another terminal):
   ```bash
   mvn exec:java -Dexec.mainClass="com.jbroker.demo.DemoClient"
   ```
   Or run: `com.jbroker.demo.DemoClient`

3. At the `>` prompt, type:
   - `connect` or `Connect {}` – connect
   - `sub news 1` – subscribe to topic `news` with id `1`
   - `pub news 1 5` – publish to `news` with subid `1`, payload length 5; then type the 5-character payload when asked
   - `unsub 1` – unsubscribe id 1
   - `quit` – exit

## Option 2: Automated demo (DemoRunner)

Runs the broker in the background and sends Connect, Sub, Pub, and unsub automatically.

```bash
mvn exec:java -Dexec.mainClass="com.jbroker.demo.DemoRunner"
```

No need to start the broker separately; the demo starts it as a daemon thread.

## Defaults

- Host: `localhost`
- Port: `4222`

Override from the command line:

```bash
mvn exec:java -Dexec.mainClass="com.jbroker.demo.DemoClient" -Dexec.args="localhost 4222"
```

Or run `DemoClient` with program arguments: `host [port]`.
