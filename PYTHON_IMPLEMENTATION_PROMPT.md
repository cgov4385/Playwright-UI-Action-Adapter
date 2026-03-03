# Detailed Python Conversion Implementation Prompt

## Executive Summary

This document provides a detailed, step-by-step implementation guide for converting the Playwright UI Action Adapter from Java to Python. Each section contains specific instructions, code templates, and implementation notes for developers to follow systematically.

---


## Phase 1: Project Initialization

### Step 1.1: Create Project Structure

```bash
# Create main directory structure
mkdir -p playwright_ui_adapter/{adapter,agent/service,driver,error,executor,model,selector,testcase,config}
mkdir -p tests/{unit,integration,fixtures}
mkdir -p scripts
mkdir -p docs
mkdir -p logs/action-logs
mkdir -p screenshots

# Create __init__.py files
touch playwright_ui_adapter/__init__.py
touch playwright_ui_adapter/adapter/__init__.py
touch playwright_ui_adapter/agent/__init__.py
touch playwright_ui_adapter/agent/service/__init__.py
touch playwright_ui_adapter/driver/__init__.py
touch playwright_ui_adapter/error/__init__.py
touch playwright_ui_adapter/executor/__init__.py
touch playwright_ui_adapter/model/__init__.py
touch playwright_ui_adapter/selector/__init__.py
touch playwright_ui_adapter/testcase/__init__.py
touch playwright_ui_adapter/config/__init__.py
touch tests/__init__.py
touch tests/unit/__init__.py
touch tests/integration/__init__.py
```

### Step 1.2: Create pyproject.toml

Create `pyproject.toml` in the project root:

```toml
[tool.poetry]
name = "playwright-ui-adapter"
version = "1.0.0"
description = "LLM-powered UI test automation with two-agent architecture"
authors = ["Your Name <your.email@example.com>"]
readme = "README.md"
packages = [{include = "playwright_ui_adapter"}]

[tool.poetry.dependencies]
python = "^3.9"
playwright = "^1.41.0"
openpyxl = "^3.1.2"
pandas = "^2.0.0"
google-cloud-aiplatform = "^1.40.0"
google-auth = "^2.27.0"
httpx = "^0.26.0"
pydantic = "^2.5.0"
pydantic-settings = "^2.1.0"
python-dotenv = "^1.0.0"

[tool.poetry.group.dev.dependencies]
pytest = "^7.4.0"
pytest-asyncio = "^0.21.0"
pytest-playwright = "^0.4.0"
pytest-cov = "^4.1.0"
mypy = "^1.8.0"
black = "^23.12.0"
ruff = "^0.1.0"
types-requests = "^2.31.0"

[tool.poetry.scripts]
ui-adapter = "scripts.main:main"
ui-adapter-validate = "scripts.validation_main:main"

[build-system]
requires = ["poetry-core"]
build-backend = "poetry.core.masonry.api"

[tool.black]
line-length = 120
target-version = ['py39', 'py310', 'py311']
exclude = '''
/(
    \.git
  | \.venv
  | build
  | dist
)/
'''

[tool.ruff]
line-length = 120
target-version = "py39"
select = ["E", "F", "I", "N", "W", "UP"]
ignore = ["E501"]

[tool.mypy]
python_version = "3.9"
warn_return_any = true
warn_unused_configs = true
disallow_untyped_defs = true
ignore_missing_imports = true

[tool.pytest.ini_options]
testpaths = ["tests"]
python_files = ["test_*.py"]
python_classes = ["Test*"]
python_functions = ["test_*"]
addopts = "-v --tb=short --cov=playwright_ui_adapter --cov-report=html --cov-report=term"
```

### Step 1.3: Create .gitignore

Create `.gitignore`:

