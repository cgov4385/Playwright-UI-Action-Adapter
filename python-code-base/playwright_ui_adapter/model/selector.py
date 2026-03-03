"""Selector model for UI element location strategies."""

from dataclasses import dataclass
from enum import Enum
from typing import Optional


class SelectorType(str, Enum):
    """Selector strategy types."""
    
    CSS = "CSS"
    XPATH = "XPATH"
    TEXT = "TEXT"
    ROLE = "ROLE"
    LABEL = "LABEL"
    PLACEHOLDER = "PLACEHOLDER"
    TEST_ID = "TEST_ID"
    
    def __str__(self) -> str:
        return self.value


@dataclass
class Selector:
    """
    Represents a selector for locating UI elements.
    
    Supports multiple selector strategies with optional metadata
    for disambiguation and semantic understanding.
    
    Attributes:
        type: The selector strategy type
        value: The selector value (e.g., CSS selector, text content)
        role: Optional ARIA role for ROLE strategy
        near_text: Optional nearby text for disambiguation
        description: Optional LLM-provided description
    """
    
    type: SelectorType
    value: str
    role: Optional[str] = None
    near_text: Optional[str] = None
    description: Optional[str] = None
    
    def __str__(self) -> str:
        parts = [f"{self.type}={self.value}"]
        if self.role:
            parts.append(f"role={self.role}")
        if self.near_text:
            parts.append(f"near={self.near_text}")
        if self.description:
            parts.append(f"desc={self.description[:30]}...")
        return f"Selector({', '.join(parts)})"
    
    def __repr__(self) -> str:
        return self.__str__()
