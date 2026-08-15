# JBroker Python client SDK

`PyJbroker` 0.1.0 is an asynchronous Python client for the JBroker. The import package is `jbroker_client`, and its public client class is
`PyClient`.



## Tutorial and workflow

### 1. Install the SDK

```powershell
python -m pip install PyJbroker

```

### 2. Start the broker

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

### 3. Run a publish/subscribe workflow

Save this as `first_message.py` outside the SDK `src` directory:

```python
import asyncio

from PyJbroker import PyClient


HOST = "127.0.0.1"
PORT = 4222


async def main() -> None:
    subscriber = PyClient(host=HOST, port=PORT)
    publisher = PyClient(host=HOST, port=PORT)

    try:
        await subscriber.connect(HOST, PORT)
        await publisher.connect(HOST, PORT)

        await subscriber.subscribe("tutorial.greetings", subscriber_id=101)
        await asyncio.sleep(0.1)

        await publisher.publish("tutorial.greetings", "hello-from-python")
        await asyncio.sleep(0.5)

        await subscriber.unsubscribe(subscriber_id=101)
        await asyncio.sleep(0.1)
    finally:
        await subscriber.disconnect()
        await publisher.disconnect()


asyncio.run(main())
```

Run it:

```powershell
python first_message.py
```

Expected operational output includes:

```text
Subscribed to 'tutorial.greetings' as subscriber 101
Received message: hello-from-python
Unsubscribed subscriber 101
```

The sleeps allow the background listener to read and print acknowledgements.
The body contains no whitespace because the current broker truncates bodies at
their first whitespace-delimited token.

## How-to guides

### Manage a connection

Prefer the async context manager for short-lived work:

```python
async with PyClient(host="127.0.0.1", port=4222) as client:
    await client.subscribe("events.audit", subscriber_id=7)
```

For explicit lifecycle control:

```python
client = PyClient(host="127.0.0.1", port=4222)
try:
    await client.connect(client.host, client.port)
    await client.subscribe("events.audit", subscriber_id=7)
finally:
    await client.disconnect()
```

`connect()` requires explicit `host` and `port` arguments even though the
constructor stores them. Calling it while connected returns immediately.
`disconnect()` cancels the listener, closes the writer, and can be called when
already disconnected.

### Configure retries

```python
client = PyClient(
    host="127.0.0.1",
    port=4222,
    timeout=5.0,
    max_retries=5,
    backoff_base=0.25,
)
```

After failed attempt `n`, the delay is
`backoff_base * 2 ** (n - 1)`. Retries cover `OSError` and
`asyncio.TimeoutError` during connection attempts. There is no jitter, delay
cap, reconnect loop, or retry for publish/subscribe operations. The current
implementation also sleeps after the final failed attempt.

### Use the TTL cache

Every client owns a cache, but network operations do not use it automatically:

```python
client.cache.set("order:123", {"state": "created"}, ttl=60.0)
value = client.cache.get("order:123")
client.cache.remove("order:123")
client.cache.clear()
```

Expiration is lazy and based on `time.monotonic()`. The cache has no size bound,
persistence, synchronization, or background cleanup. `remove()` raises
`KeyError` for an absent key.

### Serialize and log SDK exceptions

```python
import logging

from jbroker_client.exceptions import JBrokerError


try:
    await client.connect(client.host, client.port)
except JBrokerError as exc:
    logging.error(exc.to_json())
```

`to_json()` returns:

```json
{
  "error_type": "ConnectionError_",
  "message": "Could not connect after 3 attempts",
  "timestamp": 1770000000.0
}
```

The SDK itself uses `print()` and does not configure Python logging or create a
log file. Applications own logging destinations and policies.

### Test and build

From `sdks/python`:

```powershell
python -B -m unittest discover -s tests -p "test_*.py"
python -m pip install build
python -m build
```

The end-to-end tests require `java` and `javac`. They compile the actual broker
into a temporary directory and run it on an available port. Build artifacts are
written to `dist`.

## Reference

### `PyClient`

```python
PyClient(
    host: str = "127.0.0.1",
    port: int = 4222,
    timeout: float = 10.0,
    max_retries: int = 3,
    backoff_base: float = 1.0,
    cache_ttl: float = 300.0,
)
```

| Interface | Behavior |
| --- | --- |
| `await connect(host, port)` | Opens the stream, consumes the banner, performs `Connect {}`, and starts `listen()` |
| `await disconnect()` | Cancels the listener and closes the stream |
| `await subscribe(topic, subscriber_id)` | Writes `Sub`; acknowledgement is printed asynchronously |
| `await publish(topic, message)` | Writes a `Pub` header and UTF-8 body; no acknowledgement is awaited |
| `await unsubscribe(subscriber_id)` | Writes `Unsub`; acknowledgement is printed asynchronously |
| `await listen(topic=None, subscriber_id=None)` | Background response reader; optional arguments are currently unused |
| `async with PyClient(...)` | Connects on entry and disconnects on exit |

All public network operations return `None` on their normal path. The listener
prints `Subscribed`, `Unsubbed`, a hypothetical `Published`, or any other line
as a received message. The current server does not send `Published`.

### Exceptions

Custom exceptions live in `jbroker_client.exceptions`:

| Exception | Current use |
| --- | --- |
| `JBrokerError` | Base class with `to_json()` |
| `ConnectionError_` | Retry exhaustion or operation while disconnected |
| `ProtocolError` | Missing banner, closed handshake, or unexpected connect response |
| `DisconnectError` | Writer shutdown failure |
| `AuthenticationError` | Defined but not currently raised by `PyClient` |
| `SubscriptionError` | Defined but not currently raised |
| `PublishError` | Defined but not currently raised |

Not every encoding, stream, cancellation, or argument error is converted to a
custom exception.

### `TTLCache`

`TTLCache(default_ttl=300.0)` provides `get`, `set`, `remove`, `clear`, and
membership testing. `get()` returns `None` for missing or expired entries.
Membership also treats a stored `None` as absent.