```gitignore
# Python
__pycache__/
*.py[cod]
*$py.class
*.so
.Python
build/
develop-eggs/
dist/
downloads/
eggs/
.eggs/
lib/
lib64/
parts/
sdist/
var/
wheels/
*.egg-info/
.installed.cfg
*.egg

# Virtual environments
venv/
env/
ENV/
.venv

# IDEs
.vscode/
.idea/
*.swp
*.swo
*~

# Testing
.pytest_cache/
.coverage
htmlcov/
.tox/

# Mypy
.mypy_cache/
.dmypy.json
dmypy.json

# Logs
logs/
*.log
action-logs/
llm-usage.txt

# Screenshots
screenshots/
*.png
*.jpg

# Environment
.env
.env.local

# OS
.DS_Store
Thumbs.db

# Project specific
target/
```

### Step 1.4: Create .env.example

Create `config/.env.example`:

```bash
# LLM Provider Configuration
LLM_PROVIDER=gemini-vertex

# Vertex AI (Google Cloud) Settings
VERTEX_PROJECT=your-project-id
VERTEX_LOCATION=us-central1
GEMINI_MODEL=gemini-2.5-flash

# Groq Settings (if using Groq)
GROQ_API_KEY=your-groq-api-key-here
GROQ_MODEL=openai/gpt-oss-safeguard-20b

# Agent Settings
LLM_USAGE_LOG_ENABLED=true
LLM_USAGE_LOG_FILE=llm-usage.txt
LLM_MAX_EXECUTION_MESSAGES_TO_SEND=10
VALIDATION_WIRE_LOG_ENABLED=true

# Application Secrets (for test data)
MY_SSO_PASSWORD=your-password-here
MY_APP_PASSWORD=your-password-here

# Google Cloud Authentication
# GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json

# Browser Settings
PLAYWRIGHT_HEADLESS=false
PLAYWRIGHT_BROWSER=chromium

# Screenshot Settings
SCREENSHOT_DIR=screenshots
SCREENSHOT_ON_FAILURE=true

# Action Logging
ACTION_LOG_ENABLED=true
ACTION_LOG_DIR=logs/action-logs
```

---

## Phase 2: Model Layer Implementation

### Step 2.1: Implement error_types.py

Create `playwright_ui_adapter/error/error_types.py`:

```python
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


# Update __init__.py
# playwright_ui_adapter/error/__init__.py
from .error_types import ErrorType

__all__ = ["ErrorType"]
```

### Step 2.2: Implement selector.py

Create `playwright_ui_adapter/model/selector.py`:

```python
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


# playwright_ui_adapter/model/__init__.py
from .selector import Selector, SelectorType

__all__ = ["Selector", "SelectorType"]
```

### Step 2.3: Implement action.py

Create `playwright_ui_adapter/model/action.py`:

```python
"""Action model representing UI actions to be executed."""

from dataclasses import dataclass
from enum import Enum
from typing import Optional

from .selector import Selector


class ActionType(str, Enum):
    """UI action types."""
    
    NAVIGATE = "NAVIGATE"
    CLICK = "CLICK"
    TYPE = "TYPE"
    WAIT = "WAIT"
    WAIT_FOR_VISIBLE = "WAIT_FOR_VISIBLE"
    ASSERT_VISIBLE = "ASSERT_VISIBLE"
    ASSERT_TEXT = "ASSERT_TEXT"
    SCREENSHOT = "SCREENSHOT"
    SCROLL = "SCROLL"
    SWITCH_TAB = "SWITCH_TAB"
    MAXIMIZE_WINDOW = "MAXIMIZE_WINDOW"
    CLICK_CHECKBOX = "CLICK_CHECKBOX"
    CLOSE_TAB = "CLOSE_TAB"
    COMPLETE = "COMPLETE"  # Special type indicating test completion
    
    def __str__(self) -> str:
        return self.value


@dataclass
class Action:
    """
    Represents a UI action to be executed.
    
    Actions are generated by the ActionAgent based on test goals
    and executed by the ActionExecutor.
    
    Attributes:
        type: The type of action to perform
        selector: Optional selector for element location (not needed for NAVIGATE, WAIT)
        value: Optional value for the action (URL for NAVIGATE, text for TYPE, etc.)
    
    Examples:
        >>> # Navigate to URL
        >>> Action(type=ActionType.NAVIGATE, value="https://example.com")
        
        >>> # Click element
        >>> Action(type=ActionType.CLICK, selector=Selector(SelectorType.TEXT, "Submit"))
        
        >>> # Type text
        >>> Action(type=ActionType.TYPE, selector=Selector(SelectorType.CSS, "#username"), value="admin")
    """
    
    type: ActionType
    selector: Optional[Selector] = None
    value: Optional[str] = None
    
    def __str__(self) -> str:
        parts = [f"type={self.type}"]
        if self.selector:
            parts.append(f"selector={self.selector}")
        if self.value:
            # Mask if it looks like a secret
            display_value = "******" if self.value.startswith("ENV:") else self.value
            parts.append(f"value='{display_value}'")
        return f"Action({', '.join(parts)})"
    
    def __repr__(self) -> str:
        return self.__str__()


# Update __init__.py
from .action import Action, ActionType
from .selector import Selector, SelectorType

__all__ = ["Action", "ActionType", "Selector", "SelectorType"]
```

