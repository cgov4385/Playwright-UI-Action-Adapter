"""Error type enumeration for categorizing failures."""

from enum import Enum


class ErrorType(str, Enum):
    """Categorized error types for action execution failures."""
    
    # Navigation errors
    NAVIGATION_FAILED = "NAVIGATION_FAILED"
    PAGE_LOAD_TIMEOUT = "PAGE_LOAD_TIMEOUT"
    
    # Element errors
    ELEMENT_NOT_FOUND = "ELEMENT_NOT_FOUND"
    ELEMENT_NOT_VISIBLE = "ELEMENT_NOT_VISIBLE"
    ELEMENT_NOT_INTERACTABLE = "ELEMENT_NOT_INTERACTABLE"
    AMBIGUOUS_SELECTOR = "AMBIGUOUS_SELECTOR"
    
    # Selector errors
    SELECTOR_ERROR = "SELECTOR_ERROR"
    INVALID_SELECTOR = "INVALID_SELECTOR"
    
    # Action errors
    INVALID_ACTION = "INVALID_ACTION"
    EXECUTION_ERROR = "EXECUTION_ERROR"
    TIMEOUT_ERROR = "TIMEOUT_ERROR"
    
    # Assertion errors
    ASSERTION_FAILED = "ASSERTION_FAILED"
    
    # Unknown errors
    UNKNOWN_ERROR = "UNKNOWN_ERROR"
    
    def __str__(self) -> str:
        return self.value
