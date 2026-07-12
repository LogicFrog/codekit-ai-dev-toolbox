import hashlib
import secrets
from datetime import datetime, timedelta
from typing import Optional

class AuthTokenService:
    """Handle token generation and validation for API authentication."""

    TOKEN_BYTES = 32
    DEFAULT_EXPIRE_HOURS = 2

    def __init__(self):
        self._active_tokens: dict[str, dict] = {}

    def generate_token(self, user_id: str) -> str:
        raw = secrets.token_urlsafe(self.TOKEN_BYTES)
        hashed = hashlib.sha256(raw.encode()).hexdigest()
        self._active_tokens[hashed] = {
            "user_id": user_id,
            "created_at": datetime.now(),
            "expires_at": datetime.now() + timedelta(hours=self.DEFAULT_EXPIRE_HOURS),
        }
        return raw

    def validate_token(self, token: str) -> Optional[str]:
        hashed = hashlib.sha256(token.encode()).hexdigest()
        entry = self._active_tokens.get(hashed)
        if entry is None:
            return None
        if datetime.now() > entry["expires_at"]:
            del self._active_tokens[hashed]
            return None
        return entry["user_id"]

    def revoke_token(self, token: str) -> None:
        hashed = hashlib.sha256(token.encode()).hexdigest()
        self._active_tokens.pop(hashed, None)