### Step 2.4: Implement action_result.py

Create `playwright_ui_adapter/model/action_result.py`:

```python
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
            error_type: Error category
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


# Update __init__.py
from .action import Action, ActionType
from .action_result import ActionResult, ActionStatus
from .selector import Selector, SelectorType

__all__ = [
    "Action",
    "ActionType",
    "ActionResult",
    "ActionStatus",
    "Selector",
    "SelectorType",
]
```

### Step 2.5: Implement test_step.py

Create `playwright_ui_adapter/model/test_step.py`:

```python
"""Test step model for individual steps within a test case."""

from dataclasses import dataclass
from typing import Optional


@dataclass
class TestStep:
    """
    Represents a single step within a test case.
    
    Test steps are loaded from Excel files and define the actions
    to perform and expected results for validation.
    
    Attributes:
        step_summary: The action to perform (e.g., "Login with valid credentials")
        test_data: Optional test data needed for the step (e.g., "username: admin, password: ENV:MY_PASSWORD")
        expected_result: Expected outcome for validation (e.g., "User is logged in successfully")
    """
    
    step_summary: str
    test_data: Optional[str] = None
    expected_result: Optional[str] = None
    
    def __str__(self) -> str:
        parts = [f"Action: {self.step_summary}"]
        if self.test_data:
            parts.append(f"Data: {self.test_data}")
        if self.expected_result:
            parts.append(f"Expected: {self.expected_result}")
        return ", ".join(parts)
    
    def __repr__(self) -> str:
        return f"TestStep({self.step_summary})"


# Update __init__.py
from .test_step import TestStep

__all__ = [..., "TestStep"]
```

### Step 2.6: Implement test_case.py

Create `playwright_ui_adapter/model/test_case.py`:

