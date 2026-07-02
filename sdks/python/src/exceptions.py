import json
import time
from typing import Optional


class JBrokerError(Exception):
    """Base exception for all JBroker client errors."""

    def __init__(self, message: str, error_type: Optional[str] = None):
        self.error_type = error_type or type(self).__name__
        self.message = message
        super().__init__(message)

    def to_json(self) -> str:
        return json.dumps({
            "error_type": self.error_type,
            "message": self.message,
            "timestamp": time.time(),
        })


class ConnectionError_(JBrokerError):
    """Raised when connection to the broker fails."""
    pass


class ProtocolError(JBrokerError):
    """Raised when the server responds with an unexpected message."""
    pass


class AuthenticationError(JBrokerError):
    """Raised when the connect command is rejected."""
    pass


class SubscriptionError(JBrokerError):
    """Raised when a subscribe operation fails."""
    pass


class PublishError(JBrokerError):
    """Raised when a publish operation fails."""
    pass

class DisconnectError(JBrokerError):
    """Raised when an error occurs during disconnect"""
    pass
