"""Playwright UI Action Adapter - LLM-powered UI test automation."""

__version__ = "1.0.0"

from .model import (
    Action,
    ActionType,
    ActionResult,
    ActionStatus,
    Selector,
    SelectorType,
    TestCase,
    TestStep,
    ValidationResult,
    ValidationStatus,
    TestRunResult,
)
from .error import ErrorType
from .driver import BrowserDriver, PlaywrightDriver
from .config import settings

__all__ = [
    "__version__",
    "Action",
    "ActionType",
    "ActionResult",
    "ActionStatus",
    "Selector",
    "SelectorType",
    "TestCase",
    "TestStep",
    "ValidationResult",
    "ValidationStatus",
    "TestRunResult",
    "ErrorType",
    "BrowserDriver",
    "PlaywrightDriver",
    "settings",
]