```python
"""Test case model representing a complete test with metadata and steps."""

from dataclasses import dataclass, field
from typing import List, Optional

from .test_step import TestStep


@dataclass
class TestCase:
    """
    Represents a complete test case with metadata and steps.
    
    Test cases are loaded from Excel files where each row with the same
    Issue Key is grouped into a single test case with multiple steps.
    
    Attributes:
        key: Unique test case identifier (Issue Key)
        summary: Test case summary/title
        preconditions: Prerequisites for the test
        test_steps: List of test steps to execute
        description: Optional detailed description
        status: Optional test status
        priority: Optional priority level
        test_case_type: Optional test type categorization
        functional_category: Optional functional area
        component_under_test: Optional component identifier
    """
    
    key: str
    summary: str
    preconditions: Optional[str] = None
    test_steps: List[TestStep] = field(default_factory=list)
    
    # Additional metadata
    description: Optional[str] = None
    status: Optional[str] = None
    priority: Optional[str] = None
    test_case_type: Optional[str] = None
    functional_category: Optional[str] = None
    component_under_test: Optional[str] = None
    
    def add_test_step(self, step: TestStep) -> None:
        """
        Add a test step to this test case.
        
        Args:
            step: TestStep to add
        """
        self.test_steps.append(step)
    
    def to_goal_string(self) -> str:
        """
        Convert the test case to a formatted goal string for the LLM.
        
        This is the prompt that will be sent to the Action Agent to
        understand the test objectives and generate actions.
        
        Returns:
            Formatted goal string with test case details
            
        Example:
            >>> test_case = TestCase(key="TC001", summary="Login Test", ...)
            >>> goal = test_case.to_goal_string()
            >>> # Returns formatted multi-line string with all test details
        """
        lines = []
        
        # Header
        lines.append(f"Test Case: {self.summary}")
        lines.append("")
        lines.append(f"Test Case Key: {self.key}")
        
        # Description
        if self.description:
            lines.append("")
            lines.append("Description:")
            lines.append(self.description)
        
        # Preconditions
        if self.preconditions:
            lines.append("")
            lines.append("Preconditions:")
            lines.append(self.preconditions)
        
        # Test Steps
        lines.append("")
        lines.append("Test Steps:")
        lines.append("")
        
        for idx, step in enumerate(self.test_steps, start=1):
            lines.append(f"Step {idx}:")
            lines.append(f"  Action: {step.step_summary}")
            
            if step.test_data:
                lines.append(f"  Test Data: {step.test_data}")
            
            if step.expected_result:
                lines.append(f"  Expected Result: {step.expected_result}")
            
            lines.append("")
        
        return "\n".join(lines)
    
    def __str__(self) -> str:
        return f"TestCase({self.key}: {self.summary}, {len(self.test_steps)} steps)"
    
    def __repr__(self) -> str:
        return self.__str__()


# Update __init__.py
from .test_case import TestCase
from .test_step import TestStep

__all__ = [..., "TestCase", "TestStep"]
```

### Step 2.7: Implement validation_result.py

Create `playwright_ui_adapter/model/validation_result.py`:

```python
"""Validation result model for test result validation."""

from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Optional


class ValidationStatus(str, Enum):
    """Validation result status."""
    
    PASS = "PASS"
    FAIL = "FAIL"
    UNKNOWN = "UNKNOWN"
    
    def __str__(self) -> str:
        return self.value


@dataclass
class ValidationResult:
    """
    Result of validating actual vs expected results.
    
    The Validation Agent uses LLM to fuzzy-match expected results
    against actual outcomes and provides confidence scoring.
    
    Attributes:
        status: Validation status (PASS/FAIL/UNKNOWN)
        reasoning: Detailed explanation of the validation decision
        confidence: Confidence score (0.0 - 1.0)
        expected_result: The expected result that was validated against
        actual_result: The actual result that was observed
        timestamp: When the validation was performed
    """
    
    status: ValidationStatus
    reasoning: str
    confidence: float
    expected_result: str
    actual_result: str
    timestamp: datetime = field(default_factory=datetime.now)
    
    @classmethod
    def success(
        cls,
        reasoning: str,
        confidence: float,
        expected_result: str,
        actual_result: str
    ) -> "ValidationResult":
        """Factory method for successful validation."""
        return cls(
            status=ValidationStatus.PASS,
            reasoning=reasoning,
            confidence=confidence,
            expected_result=expected_result,
            actual_result=actual_result
        )
    
    @classmethod
    def failure(
        cls,
        reasoning: str,
        confidence: float,
        expected_result: str,
        actual_result: str
    ) -> "ValidationResult":
        """Factory method for failed validation."""
        return cls(
            status=ValidationStatus.FAIL,
            reasoning=reasoning,
            confidence=confidence,
            expected_result=expected_result,
            actual_result=actual_result
        )
    
    @classmethod
    def unknown(
        cls,
        reasoning: str,
        expected_result: str,
        actual_result: str
    ) -> "ValidationResult":
        """Factory method for unknown validation."""
        return cls(
            status=ValidationStatus.UNKNOWN,
            reasoning=reasoning,
            confidence=0.0,
            expected_result=expected_result,
            actual_result=actual_result
        )
    
    def to_detailed_report(self) -> str:
        """
        Generate detailed validation report.
        
        Returns:
            Formatted multi-line report string
        """
        lines = []
        lines.append("=" * 80)
        lines.append("VALIDATION RESULT")
        lines.append("=" * 80)
        lines.append(f"Status: {self.status}")
        lines.append(f"Confidence: {self.confidence * 100:.1f}%")
        lines.append("")
        lines.append("Expected:")
        lines.append(f"  {self.expected_result}")
        lines.append("")
        lines.append("Actual:")
        lines.append(f"  {self.actual_result[:200]}...")  # Truncate long results
        lines.append("")
        lines.append("Reasoning:")
        lines.append(f"  {self.reasoning}")
        lines.append("=" * 80)
        return "\n".join(lines)
    
    def __str__(self) -> str:
        return f"{self.status} (confidence: {self.confidence:.0%}): {self.reasoning}"
    
    def __repr__(self) -> str:
        return f"ValidationResult({self.status}, confidence={self.confidence:.2f})"


# Update __init__.py
from .validation_result import ValidationResult, ValidationStatus

__all__ = [..., "ValidationResult", "ValidationStatus"]
```

