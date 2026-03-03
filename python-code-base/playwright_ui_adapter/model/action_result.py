"""Action result model representing the outcome of action execution."""

from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Optional

from ..error.error_types import ErrorType


class ActionStatus(str, Enum):
    """Action execution status."""
    
    PASS = "PASS"
    FAIL = "FAIL"
    
    def __str__(self) -> str:
        return self.value


@dataclass
class ActionResult:
    """
    Result of executing a UI action.
    
    Contains status, message, error details, and optionally captured
    page state for validation and debugging.
    
    Attributes:
        status: Execution status (PASS/FAIL)
        message: Human-readable result message
        error_type: Optional error categorization for failures
        screenshot_path: Optional path to failure screenshot
        page_state: Optional captured page content for validation
        timestamp: When the result was created
    """
    
    status: ActionStatus
    message: str
    error_type: Optional[ErrorType] = None
    screenshot_path: Optional[str] = None
    page_state: Optional[str] = None
    timestamp: datetime = field(default_factory=datetime.now)
    
    @classmethod
    def success(cls, message: str, page_state: Optional[str] = None) -> "ActionResult":
        """
        Factory method for successful result.
        
        Args:
            message: Success message
            page_state: Optional captured page state
            
        Returns:
            ActionResult with PASS status
        """
        return cls(
            status=ActionStatus.PASS,
            message=message,
            page_state=page_state
        )
    
    @classmethod
    def failure(
        cls,
        message: str,
        error_type: ErrorType,
        screenshot_path: Optional[str] = None,
        page_state: Optional[str] = None
    ) -> "ActionResult":
        """
        Factory method for failed result.
        
        Args:
            message: Failure message
            error_type: Error categorization
            screenshot_path: Optional path to failure screenshot
            page_state: Optional captured page state
            
        Returns:
            ActionResult with FAIL status
        """
        return cls(
            status=ActionStatus.FAIL,
            message=message,
            error_type=error_type,
            screenshot_path=screenshot_path,
            page_state=page_state
        )
    
    def __str__(self) -> str:
        parts = [f"{self.status}: {self.message}"]
        if self.error_type:
            parts.append(f"[{self.error_type}]")
        if self.screenshot_path:
            parts.append(f"screenshot={self.screenshot_path}")
        return " ".join(parts)
    
    def __repr__(self) -> str:
        return f"ActionResult({self.status}, {self.message})"
