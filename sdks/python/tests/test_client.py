import asyncio
import sys
import types
import unittest
from pathlib import Path
from unittest.mock import AsyncMock, patch

SRC_DIR = Path(__file__).resolve().parents[1] / "src"
package = types.ModuleType("jbroker_client")
package.__path__ = [str(SRC_DIR)]
sys.modules["jbroker_client"] = package

from jbroker_client.client import PyClient
from jbroker_client.exceptions import ConnectionError_, ProtocolError

BANNER = [
    b"Connected to client\n",
    b"-------------------\n",
    b"Server Info\n",
    b"Host : 127.0.0.1\n",
    b"Port : 4222\n",
]


class FakeStream:
    """Simulates a bidirectional stream for testing."""

    def __init__(self, responses: list[bytes] = None):
        self._responses = responses or []
        self._read_idx = 0
        self._written = bytearray()
        self._closed = asyncio.Event()

    async def readline(self) -> bytes:
        if self._read_idx < len(self._responses):
            resp = self._responses[self._read_idx]
            self._read_idx += 1
            return resp
        await self._closed.wait()
        return b""

    def write(self, data: bytes):
        self._written.extend(data)

    async def drain(self):
        pass

    def close(self):
        self._closed.set()

    async def wait_closed(self):
        pass


def make_client_and_fake(responses: list[bytes]) -> tuple[PyClient, FakeStream, AsyncMock]:
    """Factory: create a client + fake stream + mock open_connection."""
    fake = FakeStream([*BANNER, *responses])
    patcher = patch("asyncio.open_connection", new_callable=AsyncMock)
    mock_conn = patcher.start()
    mock_conn.return_value = (fake, fake)
    client = PyClient(max_retries=1, backoff_base=0.01)
    client.host = "127.0.0.1"
    client.port = 4222
    client.connect_timeout = client.timeout
    return client, fake, patcher


class TestJBrokerClientInit(unittest.TestCase):
    def test_default_params(self):
        client = PyClient()
        self.assertEqual(client.timeout, 10.0)
        self.assertEqual(client.max_retries, 3)

    def test_custom_params(self):
        client = PyClient(max_retries=5, backoff_base=2.0, cache_ttl=60)
        self.assertEqual(client.max_retries, 5)
        self.assertEqual(client.backoff_base, 2.0)


class TestConnect(unittest.IsolatedAsyncioTestCase):
    async def test_success(self):
        client, fake, patcher = make_client_and_fake([b"+OK\n"])
        try:
            result = await client.connect(client.host, client.port)
            self.assertIsNone(result)
            self.assertTrue(client.connected)
        finally:
            patcher.stop()

    async def test_retries_then_raises(self):
        patcher = patch("asyncio.open_connection", new_callable=AsyncMock)
        mock_conn = patcher.start()
        mock_conn.side_effect = OSError("Connection refused")
        client = PyClient(max_retries=2, backoff_base=0.01)
        client.host = "127.0.0.1"
        client.port = 4222
        client.connect_timeout = client.timeout
        try:
            with self.assertRaises(ConnectionError_):
                await client.connect(client.host, client.port)
            self.assertFalse(client.connected)
        finally:
            patcher.stop()

    async def test_wrong_response_raises(self):
        client, fake, patcher = make_client_and_fake([b"-ERR\n"])
        try:
            with self.assertRaises(ProtocolError):
                await client.connect(client.host, client.port)
            self.assertFalse(client.connected)
        finally:
            patcher.stop()

    async def test_reconnect_after_disconnect(self):
        first = FakeStream([*BANNER, b"+OK\n"])
        second = FakeStream([*BANNER, b"+OK\n"])
        patcher = patch("asyncio.open_connection", new_callable=AsyncMock)
        mock_conn = patcher.start()
        mock_conn.side_effect = [(first, first), (second, second)]
        client = PyClient(max_retries=1, backoff_base=0.01)
        client.host = "127.0.0.1"
        client.port = 4222
        client.connect_timeout = client.timeout
        try:
            await client.connect(client.host, client.port)
            await client.disconnect()
            result = await client.connect(client.host, client.port)
            self.assertIsNone(result)
            self.assertTrue(client.connected)
        finally:
            await client.disconnect()
            patcher.stop()


class TestSubscribe(unittest.IsolatedAsyncioTestCase):
    async def test_subscribe_success(self):
        client, fake, patcher = make_client_and_fake([b"+OK\n", b"Subscribed\n"])
        try:
            await client.connect(client.host, client.port)
            result = await client.subscribe("test.topic", 1)
            self.assertIsNone(result)
            written = bytes(fake._written).decode()
            self.assertIn("Sub test.topic 1", written)
        finally:
            patcher.stop()

    async def test_subscribe_fails_when_not_connected(self):
        client = PyClient()
        with self.assertRaises(ConnectionError_):
            await client.subscribe("x", 1)

    async def test_unsubscribe_success(self):
        client, fake, patcher = make_client_and_fake([b"+OK\n", b"Unsubbed\n"])
        try:
            await client.connect(client.host, client.port)
            result = await client.unsubscribe(1)
            self.assertIsNone(result)
            self.assertIn("Unsub 1", bytes(fake._written).decode())
        finally:
            patcher.stop()


class TestPublish(unittest.IsolatedAsyncioTestCase):
    async def test_publish_success(self):
        client, fake, patcher = make_client_and_fake([b"+OK\n", b"Published\n"])
        try:
            await client.connect(client.host, client.port)
            result = await client.publish("sports", "hello")
            self.assertIsNone(result)
            written = bytes(fake._written).decode()
            self.assertIn("Pub sports 5", written)
            self.assertIn("hello", written)
        finally:
            patcher.stop()

    async def test_repeated_publish_sends_each_message(self):
        client, fake, patcher = make_client_and_fake(
            [b"+OK\n", b"Published\n", b"Published\n"]
        )
        try:
            await client.connect(client.host, client.port)
            await client.publish("t", "msg")
            await client.publish("t", "msg")
            written = bytes(fake._written).decode()
            self.assertEqual(written.count("Pub t 3"), 2)
        finally:
            patcher.stop()

    async def test_publish_fails_when_not_connected(self):
        client = PyClient()
        with self.assertRaises(ConnectionError_):
            await client.publish("t", "m")


class TestContextManager(unittest.IsolatedAsyncioTestCase):
    async def test_connect_and_disconnect(self):
        client, fake, patcher = make_client_and_fake([b"+OK\n"])
        try:
            async with client:
                self.assertTrue(client.connected)
            self.assertFalse(client.connected)
        finally:
            patcher.stop()


class TestTTLCache(unittest.TestCase):
    def test_ttl_expiry(self):
        from jbroker_client.cache import TTLCache
        import time
        cache = TTLCache(default_ttl=0.1)
        cache.set("k", "v")
        self.assertEqual(cache.get("k"), "v")
        time.sleep(0.15)
        self.assertIsNone(cache.get("k"))

    def test_clear(self):
        from jbroker_client.cache import TTLCache
        cache = TTLCache()
        cache.set("a", 1)
        cache.set("b", 2)
        cache.clear()
        self.assertIsNone(cache.get("a"))
        self.assertIsNone(cache.get("b"))

    def test_contains(self):
        from jbroker_client.cache import TTLCache
        cache = TTLCache()
        cache.set("x", "y")
        self.assertIn("x", cache)
        self.assertNotIn("z", cache)


if __name__ == "__main__":
    unittest.main()