---

## Phase 3: Driver Layer Implementation

### Step 3.1: Implement browser_driver.py

Create `playwright_ui_adapter/driver/browser_driver.py`:

```python
"""Abstract base class for browser drivers."""

from abc import ABC, abstractmethod
from typing import List

from playwright.sync_api import Page


class BrowserDriver(ABC):
    """
    Abstract base class for browser drivers.
    
    Defines the contract for browser automation drivers.
    Implementations should handle browser lifecycle, page management,
    and basic browser operations.
    """
    
    @abstractmethod
    def start(self) -> None:
        """
        Initialize and start the browser.
        
        This should launch the browser and create initial context/page.
        """
        pass
    
    @abstractmethod
    def get_page(self) -> Page:
        """
        Get the current active page instance.
        
        Returns:
            Current Playwright Page instance
            
        Raises:
            RuntimeError: If driver not started
        """
        pass
    
    @abstractmethod
    def get_all_pages(self) -> List[Page]:
        """
        Get all open pages/tabs.
        
        Returns:
            List of all Page instances in the browser context
            
        Raises:
            RuntimeError: If driver not started
        """
        pass
    
    @abstractmethod
    def switch_to_page(self, index: int) -> None:
        """
        Switch to a specific page by index.
        
        Args:
            index: Zero-based page index
            
        Raises:
            RuntimeError: If driver not started
            ValueError: If index is out of range
        """
        pass
    
    @abstractmethod
    def take_screenshot(self) -> bytes:
        """
        Capture screenshot of current page.
        
        Returns:
            Screenshot bytes (PNG format)
        """
        pass
    
    @abstractmethod
    def close(self) -> None:
        """
        Close the browser and cleanup resources.
        
        This should close all pages, contexts, and the browser itself.
        """
        pass


# playwright_ui_adapter/driver/__init__.py
from .browser_driver import BrowserDriver

__all__ = ["BrowserDriver"]
```

### Step 3.2: Implement playwright_driver.py

Create `playwright_ui_adapter/driver/playwright_driver.py`:

