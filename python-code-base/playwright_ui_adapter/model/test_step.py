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
        test_data: Optional test data for the step (e.g., "username: admin, password: admin123")
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
