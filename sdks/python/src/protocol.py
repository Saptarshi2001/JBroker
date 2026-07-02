"""Protocol constants and helpers for the JBroker text-based TCP protocol."""

CRLF = "\n"
ENCODING = "utf-8"


class Command:
    CONNECT = "Connect"
    SUBSCRIBE = "Sub"
    PUBLISH = "Pub"
    UNSUBSCRIBE = "Unsub"


class Response:
    OK = "+OK"
    SUBSCRIBED = "Subscribed"
    UNSUBBED = "Unsubbed"
    PUBLISHED = "Published"


def encode_message(*parts: str) -> bytes:
    msg = " ".join(parts) + CRLF
    return msg.encode(ENCODING)


def encode_publish_body(body: str) -> bytes:
    return (body + CRLF).encode(ENCODING)


def parse_response(line: str) -> str:
    return line.strip()