```python
"""Playwright implementation of BrowserDriver."""

from typing import List, Optional

from playwright.sync_api import (
    Browser,
    BrowserContext,
    Page,
    Playwright,
    sync_playwright,
)

from .browser_driver import BrowserDriver


class PlaywrightDriver(BrowserDriver):
    """
    Playwright implementation of BrowserDriver.
    
    Manages Chromium browser lifecycle using Playwright.
    Supports headless/headed modes and multiple tabs.
    
    Example:
        >>> driver = PlaywrightDriver(headless=False)
        >>> driver.start()
        >>> page = driver.get_page()
        >>> page.goto("https://example.com")
        >>> driver.close()
        
        >>> # Or use as context manager
        >>> with PlaywrightDriver() as driver:
        ...     page = driver.get_page()
        ...     page.goto("https://example.com")
    """
    
    def __init__(self, headless: bool = False):
        """
        Initialize Playwright driver.
        
        Args:
            headless: Whether to run browser in headless mode
        """
        self.headless = headless
        self._playwright: Optional[Playwright] = None
        self._browser: Optional[Browser] = None
        self._context: Optional[BrowserContext] = None
        self._page: Optional[Page] = None
    
    def start(self) -> None:
        """Start Playwright and launch browser."""
        self._playwright = sync_playwright().start()
        self._browser = self._playwright.chromium.launch(headless=self.headless)
        self._context = self._browser.new_context()
        self._page = self._context.new_page()
    
    def get_page(self) -> Page:
        """
        Get current page instance.
        
        Returns:
            Current Playwright Page
            
        Raises:
            RuntimeError: If driver not started
        """
        if self._page is None:
            raise RuntimeError("Driver not started. Call start() first.")
        return self._page
    
    def get_all_pages(self) -> List[Page]:
        """
        Get all open pages in the context.
        
        Returns:
            List of all Page instances
            
        Raises:
            RuntimeError: If driver not started
        """
        if self._context is None:
            raise RuntimeError("Driver not started. Call start() first.")
        return self._context.pages
    
    def switch_to_page(self, index: int) -> None:
        """
        Switch to page at given index.
        
        Args:
            index: Zero-based page index
            
        Raises:
            RuntimeError: If driver not started
            ValueError: If index out of range
        """
        if self._context is None:
            raise RuntimeError("Driver not started. Call start() first.")
        
        pages = self._context.pages
        if not (0 <= index < len(pages)):
            raise ValueError(
                f"Invalid page index: {index}. Total pages: {len(pages)}"
            )
        
        self._page = pages[index]
        self._page.bring_to_front()
    
    def take_screenshot(self) -> bytes:
        """
        Capture full-page screenshot.
        
        Returns:
            Screenshot bytes in PNG format
        """
        if self._page is None:
            return b""
        return self._page.screenshot(full_page=True)
    
    def close(self) -> None:
        """Close browser and cleanup resources."""
        if self._context:
            self._context.close()
        if self._browser:
            self._browser.close()
        if self._playwright:
            self._playwright.stop()
    
    def __enter__(self) -> "PlaywrightDriver":
        """Context manager entry."""
        self.start()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        """Context manager exit."""
        self.close()


# Update __init__.py
from .browser_driver import BrowserDriver
from .playwright_driver import PlaywrightDriver

__all__ = ["BrowserDriver", "PlaywrightDriver"]
```

---

## Phase 4: Configuration Layer

### Step 4.1: Implement settings.py

Create `playwright_ui_adapter/config/settings.py`:

