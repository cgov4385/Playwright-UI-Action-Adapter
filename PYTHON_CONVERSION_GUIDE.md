# Python Conversion Guide: Playwright UI Action Adapter

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture Analysis](#architecture-analysis)
3. [Technology Stack Mapping](#technology-stack-mapping)
4. [Project Structure Conversion](#project-structure-conversion)
5. [Dependency Mapping](#dependency-mapping)
6. [Module-by-Module Conversion Guide](#module-by-module-conversion-guide)
7. [Configuration & Environment](#configuration--environment)
8. [Testing Strategy](#testing-strategy)
9. [Migration Checklist](#migration-checklist)
10. [Python Best Practices](#python-best-practices)

---

## Project Overview

### Current State (Java)
The Playwright UI Action Adapter is a sophisticated LLM-powered test automation framework that uses a **two-agent architecture** to intelligently execute and validate UI tests.

**Core Capabilities:**
- LLM-driven test execution using natural language goals
- Two-agent system: Action Agent + Validation Agent
- Excel-based test case management
- Multiple LLM provider support (Google Gemini Vertex AI, Groq)
- Comprehensive action logging and screenshot capture
- Playwright browser automation
- Intelligent selector resolution with multiple strategies

**Key Statistics:**
- **Total Java Classes:** ~25-30 files
- **Lines of Code:** ~5000-6000 LOC
- **Main Entry Points:** Main.java, ValidationMain.java
- **Test Case Format:** Excel (.xlsx) with multi-step test cases
- **Build Tool:** Maven (pom.xml)
- **Java Version:** Java 11

---

## Architecture Analysis

### Two-Agent Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  Test Execution Flow                         │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
        ┌──────────────────────────────────────┐
        │   Excel Test Case Loader             │
        │   - Parses .xlsx files               │
        │   - Groups steps by Issue Key        │
        │   - Creates TestCase objects         │
        └──────────────────────────────────────┘
                           │
                           ▼
        ┌──────────────────────────────────────┐
        │   TwoAgentOrchestrator               │
        │   - Coordinates both agents          │
        │   - Manages execution flow           │
        └──────────────────────────────────────┘
                           │
            ┌──────────────┴──────────────┐
            ▼                             ▼
    ┌───────────────┐           ┌───────────────┐
    │ ActionAgent   │           │ Validation    │
    │               │           │    Agent      │
    │ - Interprets  │           │ - Validates   │
    │   test goals  │           │   results     │
    │ - Generates   │           │ - Fuzzy       │
    │   UI actions  │           │   matching    │
    │ - LLM-powered │           │ - Confidence  │
    └───────────────┘           └───────────────┘
            │
            ▼
    ┌───────────────────────┐
    │ UIActionAdapter       │
    │ - Executes actions    │
    │ - Captures state      │
    └───────────────────────┘
            │
            ▼
    ┌───────────────────────┐
    │ ActionExecutor        │
    │ - Browser control     │
    │ - Screenshot capture  │
    │ - Secret resolution   │
    └───────────────────────┘
            │
            ▼
    ┌───────────────────────┐
    │ PlaywrightDriver      │
    │ - Chromium launch     │
    │ - Page management     │
    └───────────────────────┘
```

### Component Breakdown

#### 1. **Model Layer** (Data Structures)
- `Action` - UI action representation (CLICK, TYPE, NAVIGATE, etc.)
- `ActionResult` - Execution result with status, message, screenshot
- `TestCase` - Test case container with metadata and steps
- `TestStep` - Individual test step with expected result
- `Selector` - Multi-strategy selector (CSS, XPATH, TEXT, ROLE, etc.)
- `ValidationResult` - Validation outcome with confidence scoring

#### 2. **Agent Layer** (LLM Intelligence)
- `ActionAgent` (formerly LlmAgent) - Generates UI actions from goals
- `ValidationAgent` - Validates actual vs expected results
- `TestAgent` (interface) - Common agent contract

#### 3. **LLM Service Layer** (Provider Abstraction)
- `LlmClient` (interface) - Provider abstraction
- `GeminiVertexLlmClient` - Google Cloud Vertex AI integration
- `GroqLlmClient` - Groq API integration
- `LlmUsageLogger` - Token usage tracking
- `TokenCounter` - Token counting utility

#### 4. **Execution Layer** (Browser Automation)
- `TwoAgentOrchestrator` - Coordinates two-agent execution
- `ActionExecutor` - Executes individual actions
- `UIActionAdapter` - High-level action execution interface
- `ActionLogger` - Comprehensive action logging

#### 5. **Driver Layer** (Browser Control)
- `BrowserDriver` (interface) - Driver abstraction
- `PlaywrightDriver` - Playwright implementation

#### 6. **Selector Layer** (Element Location)
- `SelectorResolver` - Resolves selectors to Playwright locators

#### 7. **Test Case Loader Layer** (Test Data)
- `TestCaseLoader` (interface) - Loader abstraction
- `ExcelTestCaseLoader` - Excel file parsing with Apache POI

#### 8. **Error Handling Layer**
- `ErrorType` (enum) - Categorized error types

---

## Technology Stack Mapping

### Java → Python Equivalents

| Java Technology | Python Equivalent | Notes |
|----------------|-------------------|-------|
| **Maven** | `pip` + `poetry` / `requirements.txt` | Poetry recommended for dependency management |
| **Java 11** | Python 3.9+ | Prefer Python 3.11 or 3.12 for performance |
| **Playwright Java** | `playwright` (Python) | Official Playwright Python library |
| **Apache POI** | `openpyxl` or `pandas` | openpyxl for direct Excel, pandas for data manipulation |
| **Gson** | `json` (built-in) or `pydantic` | Pydantic recommended for data validation |
| **java.net.http.HttpClient** | `httpx` or `requests` | httpx recommended (async support) |
| **Google Cloud Vertex AI SDK** | `google-cloud-aiplatform` | Official Google Cloud Python library |
| **Google Auth** | `google-auth` | Official Google Auth library |
| **Properties files** | `python-dotenv` or `configparser` | python-dotenv recommended for .env files |
| **Maven properties** | Environment variables + .env | |
| **JUnit (implicit)** | `pytest` | Standard Python testing framework |

---

## Project Structure Conversion

### Java Structure
```
src/
  main/
    java/
      ui_adapter/
        adapter/
        agent/
          service/
        driver/
        error/
        executor/
        model/
        selector/
        testcase/
    resources/
      application.properties
target/
pom.xml
```

### Proposed Python Structure
```
playwright_ui_adapter/              # Main package
  __init__.py
  
  adapter/
    __init__.py
    ui_action_adapter.py           # UIActionAdapter
  
  agent/
    __init__.py
    action_agent.py                 # ActionAgent
    validation_agent.py             # ValidationAgent
    test_agent.py                   # TestAgent (abstract base class)
    
    service/
      __init__.py
      llm_client.py                 # LlmClient (abstract)
      gemini_vertex_client.py       # GeminiVertexLlmClient
      groq_client.py                # GroqLlmClient
      llm_usage_logger.py           # LlmUsageLogger
      token_counter.py              # TokenCounter
  
  driver/
    __init__.py
    browser_driver.py               # BrowserDriver (abstract)
    playwright_driver.py            # PlaywrightDriver
  
  error/
    __init__.py
    error_types.py                  # ErrorType enum
  
  executor/
    __init__.py
    action_executor.py              # ActionExecutor
    action_logger.py                # ActionLogger
    two_agent_orchestrator.py      # TwoAgentOrchestrator
  
  model/
    __init__.py
    action.py                       # Action class
    action_result.py                # ActionResult
    test_case.py                    # TestCase
    test_step.py                    # TestStep
    selector.py                     # Selector
    validation_result.py            # ValidationResult
    test_run_result.py              # TestRunResult
  
  selector/
    __init__.py
    selector_resolver.py            # SelectorResolver
  
  testcase/
    __init__.py
    test_case_loader.py             # TestCaseLoader (abstract)
    excel_test_case_loader.py       # ExcelTestCaseLoader
  
  config/
    __init__.py
    settings.py                     # Centralized configuration

tests/                              # Test directory
  __init__.py
  test_action_executor.py
  test_agents.py
  test_excel_loader.py
  fixtures/
    sample_test_cases.xlsx

scripts/                            # Entry point scripts
  main.py                           # Main entry point
  validation_main.py                # Validation mode entry point
  excel_test_example.py             # Example script

config/
  .env.example                      # Example environment variables
  application.properties            # Optional: keep for compatibility

logs/
  action-logs/                      # Action logs directory

screenshots/                        # Screenshot directory

docs/                              # Documentation
  ARCHITECTURE.md
  TWO_AGENT_ORCHESTRATOR.md
  ACTION_LOGGING.md
  (etc.)

pyproject.toml                      # Poetry configuration
requirements.txt                    # Pip requirements (generated from pyproject.toml)
setup.py                           # Package setup (optional)
README.md                          # Updated README
.env                               # Environment variables (gitignored)
.gitignore                         # Git ignore file
pytest.ini                         # Pytest configuration
```

---

## Dependency Mapping

### Maven Dependencies → Python Packages

#### pom.xml to pyproject.toml/requirements.txt

**Current Maven Dependencies:**
```xml
<dependencies>
    <!-- Playwright -->
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>1.41.1</version>
    </dependency>
    
    <!-- JSON -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>
    
    <!-- Vertex AI Gemini -->
    <dependency>
        <groupId>com.google.cloud</groupId>
        <artifactId>google-cloud-vertexai</artifactId>
        <version>1.18.0</version>
    </dependency>
    
    <!-- Google Auth -->
    <dependency>
        <groupId>com.google.auth</groupId>
        <artifactId>google-auth-library-oauth2-http</artifactId>
        <version>1.23.0</version>
    </dependency>
    
    <!-- Apache POI -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi</artifactId>
        <version>5.2.5</version>
    </dependency>
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.5</version>
    </dependency>
</dependencies>
```

**Python requirements.txt:**
```
# Core Dependencies
playwright>=1.41.0
openpyxl>=3.1.2              # Excel file handling
pandas>=2.0.0                # Optional: for advanced Excel operations

# LLM Providers
google-cloud-aiplatform>=1.40.0    # Vertex AI / Gemini
google-auth>=2.27.0                # Google Cloud authentication
httpx>=0.26.0                      # HTTP client for Groq API
requests>=2.31.0                   # Fallback HTTP client

# Data Validation & Serialization
pydantic>=2.5.0              # Data validation and settings management
pydantic-settings>=2.1.0     # Settings from environment variables

# Configuration
python-dotenv>=1.0.0         # Environment variable loading

# Testing
pytest>=7.4.0
pytest-asyncio>=0.21.0       # Async test support
pytest-playwright>=0.4.0     # Playwright fixtures for pytest

# Logging & Utilities
loguru>=0.7.0                # Better logging (optional)
rich>=13.7.0                 # Better console output (optional)

# Type Checking (Development)
mypy>=1.8.0
types-requests>=2.31.0
```

**pyproject.toml (Poetry - Recommended):**
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
loguru = {version = "^0.7.0", optional = true}
rich = {version = "^13.7.0", optional = true}

[tool.poetry.group.dev.dependencies]
pytest = "^7.4.0"
pytest-asyncio = "^0.21.0"
pytest-playwright = "^0.4.0"
mypy = "^1.8.0"
black = "^23.12.0"
ruff = "^0.1.0"
types-requests = "^2.31.0"

[tool.poetry.extras]
enhanced = ["loguru", "rich"]

[tool.poetry.scripts]
ui-adapter = "scripts.main:main"
ui-adapter-validate = "scripts.validation_main:main"

[build-system]
requires = ["poetry-core"]
build-backend = "poetry.core.masonry.api"

[tool.black]
line-length = 120
target-version = ['py39', 'py310', 'py311']

[tool.ruff]
line-length = 120
target-version = "py39"

[tool.mypy]
python_version = "3.9"
warn_return_any = true
warn_unused_configs = true
disallow_untyped_defs = true

[tool.pytest.ini_options]
testpaths = ["tests"]
python_files = ["test_*.py"]
python_classes = ["Test*"]
python_functions = ["test_*"]
addopts = "-v --tb=short"
```

---

## Module-by-Module Conversion Guide

### 1. Model Layer

#### 1.1 Action.java → action.py

**Java:**
```java
public class Action {
    public enum Type {
        NAVIGATE, CLICK, TYPE, WAIT, WAIT_FOR_VISIBLE,
        ASSERT_VISIBLE, ASSERT_TEXT, SCREENSHOT, SCROLL,
        SWITCH_TAB, MAXIMIZE_WINDOW, CLICK_CHECKBOX, CLOSE_TAB
    }
    private final Type type;
    private final Selector selector;
    private final String value;
}
```

**Python:**
```python
from enum import Enum
from dataclasses import dataclass
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

@dataclass
class Action:
    """Represents a UI action to be executed."""
    type: ActionType
    selector: Optional[Selector] = None
    value: Optional[str] = None
    
    def __str__(self) -> str:
        return f"Action(type={self.type}, selector={self.selector}, value='{self.value}')"
```

**Key Changes:**
- Use `enum.Enum` for ActionType
- Use `@dataclass` for simple data classes (or Pydantic BaseModel)
- Use `Optional[Type]` for nullable fields
- Type hints for all fields

#### 1.2 ActionResult.java → action_result.py

**Python:**
```python
from enum import Enum
from dataclasses import dataclass, field
from typing import Optional
from datetime import datetime
from .error_types import ErrorType

class ActionStatus(str, Enum):
    """Action execution status."""
    PASS = "PASS"
    FAIL = "FAIL"

@dataclass
class ActionResult:
    """Result of executing a UI action."""
    status: ActionStatus
    message: str
    error_type: Optional[ErrorType] = None
    screenshot_path: Optional[str] = None
    page_state: Optional[str] = None
    timestamp: datetime = field(default_factory=datetime.now)
    
    @classmethod
    def success(cls, message: str, page_state: Optional[str] = None) -> 'ActionResult':
        """Factory method for successful result."""
        return cls(
            status=ActionStatus.PASS,
            message=message,
            page_state=page_state
        )
    
    @classmethod
    def failure(cls, message: str, error_type: ErrorType, 
                screenshot_path: Optional[str] = None) -> 'ActionResult':
        """Factory method for failed result."""
        return cls(
            status=ActionStatus.FAIL,
            message=message,
            error_type=error_type,
            screenshot_path=screenshot_path
        )
    
    def __str__(self) -> str:
        return f"ActionResult({self.status}, {self.message})"
```

**Alternative with Pydantic (Recommended for validation):**
```python
from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional

class ActionResult(BaseModel):
    """Result of executing a UI action."""
    status: ActionStatus
    message: str
    error_type: Optional[ErrorType] = None
    screenshot_path: Optional[str] = None
    page_state: Optional[str] = None
    timestamp: datetime = Field(default_factory=datetime.now)
    
    model_config = {
        "frozen": False,  # Allow mutation if needed
        "use_enum_values": True
    }
```

#### 1.3 TestCase.java → test_case.py

**Python:**
```python
from dataclasses import dataclass, field
from typing import List, Optional
from .test_step import TestStep

@dataclass
class TestCase:
    """Represents a complete test case with metadata and steps."""
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
        """Add a test step to this test case."""
        self.test_steps.append(step)
    
    def to_goal_string(self) -> str:
        """
        Converts the test case to a formatted goal string for the LLM.
        This is the prompt that will be sent to the Action Agent.
        """
        lines = []
        lines.append(f"Test Case: {self.summary}")
        lines.append("")
        lines.append(f"Test Case Key: {self.key}")
        
        if self.description:
            lines.append("")
            lines.append("Description:")
            lines.append(self.description)
        
        if self.preconditions:
            lines.append("")
            lines.append("Preconditions:")
            lines.append(self.preconditions)
        
        lines.append("")
        lines.append("Test Steps:")
        lines.append("")
        
        for idx, step in enumerate(self.test_steps, 1):
            lines.append(f"Step {idx}:")
            lines.append(f"  Action: {step.step_summary}")
            if step.test_data:
                lines.append(f"  Test Data: {step.test_data}")
            if step.expected_result:
                lines.append(f"  Expected Result: {step.expected_result}")
            lines.append("")
        
        return "\n".join(lines)
```

#### 1.4 Selector.java → selector.py

**Python:**
```python
from enum import Enum
from dataclasses import dataclass
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

@dataclass
class Selector:
    """Represents a selector for locating UI elements."""
    type: SelectorType
    value: str
    role: Optional[str] = None           # For ROLE strategy
    near_text: Optional[str] = None      # Disambiguation text
    description: Optional[str] = None    # LLM-provided description
    
    def __str__(self) -> str:
        parts = [f"{self.type}={self.value}"]
        if self.role:
            parts.append(f"role={self.role}")
        if self.near_text:
            parts.append(f"near={self.near_text}")
        return f"Selector({', '.join(parts)})"
```

### 2. Driver Layer

#### 2.1 BrowserDriver.java → browser_driver.py

**Python:**
```python
from abc import ABC, abstractmethod
from typing import List
from playwright.sync_api import Page

class BrowserDriver(ABC):
    """Abstract base class for browser drivers."""
    
    @abstractmethod
    def start(self) -> None:
        """Initialize and start the browser."""
        pass
    
    @abstractmethod
    def get_page(self) -> Page:
        """Get the current page instance."""
        pass
    
    @abstractmethod
    def get_all_pages(self) -> List[Page]:
        """Get all open pages/tabs."""
        pass
    
    @abstractmethod
    def switch_to_page(self, index: int) -> None:
        """Switch to a specific page by index."""
        pass
    
    @abstractmethod
    def take_screenshot(self) -> bytes:
        """Capture screenshot of current page."""
        pass
    
    @abstractmethod
    def close(self) -> None:
        """Close the browser and cleanup resources."""
        pass
```

#### 2.2 PlaywrightDriver.java → playwright_driver.py

**Python:**
```python
from typing import List, Optional
from playwright.sync_api import sync_playwright, Browser, BrowserContext, Page, Playwright
from .browser_driver import BrowserDriver

class PlaywrightDriver(BrowserDriver):
    """Playwright implementation of BrowserDriver."""
    
    def __init__(self, headless: bool = False):
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
        """Get current page instance."""
        if self._page is None:
            raise RuntimeError("Driver not started. Call start() first.")
        return self._page
    
    def get_all_pages(self) -> List[Page]:
        """Get all open pages in the context."""
        if self._context is None:
            raise RuntimeError("Driver not started. Call start() first.")
        return self._context.pages
    
    def switch_to_page(self, index: int) -> None:
        """Switch to page at given index."""
        if self._context is None:
            raise RuntimeError("Driver not started. Call start() first.")
        
        pages = self._context.pages
        if not (0 <= index < len(pages)):
            raise ValueError(f"Invalid page index: {index}. Total pages: {len(pages)}")
        
        self._page = pages[index]
        self._page.bring_to_front()
    
    def take_screenshot(self) -> bytes:
        """Capture full-page screenshot."""
        if self._page is None:
            return b''
        return self._page.screenshot(full_page=True)
    
    def close(self) -> None:
        """Close browser and cleanup."""
        if self._context:
            self._context.close()
        if self._browser:
            self._browser.close()
        if self._playwright:
            self._playwright.stop()
    
    def __enter__(self):
        """Context manager entry."""
        self.start()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        self.close()
```

### 3. Executor Layer

#### 3.1 ActionExecutor.java → action_executor.py

**Python (Excerpt - Full implementation would be ~400 lines):**
```python
import os
import time
import uuid
from pathlib import Path
from typing import Optional
from playwright.sync_api import Page, Locator, TimeoutError as PlaywrightTimeout
from ..driver.browser_driver import BrowserDriver
from ..selector.selector_resolver import SelectorResolver
from ..model.action import Action, ActionType
from ..model.action_result import ActionResult, ActionStatus
from ..error.error_types import ErrorType
from .action_logger import ActionLogger

class ActionExecutor:
    """Executes UI actions using Playwright."""
    
    def __init__(self, driver: BrowserDriver, selector_resolver: SelectorResolver):
        self.driver = driver
        self.selector_resolver = selector_resolver
        self.capture_page_state = False
        self.action_logger: Optional[ActionLogger] = None
    
    def set_capture_page_state(self, capture: bool) -> None:
        """Enable/disable page state capture for validation."""
        self.capture_page_state = capture
    
    def set_action_logger(self, logger: ActionLogger) -> None:
        """Set action logger for this executor."""
        self.action_logger = logger
    
    def _resolve_value(self, value: Optional[str]) -> str:
        """
        Resolve values with ENV: prefix from environment variables.
        Format: ENV:VARIABLE_NAME
        """
        if not value:
            return ""
        
        trimmed = value.strip()
        if trimmed.upper().startswith("ENV:"):
            env_key = trimmed[4:].strip()
            env_val = os.getenv(env_key)
            
            if env_val:
                print(f"[SECRET] Resolved secret for key: {env_key}")
                return env_val
            
            print(f"CRITICAL ERROR: Secret '{env_key}' not found in environment variables.")
            return ""  # Return empty to prevent typing placeholder
        
        return value
    
    def execute(self, action: Action) -> ActionResult:
        """Execute a single UI action."""
        # Log action start
        if self.action_logger and self.action_logger.is_enabled:
            self.action_logger.log_action_start(action)
        
        try:
            page = self.driver.get_page()
            result = self._execute_action(action, page)
            
            # Log result
            if self.action_logger:
                self.action_logger.log_action_result(action, result)
            
            return result
            
        except Exception as e:
            error_result = ActionResult.failure(
                message=f"Action execution failed: {str(e)}",
                error_type=ErrorType.EXECUTION_ERROR,
                screenshot_path=self._capture_failure_screenshot(page)
            )
            
            if self.action_logger:
                self.action_logger.log_action_result(action, error_result)
            
            return error_result
    
    def _execute_action(self, action: Action, page: Page) -> ActionResult:
        """Execute specific action type."""
        
        if action.type == ActionType.NAVIGATE:
            return self._execute_navigate(action, page)
        
        elif action.type == ActionType.CLICK:
            return self._execute_click(action, page)
        
        elif action.type == ActionType.TYPE:
            return self._execute_type(action, page)
        
        elif action.type == ActionType.WAIT:
            return self._execute_wait(action, page)
        
        # ... other action types
        
        else:
            return ActionResult.failure(
                message=f"Unknown action type: {action.type}",
                error_type=ErrorType.INVALID_ACTION
            )
    
    def _execute_navigate(self, action: Action, page: Page) -> ActionResult:
        """Execute NAVIGATE action."""
        url = self._resolve_value(action.value)
        if not url:
            return ActionResult.failure(
                message="Navigation URL is missing",
                error_type=ErrorType.NAVIGATION_FAILED
            )
        
        page.goto(url)
        page_state = self._capture_page_state(page) if self.capture_page_state else None
        return ActionResult.success(f"Navigated to {action.value}", page_state)
    
    def _execute_click(self, action: Action, page: Page) -> ActionResult:
        """Execute CLICK action."""
        locator = self.selector_resolver.resolve(page, action.selector)
        if not locator:
            return ActionResult.failure(
                message="Selector could not be resolved",
                error_type=ErrorType.SELECTOR_ERROR
            )
        
        locator.click()
        page_state = self._capture_page_state(page) if self.capture_page_state else None
        return ActionResult.success(f"Clicked element {action.selector}", page_state)
    
    def _execute_type(self, action: Action, page: Page) -> ActionResult:
        """Execute TYPE action."""
        locator = self.selector_resolver.resolve(page, action.selector)
        if not locator:
            return ActionResult.failure(
                message="Selector could not be resolved",
                error_type=ErrorType.SELECTOR_ERROR
            )
        
        text_to_type = self._resolve_value(action.value)
        locator.fill(text_to_type)
        
        # Mask value in result if it was an ENV var
        displayed_value = "******" if action.value and action.value.startswith("ENV:") else action.value
        page_state = self._capture_page_state(page) if self.capture_page_state else None
        return ActionResult.success(f"Typed '{displayed_value}' into {action.selector}", page_state)
    
    def _execute_wait(self, action: Action, page: Page) -> ActionResult:
        """Execute WAIT action (static sleep)."""
        try:
            wait_seconds = int(action.value) if action.value else 5
        except ValueError:
            wait_seconds = 5
        
        # Clamp to reasonable range
        wait_seconds = max(0, min(wait_seconds, 60))
        
        time.sleep(wait_seconds)
        page_state = self._capture_page_state(page) if self.capture_page_state else None
        return ActionResult.success(f"Waited for {wait_seconds} second(s)", page_state)
    
    def _capture_page_state(self, page: Page) -> str:
        """Capture current page state for validation."""
        try:
            # Capture visible text content
            body_text = page.inner_text('body')
            return body_text[:5000]  # Limit to avoid token overflow
        except Exception:
            return ""
    
    def _capture_failure_screenshot(self, page: Page) -> Optional[str]:
        """Capture screenshot on failure."""
        try:
            screenshots_dir = Path("screenshots")
            screenshots_dir.mkdir(exist_ok=True)
            
            filename = f"failure-{uuid.uuid4()}.png"
            filepath = screenshots_dir / filename
            
            page.screenshot(path=str(filepath), full_page=True)
            return str(filepath)
        except Exception:
            return None
```

### 4. Agent Layer

#### 4.1 ActionAgent.java → action_agent.py

**Python (Excerpt - Core structure):**
```python
import os
import json
from typing import Optional, List, Dict, Any
from ..model.action import Action, ActionType
from ..model.action_result import ActionResult, ActionStatus
from ..model.selector import Selector, SelectorType
from .test_agent import TestAgent
from .service.llm_client import LlmClient
from .service.gemini_vertex_client import GeminiVertexLlmClient
from .service.groq_client import GroqLlmClient

class ActionAgent(TestAgent):
    """
    Action Agent - Uses LLM to decide the next UI action.
    
    In the two-agent architecture:
    - This is the ACTION AGENT that generates UI actions
    - Works alongside ValidationAgent which validates results
    """
    
    def __init__(self, goal: str):
        self.goal = goal
        self.messages: List[Dict[str, str]] = []
        self.is_complete = False
        self.consecutive_failures = 0
        self.max_consecutive_failures = 3
        
        # Initialize LLM client
        self.llm_client = self._create_llm_client()
        
        # Initialize conversation with system prompt and goal
        self._initialize_conversation()
    
    def _create_llm_client(self) -> LlmClient:
        """Create LLM client based on configuration."""
        provider = os.getenv("LLM_PROVIDER", "gemini-vertex").lower()
        
        if provider in ["gemini", "gemini-vertex", "vertex"]:
            project = os.getenv("VERTEX_PROJECT") or os.getenv("GOOGLE_CLOUD_PROJECT")
            location = os.getenv("VERTEX_LOCATION", "us-central1")
            model = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
            
            if not project:
                raise ValueError("Vertex project missing. Set VERTEX_PROJECT or GOOGLE_CLOUD_PROJECT.")
            
            return GeminiVertexLlmClient(project, location, model)
        
        else:  # groq
            api_key = os.getenv("GROQ_API_KEY")
            if not api_key:
                raise ValueError("GROQ_API_KEY environment variable is missing")
            
            return GroqLlmClient(api_key)
    
    def _initialize_conversation(self) -> None:
        """Initialize conversation with system prompt and goal."""
        system_prompt = self._get_system_prompt()
        
        self.messages.append({
            "role": "system",
            "content": system_prompt
        })
        
        self.messages.append({
            "role": "user",
            "content": f"GOAL: {self.goal}"
        })
    
    def _get_system_prompt(self) -> str:
        """Get the system prompt for the Action Agent."""
        return """You are a UI test automation agent...
        [Full system prompt from Java version]
        """
    
    def next_action(self, previous_result: Optional[ActionResult] = None) -> Optional[Action]:
        """
        Decide the next action based on the test goal and previous results.
        Returns None when the test is complete.
        """
        if self.is_complete:
            return None
        
        # Add previous result to conversation
        if previous_result:
            self._add_result_to_history(previous_result)
            
            if previous_result.status == ActionStatus.FAIL:
                self.consecutive_failures += 1
                print(f"Action failed ({self.consecutive_failures}/{self.max_consecutive_failures}): "
                      f"{previous_result.message}")
                
                if self.consecutive_failures >= self.max_consecutive_failures:
                    print("Too many consecutive failures. Stopping.")
                    self.is_complete = True
                    return None
            else:
                self.consecutive_failures = 0
        
        # Call LLM to get next action
        try:
            response = self._call_llm()
            action = self._parse_llm_response(response)
            
            # Add assistant decision to history
            self.messages.append({
                "role": "assistant",
                "content": json.dumps(response)
            })
            
            return action
            
        except Exception as e:
            print(f"Error getting next action from LLM: {e}")
            self.is_complete = True
            return None
    
    def _call_llm(self) -> Dict[str, Any]:
        """Call LLM and return parsed JSON response."""
        # Prepare request
        request_data = {
            "messages": self._get_recent_messages(),
            "temperature": 0.0
        }
        
        # Call LLM client
        response_json = self.llm_client.chat_completions(json.dumps(request_data))
        response = json.loads(response_json)
        
        # Extract content from response
        content = response["choices"][0]["message"]["content"]
        
        # Parse JSON from content
        return self._extract_json(content)
    
    def _parse_llm_response(self, response: Dict[str, Any]) -> Optional[Action]:
        """Parse LLM JSON response into Action object."""
        action_type_str = response.get("type", "").upper()
        
        # Check for completion
        if action_type_str == "COMPLETE":
            self.is_complete = True
            return None
        
        try:
            action_type = ActionType(action_type_str)
        except ValueError:
            print(f"Unknown action type: {action_type_str}")
            return None
        
        # Parse selector
        selector = None
        if "selector" in response:
            selector = self._parse_selector(response["selector"])
        
        # Get value
        value = response.get("value")
        
        return Action(
            type=action_type,
            selector=selector,
            value=value
        )
    
    def _parse_selector(self, selector_data: Dict[str, Any]) -> Selector:
        """Parse selector from JSON."""
        selector_type = SelectorType(selector_data.get("type", "TEXT").upper())
        value = selector_data.get("value", "")
        role = selector_data.get("role")
        near_text = selector_data.get("nearText")
        description = selector_data.get("description")
        
        return Selector(
            type=selector_type,
            value=value,
            role=role,
            near_text=near_text,
            description=description
        )
    
    def is_test_complete(self) -> bool:
        """Check if test execution is complete."""
        return self.is_complete
    
    def on_test_end(self, results: List[ActionResult]) -> None:
        """Called when test execution ends."""
        pass
```

### 5. Test Case Loader

#### 5.1 ExcelTestCaseLoader.java → excel_test_case_loader.py

**Python:**
```python
from pathlib import Path
from typing import List, Dict, Optional
import openpyxl
from openpyxl.worksheet.worksheet import Worksheet
from ..model.test_case import TestCase
from ..model.test_step import TestStep
from .test_case_loader import TestCaseLoader

class ExcelTestCaseLoader(TestCaseLoader):
    """Loads test cases from Excel files."""
    
    # Column names
    COL_ISSUE_KEY = "Issue Key"
    COL_SUMMARY = "Summary"
    COL_DESCRIPTION = "Description"
    COL_PRECONDITION = "Precondition"
    COL_STATUS = "Status"
    COL_PRIORITY = "Priority"
    COL_STEP_SUMMARY = "Step Summary"
    COL_TEST_STEP = "Test Step"
    COL_TEST_DATA = "Test Data"
    COL_EXPECTED_RESULT = "Expected Result"
    COL_TESTCASE_TYPE = "TestCase Type"
    COL_FUNCTIONAL_CATEGORY = "functional category"
    COL_COMPONENT_UNDER_TEST = "component under test"
    
    def __init__(self, file_path: str):
        self.file_path = Path(file_path)
    
    def load_test_cases(self) -> List[TestCase]:
        """Load all test cases from the Excel file."""
        if not self.file_path.exists():
            raise FileNotFoundError(f"Excel file not found: {self.file_path}")
        
        workbook = openpyxl.load_workbook(self.file_path)
        sheet = workbook.active
        
        # Read header row
        column_map = self._create_column_map(sheet)
        
        # Read data rows
        test_case_map: Dict[str, TestCase] = {}
        last_issue_key: Optional[str] = None
        
        for row in sheet.iter_rows(min_row=2, values_only=False):
            if self._is_empty_row(row):
                continue
            
            issue_key = self._get_cell_value(row, column_map.get(self.COL_ISSUE_KEY))
            
            # Use last issue key if current is empty (continuation)
            if not issue_key or not issue_key.strip():
                if last_issue_key:
                    issue_key = last_issue_key
                else:
                    continue
            else:
                last_issue_key = issue_key
            
            # Get or create test case
            test_case = test_case_map.get(issue_key)
            if test_case is None:
                test_case = TestCase(
                    key=issue_key,
                    summary=self._get_cell_value(row, column_map.get(self.COL_SUMMARY)) or "",
                    preconditions=self._get_cell_value(row, column_map.get(self.COL_PRECONDITION)),
                    description=self._get_cell_value(row, column_map.get(self.COL_DESCRIPTION)),
                    status=self._get_cell_value(row, column_map.get(self.COL_STATUS)),
                    priority=self._get_cell_value(row, column_map.get(self.COL_PRIORITY)),
                    test_case_type=self._get_cell_value(row, column_map.get(self.COL_TESTCASE_TYPE)),
                    functional_category=self._get_cell_value(row, column_map.get(self.COL_FUNCTIONAL_CATEGORY)),
                    component_under_test=self._get_cell_value(row, column_map.get(self.COL_COMPONENT_UNDER_TEST))
                )
                test_case_map[issue_key] = test_case
            
            # Add test step
            step_summary = self._get_cell_value(row, column_map.get(self.COL_STEP_SUMMARY))
            if not step_summary:
                step_summary = self._get_cell_value(row, column_map.get(self.COL_TEST_STEP))
            
            test_data = self._get_cell_value(row, column_map.get(self.COL_TEST_DATA))
            expected_result = self._get_cell_value(row, column_map.get(self.COL_EXPECTED_RESULT))
            
            if step_summary or expected_result:
                test_step = TestStep(
                    step_summary=step_summary or "",
                    test_data=test_data,
                    expected_result=expected_result
                )
                test_case.add_test_step(test_step)
        
        return list(test_case_map.values())
    
    def load_test_case(self, test_case_key: str) -> TestCase:
        """Load a specific test case by key."""
        all_test_cases = self.load_test_cases()
        
        for test_case in all_test_cases:
            if test_case.key == test_case_key:
                return test_case
        
        raise ValueError(f"Test case not found with key: {test_case_key}")
    
    def _create_column_map(self, sheet: Worksheet) -> Dict[str, int]:
        """Create mapping of column names to indices."""
        column_map = {}
        header_row = next(sheet.iter_rows(min_row=1, max_row=1, values_only=True))
        
        for idx, cell_value in enumerate(header_row):
            if cell_value:
                column_map[str(cell_value).strip()] = idx
        
        return column_map
    
    def _get_cell_value(self, row, column_index: Optional[int]) -> Optional[str]:
        """Get cell value as string."""
        if column_index is None or column_index >= len(row):
            return None
        
        cell = row[column_index]
        if cell.value is None:
            return None
        
        return str(cell.value).strip()
    
    def _is_empty_row(self, row) -> bool:
        """Check if row is empty."""
        return all(cell.value is None for cell in row)
```

### 6. LLM Service Layer

#### 6.1 GeminiVertexLlmClient.java → gemini_vertex_client.py

**Python:**
```python
import json
from typing import Dict, Any
from google.cloud import aiplatform
from google.auth import default
from .llm_client import LlmClient

class GeminiVertexLlmClient(LlmClient):
    """Gemini implementation using Vertex AI."""
    
    def __init__(self, project_id: str, location: str, model: str):
        self.project_id = project_id
        self.location = location
        self.model = model
        
        # Initialize Vertex AI
        aiplatform.init(project=project_id, location=location)
    
    def chat_completions(self, request_json: str) -> str:
        """Call Vertex AI Gemini API."""
        request_data = json.loads(request_json)
        
        # Extract parameters
        temperature = request_data.get("temperature", 0.0)
        messages = request_data.get("messages", [])
        
        # Extract system prompt and user messages
        system_prompt = self._extract_system_prompt(messages)
        user_prompt = self._flatten_messages(messages)
        
        # Combine prompts
        full_prompt = f"{system_prompt}\n\n{user_prompt}" if system_prompt else user_prompt
        
        # Call Vertex AI
        from vertexai.preview.generative_models import GenerativeModel
        
        model = GenerativeModel(self.model)
        response = model.generate_content(
            full_prompt,
            generation_config={
                "temperature": temperature,
                "max_output_tokens": 2048,
            }
        )
        
        # Format response to OpenAI-like structure
        response_text = response.text
        
        return json.dumps({
            "choices": [{
                "message": {
                    "content": response_text
                }
            }]
        })
    
    def provider_name(self) -> str:
        """Return provider name."""
        return "gemini-vertex"
    
    def _extract_system_prompt(self, messages: list) -> str:
        """Extract system prompt from messages."""
        for msg in messages:
            if msg.get("role") == "system":
                return msg.get("content", "")
        return ""
    
    def _flatten_messages(self, messages: list) -> str:
        """Flatten conversation messages into single prompt."""
        user_messages = [
            msg.get("content", "")
            for msg in messages
            if msg.get("role") in ["user", "assistant"]
        ]
        return "\n\n".join(user_messages)
```

---

## Configuration & Environment

### Environment Variables (.env file)

Create a `.env` file:

```bash
# LLM Provider Configuration
LLM_PROVIDER=gemini-vertex  # Options: gemini-vertex, groq

# Vertex AI (Google Cloud) Settings
VERTEX_PROJECT=your-project-id
VERTEX_LOCATION=us-central1
GEMINI_MODEL=gemini-2.5-flash

# Groq Settings (if using Groq)
# GROQ_API_KEY=your-groq-api-key
# GROQ_MODEL=openai/gpt-oss-safeguard-20b

# Agent Settings
LLM_USAGE_LOG_ENABLED=true
LLM_USAGE_LOG_FILE=llm-usage.txt
LLM_MAX_EXECUTION_MESSAGES_TO_SEND=10

# Application Secrets (for test data)
MY_SSO_PASSWORD=your-password
MY_APP_PASSWORD=your-password

# Google Cloud Authentication
# GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
```

### Settings Management (config/settings.py)

```python
from pydantic_settings import BaseSettings
from typing import Optional

class Settings(BaseSettings):
    """Application settings loaded from environment variables."""
    
    # LLM Configuration
    llm_provider: str = "gemini-vertex"
    
    # Vertex AI
    vertex_project: Optional[str] = None
    vertex_location: str = "us-central1"
    gemini_model: str = "gemini-2.5-flash"
    
    # Groq
    groq_api_key: Optional[str] = None
    groq_model: str = "openai/gpt-oss-safeguard-20b"
    
    # Agent Settings
    llm_usage_log_enabled: bool = True
    llm_usage_log_file: str = "llm-usage.txt"
    llm_max_execution_messages_to_send: int = 10
    
    # Secrets
    my_sso_password: Optional[str] = None
    my_app_password: Optional[str] = None
    
    # Google Cloud
    google_application_credentials: Optional[str] = None
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False

# Global settings instance
settings = Settings()
```

---

## Testing Strategy

### Pytest Configuration (pytest.ini)

```ini
[pytest]
testpaths = tests
python_files = test_*.py
python_classes = Test*
python_functions = test_*
addopts = 
    -v
    --tb=short
    --strict-markers
    --color=yes
markers =
    unit: Unit tests
    integration: Integration tests
    slow: Slow running tests
    llm: Tests that call LLM APIs
```

### Example Test (tests/test_action_executor.py)

```python
import pytest
from playwright_ui_adapter.model.action import Action, ActionType
from playwright_ui_adapter.model.selector import Selector, SelectorType
from playwright_ui_adapter.executor.action_executor import ActionExecutor
from playwright_ui_adapter.driver.playwright_driver import PlaywrightDriver
from playwright_ui_adapter.selector.selector_resolver import SelectorResolver

@pytest.fixture
def driver():
    """Create and start a test driver."""
    driver = PlaywrightDriver(headless=True)
    driver.start()
    yield driver
    driver.close()

@pytest.fixture
def executor(driver):
    """Create action executor."""
    resolver = SelectorResolver()
    return ActionExecutor(driver, resolver)

def test_navigate_action(executor):
    """Test NAVIGATE action execution."""
    action = Action(
        type=ActionType.NAVIGATE,
        value="https://example.com"
    )
    
    result = executor.execute(action)
    
    assert result.status == ActionStatus.PASS
    assert "Navigated to" in result.message

@pytest.mark.integration
def test_click_action(executor):
    """Test CLICK action execution."""
    # Navigate first
    nav_action = Action(
        type=ActionType.NAVIGATE,
        value="https://example.com"
    )
    executor.execute(nav_action)
    
    # Click action
    click_action = Action(
        type=ActionType.CLICK,
        selector=Selector(type=SelectorType.TEXT, value="More information")
    )
    
    result = executor.execute(click_action)
    assert result.status == ActionStatus.PASS
```

---

## Migration Checklist

### Phase 1: Project Setup
- [ ] Create Python project structure
- [ ] Set up Poetry or pip with requirements.txt
- [ ] Configure pyproject.toml
- [ ] Set up .env file and python-dotenv
- [ ] Create .gitignore for Python
- [ ] Set up pytest configuration
- [ ] Create README.md for Python version

### Phase 2: Model Layer (Data Classes)
- [ ] Convert Action.java → action.py
- [ ] Convert ActionResult.java → action_result.py
- [ ] Convert Selector.java → selector.py
- [ ] Convert TestCase.java → test_case.py
- [ ] Convert TestStep.java → test_step.py
- [ ] Convert ValidationResult.java → validation_result.py
- [ ] Convert TestRunResult.java → test_run_result.py
- [ ] Convert ErrorType.java → error_types.py
- [ ] Write unit tests for all models

### Phase 3: Driver Layer
- [ ] Convert BrowserDriver interface → browser_driver.py (ABC)
- [ ] Convert PlaywrightDriver → playwright_driver.py
- [ ] Test browser startup and page management
- [ ] Test screenshot capture
- [ ] Test multi-tab handling

### Phase 4: Selector Layer
- [ ] Convert SelectorResolver.java → selector_resolver.py
- [ ] Test all selector strategies (CSS, XPATH, TEXT, ROLE, etc.)
- [ ] Test ambiguous selector handling
- [ ] Test nearText filtering

### Phase 5: Executor Layer
- [ ] Convert ActionExecutor.java → action_executor.py
- [ ] Implement all action types (NAVIGATE, CLICK, TYPE, etc.)
- [ ] Implement ENV: variable resolution
- [ ] Convert ActionLogger.java → action_logger.py
- [ ] Test action execution with all action types
- [ ] Test failure handling and screenshot capture

### Phase 6: LLM Service Layer
- [ ] Convert LlmClient interface → llm_client.py (ABC)
- [ ] Convert GeminiVertexLlmClient → gemini_vertex_client.py
- [ ] Convert GroqLlmClient → groq_client.py
- [ ] Convert LlmUsageLogger → llm_usage_logger.py
- [ ] Convert TokenCounter → token_counter.py
- [ ] Test LLM client integration (with mocking)

### Phase 7: Agent Layer
- [ ] Convert TestAgent interface → test_agent.py (ABC)
- [ ] Convert ActionAgent.java → action_agent.py
- [ ] Convert ValidationAgent.java → validation_agent.py
- [ ] Test agent decision making (with mocked LLM)
- [ ] Test conversation history management
- [ ] Test failure handling and retry logic

### Phase 8: Orchestrator Layer
- [ ] Convert TwoAgentOrchestrator → two_agent_orchestrator.py
- [ ] Test two-agent coordination
- [ ] Test validation flow
- [ ] Test result aggregation

### Phase 9: Test Case Loader
- [ ] Convert TestCaseLoader interface → test_case_loader.py (ABC)
- [ ] Convert ExcelTestCaseLoader → excel_test_case_loader.py
- [ ] Test Excel parsing with sample files
- [ ] Test multi-row test case grouping
- [ ] Test column mapping

### Phase 10: Adapter Layer
- [ ] Convert UIActionAdapter → ui_action_adapter.py
- [ ] Test high-level action execution
- [ ] Test test run orchestration

### Phase 11: Entry Points
- [ ] Convert Main.java → scripts/main.py
- [ ] Convert ValidationMain.java → scripts/validation_main.py
- [ ] Create CLI using argparse or Click
- [ ] Test end-to-end execution

### Phase 12: Configuration
- [ ] Create settings.py with Pydantic
- [ ] Migrate application.properties → .env
- [ ] Test configuration loading
- [ ] Document environment variables

### Phase 13: Documentation
- [ ] Update all .md files for Python
- [ ] Create Python-specific examples
- [ ] Document installation process
- [ ] Create development guide
- [ ] Add API documentation (docstrings)

### Phase 14: Testing & Quality
- [ ] Write comprehensive unit tests (>80% coverage)
- [ ] Write integration tests
- [ ] Set up pytest fixtures
- [ ] Configure mypy for type checking
- [ ] Configure black for code formatting
- [ ] Configure ruff for linting
- [ ] Run all tests and fix issues

### Phase 15: Deployment
- [ ] Create setup.py or use Poetry build
- [ ] Test package installation
- [ ] Create Docker container (optional)
- [ ] Document deployment process

---

## Python Best Practices

### 1. Type Hints
Always use type hints for better IDE support and catch errors early:

```python
def execute_action(self, action: Action, page: Page) -> ActionResult:
    """Execute a single action."""
    pass
```

### 2. Use Dataclasses or Pydantic
For simple data structures, use `@dataclass`. For validation, use Pydantic:

```python
from dataclasses import dataclass
from pydantic import BaseModel, Field

# Simple data
@dataclass
class Action:
    type: ActionType
    value: str

# With validation
class ActionConfig(BaseModel):
    timeout: int = Field(gt=0, le=60)
    retry_count: int = Field(ge=0, le=5)
```

### 3. Use Enums for Constants
```python
from enum import Enum

class ActionType(str, Enum):
    CLICK = "CLICK"
    TYPE = "TYPE"
```

### 4. Context Managers
Use context managers for resource management:

```python
with PlaywrightDriver() as driver:
    driver.get_page().goto("https://example.com")
# Automatic cleanup
```

### 5. Async/Await (Optional Enhancement)
Consider using async Playwright for better performance:

```python
from playwright.async_api import async_playwright

async def execute_async():
    async with async_playwright() as p:
        browser = await p.chromium.launch()
        # ...
```

### 6. Logging
Use proper logging instead of print statements:

```python
import logging

logger = logging.getLogger(__name__)
logger.info("Executing action: %s", action)
```

### 7. Error Handling
Use specific exceptions and proper error handling:

```python
class ActionExecutionError(Exception):
    """Raised when action execution fails."""
    pass

try:
    result = executor.execute(action)
except PlaywrightTimeout:
    # Handle timeout specifically
    pass
except ActionExecutionError as e:
    logger.error("Action failed: %s", e)
```

### 8. Documentation
Use docstrings with proper formatting:

```python
def execute(self, action: Action) -> ActionResult:
    """
    Execute a UI action.
    
    Args:
        action: The action to execute
        
    Returns:
        ActionResult containing execution status and details
        
    Raises:
        ActionExecutionError: If action execution fails
        
    Example:
        >>> action = Action(type=ActionType.CLICK, selector=...)
        >>> result = executor.execute(action)
    """
    pass
```

### 9. Code Formatting
Use Black and Ruff for consistent code style:

```bash
# Format code
black playwright_ui_adapter/

# Lint code
ruff check playwright_ui_adapter/

# Type check
mypy playwright_ui_adapter/
```

### 10. Virtual Environments
Always use virtual environments:

```bash
# Using venv
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Using Poetry (recommended)
poetry install
poetry shell
```

---

## Critical Conversion Notes

### 1. **Java Static Methods → Python Class Methods or Module Functions**
Java static utilities should become either class methods, static methods, or module-level functions.

### 2. **Java Interfaces → Python Abstract Base Classes**
Use `abc.ABC` and `@abstractmethod`:

```python
from abc import ABC, abstractmethod

class TestAgent(ABC):
    @abstractmethod
    def next_action(self, result: Optional[ActionResult]) -> Optional[Action]:
        pass
```

### 3. **Java Enums → Python Enums**
Use `enum.Enum` or `enum.StrEnum` (Python 3.11+):

```python
from enum import Enum, auto

class ActionType(str, Enum):
    CLICK = "CLICK"
    TYPE = "TYPE"
```

### 4. **Java Properties → Python Properties**
Use `@property` decorator:

```python
@property
def is_complete(self) -> bool:
    return self._is_complete
```

### 5. **Java Exceptions → Python Exceptions**
Create custom exception hierarchy:

```python
class UIAdapterError(Exception):
    """Base exception for UI Adapter."""
    pass

class ActionExecutionError(UIAdapterError):
    """Action execution failed."""
    pass
```

### 6. **Java Optional → Python Optional**
Use `Optional[Type]` from typing:

```python
from typing import Optional

def get_value(self) -> Optional[str]:
    return self._value  # Can be None
```

### 7. **Java Streams → Python Comprehensions/Generators**
Convert Java streams to Python list comprehensions or generator expressions:

```java
// Java
long passCount = results.stream()
    .filter(r -> r.getStatus() == Status.PASS)
    .count();
```

```python
# Python
pass_count = sum(1 for r in results if r.status == ActionStatus.PASS)
```

### 8. **Maven Plugin Execution → Python Scripts**
Convert Maven exec goals to Python scripts or Poetry scripts.

### 9. **Java Package Structure → Python Module Structure**
Java packages become Python packages with `__init__.py` files.

### 10. **Resource Files**
In Java: `src/main/resources/application.properties`
In Python: `.env` file with `python-dotenv` or config files

---

## Installation & Setup (Python Version)

### Using Poetry (Recommended)

```bash
# Clone repository
git clone <repo-url>
cd playwright-ui-adapter

# Install dependencies with Poetry
poetry install

# Activate virtual environment
poetry shell

# Install Playwright browsers
playwright install chromium

# Set up environment variables
cp config/.env.example .env
# Edit .env with your configuration

# Run tests
pytest

# Run main script
poetry run python scripts/main.py --testcase path/to/testcases.xlsx
```

### Using pip

```bash
# Create virtual environment
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Install Playwright browsers
playwright install chromium

# Set up environment variables
cp config/.env.example .env
# Edit .env with your configuration

# Run tests
pytest

# Run main script
python scripts/main.py --testcase path/to/testcases.xlsx
```

---

## Summary

This conversion guide provides a comprehensive roadmap for migrating the Playwright UI Action Adapter from Java to Python. Key points:

1. **Architecture Preservation**: Maintain the two-agent architecture and component separation
2. **Modern Python Practices**: Use type hints, Pydantic, Poetry, and pytest
3. **Equivalent Technologies**: Map Java libraries to their Python equivalents
4. **Testing Strategy**: Implement comprehensive unit and integration tests
5. **Configuration Management**: Use .env files and Pydantic Settings
6. **Code Quality**: Set up Black, Ruff, and mypy for code quality

**Estimated Effort**: 40-60 hours for complete conversion with testing

**Recommended Approach**: 
- Start with model layer (easiest, no dependencies)
- Progress through driver, executor, and agent layers
- Test each layer thoroughly before moving to the next
- Use TDD (Test-Driven Development) approach
- Maintain parallel Java/Python versions during transition

**Success Criteria**:
- All tests passing
- 80%+ code coverage
- Type checking passing with mypy
- Same functionality as Java version
- Comprehensive documentation
