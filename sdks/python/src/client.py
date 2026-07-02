import asyncio
import time
from typing import Optional

from .exceptions import ConnectionError_,ProtocolError,SubscriptionError,DisconnectError,PublishError
from .protocol import Command,Response,encode_message,encode_publish_body,parse_response
from .cache import TTLCache


class PyClient:
    """Asynchronous client for the JBroker over a text-based TCP protocol.

    The JBroker protocol uses simple line-delimited commands:
      - Connect {}                     -> +OK
      - Sub <topic> <subscriber_id>   -> Subscribed
      - Pub <topic> <msg_len>         -> (expects message body on next line)
      - Unsub <subscriber_id>         -> Unsubbed
    """

    def __init__(self,host: str="127.0.0.1",port:int=4222,timeout: float = 10.0,max_retries: int = 3,backoff_base: float = 1.0,cache_ttl: float = 300.0):
        self.host=host
        self.port=port
        self.timeout = timeout
        self.max_retries = max_retries
        self.backoff_base = backoff_base
        self.reader: Optional[asyncio.StreamReader] = None
        self.writer: Optional[asyncio.StreamWriter] = None
        self.connected = False
        self.cache = TTLCache(default_ttl=cache_ttl)
        self.current_topic = None
        self.current_subscriber_id = None
        self._listener_task: Optional[asyncio.Task] = None
        
        

    # ------------------------------------------------------------------
    # Connection
    # ------------------------------------------------------------------

    async def connect(self,host,port) -> None:
        """Connect to the broker with automatic retries and exponential backoff."""
        if self.connected:
            return 

        last_exception = None
        for attempt in range(1, self.max_retries + 1):
            try:
                print(f"Connecting to {self.host}:{self.port} " f"(attempt {attempt}/{self.max_retries})")
                self.reader, self.writer = await asyncio.wait_for(asyncio.open_connection(host,port),timeout=self.timeout)
                await self._consume_server_banner()
                await self._send_command(Command.CONNECT, "{}")
                resp = await self._read_non_empty_response()
                if resp != Response.OK:
                    raise ProtocolError(f"Expected +OK, got: {resp}")
                self.connected = True
                self._listener_task = asyncio.create_task(self.listen())
                return
                
            
            except (OSError, asyncio.TimeoutError) as exc:
                last_exception = exc
                delay = self.backoff_base * (2 ** (attempt - 1))
                print(f"Connection attempt {attempt} failed: {exc}. "f"Retrying in {delay:.1f}s")
                await asyncio.sleep(delay)

        msg = f"Could not connect after {self.max_retries} attempts"
        print(msg)
        raise ConnectionError_(msg) from last_exception

    async def listen(self,topic=None,subscriber_id=None)-> None:
        while self.connected:
            line=await self.reader.readline()
            if not line:
                self.connected = False
                print("Connection closed by server")
                break
            
            resp=parse_response(line.decode("utf-8"))
            if resp==Response.SUBSCRIBED:
                print(f"Subscribed to '{self.current_topic}' "f"as subscriber {self.current_subscriber_id}")
                

            elif resp==Response.UNSUBBED:
                print(f"Unsubscribed subscriber {self.current_subscriber_id}")
                
            
            elif resp==Response.PUBLISHED:
                print(f"Published to '{self.current_topic}'")
            else:
                print(f"Received message: {resp}")
                
            
                

    
    async def disconnect(self) -> None:
        """Close the connection gracefully."""
        self.connected = False
        if self._listener_task:
            self._listener_task.cancel()
            try:
                await self._listener_task
            except asyncio.CancelledError:
                pass
            finally:
                self._listener_task = None

        if self.writer:
            try:
                self.writer.close()
                await self.writer.wait_closed()
                return
            except Exception as exc:
                msg="Failed to disconnect"
                raise DisconnectError(msg) from exc
            finally:
                self.writer = None
                self.reader = None
                print("Disconnected from JBroker")


    # ------------------------------------------------------------------
    # Subscribe / Unsubscribe
    # ------------------------------------------------------------------

    async def subscribe(self, topic: str, subscriber_id: int) -> None:
        """Subscribe to a topic with a unique subscriber id.

        Args:
            topic: Topic name (alphanumeric, dots, and * allowed).
            subscriber_id: Integer subscriber identifier.

        Returns:
            Server response string (e.g. "Subscribed").
        """
        self._require_connected()
        self.current_topic = topic
        self.current_subscriber_id = subscriber_id
        await self._send_command(
            Command.SUBSCRIBE, topic, str(subscriber_id)
        )
        

    async def unsubscribe(self, subscriber_id: int) -> None:
        """Unsubscribe a subscriber by id."""
        self._require_connected()
        self.current_subscriber_id = subscriber_id
        await self._send_command(Command.UNSUBSCRIBE, str(self.current_subscriber_id))

    # ------------------------------------------------------------------
    # Publish
    # ------------------------------------------------------------------

    async def publish(self, topic: str, message: str) -> None:
        """Publish a message to a topic.

        """
        self.current_topic = topic
        body = message.encode("utf-8")
        self._require_connected()
        self.writer.write(encode_message(Command.PUBLISH, topic, str(len(body))))
        self.writer.write(encode_publish_body(message))
        await self.writer.drain()
        
    # ------------------------------------------------------------------
    # Context manager
    # ------------------------------------------------------------------

    async def __aenter__(self) -> "PyClient":
        await self.connect(self.host,self.port)
        return self

    async def __aexit__(self, *exc_info) -> None:
        await self.disconnect()

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _require_connected(self) -> None:
        if not self.connected or self.writer is None:
            raise ConnectionError_("Not connected to JBroker. Call connect() first.")

    async def _send_command(self, *parts: str) -> None:
        """Send a command and return the stripped response line."""
        data = encode_message(*parts)
        self.writer.write(data)
        await self.writer.drain()

    async def _consume_server_banner(self) -> None:
        """Read the connection banner emitted before the protocol handshake."""
        while True:
            line = await self.reader.readline()
            if not line:
                raise ProtocolError("Connection closed before server banner")

            text = parse_response(line.decode("utf-8"))
            if text.startswith("Port :"):
                return

    async def _read_non_empty_response(self) -> str:
        """Read the next non-empty protocol response from the broker."""
        while True:
            line = await self.reader.readline()
            if not line:
                raise ProtocolError("Connection closed while waiting for response")

            response = parse_response(line.decode("utf-8"))
            if response:
                return response
    