```python
"""Application configuration management using Pydantic."""

import os
from typing import Optional

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """
    Application settings loaded from environment variables.
    
    Supports loading from .env file using python-dotenv.
    All settings can be overridden via environment variables.
    
    Example:
        >>> from playwright_ui_adapter.config import settings
        >>> print(settings.llm_provider)
        'gemini-vertex'
    """
    
    # LLM Configuration
    llm_provider: str = "gemini-vertex"
    
    # Vertex AI Settings
    vertex_project: Optional[str] = None
    google_cloud_project: Optional[str] = None  # Alias for vertex_project
    vertex_location: str = "us-central1"
    google_cloud_location: Optional[str] = None  # Alias for vertex_location
    gemini_model: str = "gemini-2.5-flash"
    
    # Groq Settings
    groq_api_key: Optional[str] = None
    groq_model: str = "openai/gpt-oss-safeguard-20b"
    
    # Agent Settings
    llm_usage_log_enabled: bool = True
    llm_usage_log_file: str = "llm-usage.txt"
    llm_max_execution_messages_to_send: int = 10
    validation_wire_log_enabled: bool = True
    
    # Application Secrets
    my_sso_password: Optional[str] = None
    my_app_password: Optional[str] = None
    
    # Google Cloud Authentication
    google_application_credentials: Optional[str] = None
    
    # Browser Settings
    playwright_headless: bool = False
    playwright_browser: str = "chromium"
    
    # Screenshot Settings
    screenshot_dir: str = "screenshots"
    screenshot_on_failure: bool = True
    
    # Action Logging
    action_log_enabled: bool = True
    action_log_dir: str = "logs/action-logs"
    
    @property
    def effective_vertex_project(self) -> Optional[str]:
        """Get effective Vertex project (supports multiple env var names)."""
        return self.vertex_project or self.google_cloud_project
    
    @property
    def effective_vertex_location(self) -> str:
        """Get effective Vertex location (supports multiple env var names)."""
        return self.google_cloud_location or self.vertex_location
    
    class Config:
        """Pydantic configuration."""
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False
        extra = "allow"  # Allow extra fields from environment


# Global settings instance
settings = Settings()


# playwright_ui_adapter/config/__init__.py
from .settings import settings, Settings

__all__ = ["settings", "Settings"]
```

---

## Implementation Notes

### Critical Implementation Details

1. **Type Hints**: Use type hints everywhere for better IDE support and type checking
2. **Dataclasses vs Pydantic**: 
   - Use `@dataclass` for simple data structures without validation
   - Use Pydantic `BaseModel` when you need validation or serialization
3. **Enums**: Always use `str, Enum` to make enums JSON-serializable
4. **Optional Fields**: Use `Optional[Type]` for nullable fields
5. **Factory Methods**: Use `@classmethod` for alternative constructors
6. **Context Managers**: Implement `__enter__` and `__exit__` for resource management
7. **Documentation**: Write comprehensive docstrings with examples
8. **Error Handling**: Create custom exception hierarchy
9. **Logging**: Use proper logging framework (not print statements)
10. **Testing**: Write tests alongside implementation (TDD approach)

### Next Steps

Continue with:
1. Selector Layer (selector_resolver.py)
2. Executor Layer (action_executor.py, action_logger.py)
3. LLM Service Layer (llm_client.py, implementations)
4. Agent Layer (action_agent.py, validation_agent.py)
5. Orchestrator (two_agent_orchestrator.py)
6. Test Case Loader (excel_test_case_loader.py)
7. Entry Points (main.py, validation_main.py)
8. Testing Suite

### Testing Approach

For each module:
1. Write unit tests first (TDD)
2. Test happy path
3. Test error cases
4. Test edge cases
5. Mock external dependencies (LLM, browser)
6. Aim for >80% code coverage

### Code Quality Checks

After implementation:
```bash
# Format code
black playwright_ui_adapter/

# Lint code
ruff check playwright_ui_adapter/

# Type check
mypy playwright_ui_adapter/

# Run tests
pytest --cov=playwright_ui_adapter --cov-report=html

# Generate documentation
pdoc playwright_ui_adapter/ -o docs/
```

---

## Conclusion

This implementation prompt provides detailed step-by-step instructions for converting the Java project to Python. Follow the phases in order, implementing and testing each module before moving to the next. The provided code templates are production-ready and follow Python best practices.

**Estimated Time per Phase:**
- Phase 1 (Setup): 2-4 hours
- Phase 2 (Models): 4-6 hours
- Phase 3 (Driver): 2-3 hours
- Phase 4 (Config): 1-2 hours
- Remaining phases: 30-40 hours

**Total Estimated Effort**: 40-60 hours for complete conversion with comprehensive testing.
