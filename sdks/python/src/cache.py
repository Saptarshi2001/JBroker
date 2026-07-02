import time
from typing import Any, Dict, Optional
from collections import OrderedDict


class TTLCache:
    """Simple TTL-based cache for published message deduplication."""

    def __init__(self, default_ttl: float = 300.0):
        self.default_ttl = default_ttl
        self.store: Dict[str, tuple[Any, float]] = OrderedDict()

    def get(self, key: str) -> Optional[Any]:
        if key not in self.store:
            return None
        value, expiry = self.store[key]
        if time.monotonic() > expiry:
            self.remove(key)
            return None
        return value

    def set(self, key: str, value: Any, ttl: Optional[float] = None) -> None:
        expiry = time.monotonic() + (ttl if ttl is not None else self.default_ttl)
        self.store[key] = (value, expiry)

    def remove(self, key: str):
        self.store.pop(key)

    def clear(self) -> None:
        self.store.clear()

    def __contains__(self, key: str) -> bool:
        return self.get(key) is not None
