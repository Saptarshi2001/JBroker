# JBroker Python Client SDK

Async Python client for [JBroker](https://github.com/anomalyco/jbroker).

## Installation

```bash
pip install jbroker-client
```

## Quick Start

```python
import asyncio
from jbroker_client import JBrokerClient


async def main():
    async with JBrokerClient(host="127.0.0.1", port=4222) as client:
        await client.subscribe("sports.football", subscriber_id=1)
        result = await client.publish("sports.football", "Goal!")
        print(result)  # JSON response
        await client.unsubscribe(subscriber_id=1)


asyncio.run(main())
```

## Features

- Fully async (asyncio)
- Automatic retry with exponential backoff
- Built-in message caching (TTL-based)
- Structured JSON error responses
- File + console logging
- Context manager support
