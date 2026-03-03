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
        preconditions: Optional preconditions for the test
        test_steps: List of test steps to execute
        description: Optional detailed description
        status: Optional test status (e.g., "Ready for Testing")
        priority: Optional priority level
        test_case_type: Optional test type (e.g., "Functional")
        functional_category: Optional functional category
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
            step: The test step to add
        """
        self.test_steps.append(step)
    
    def to_goal_string(self) -> str:
        """
        Convert test case to a natural language goal string for the LLM.
        
        Returns:
            A formatted string describing the test goal with all steps
        
        Example:
            Test Case: Login Functionality
            Preconditions: User is on login page
            
            Step 1: Enter valid username
            Data: username: admin
            Expected: Username field is populated
            
            Step 2: Enter valid password
            Data: password: admin123
            Expected: Password field is populated
            
            Step 3: Click login button
            Expected: User is redirected to dashboard
        """
        lines = []
        lines.append(f"Test Case: {self.summary}")
        
        if self.preconditions:
            lines.append(f"Preconditions: {self.preconditions}")
        
        if self.description:
            lines.append(f"Description: {self.description}")
        
        lines.append("")  # Blank line
        
        for i, step in enumerate(self.test_steps, 1):
            lines.append(f"Step {i}: {step.step_summary}")
            if step.test_data:
                lines.append(f"Data: {step.test_data}")
            if step.expected_result:
                lines.append(f"Expected: {step.expected_result}")
            lines.append("")  # Blank line between steps
        
        return "\n".join(lines)
    
    def __str__(self) -> str:
        return f"TestCase({self.key}: {self.summary}, {len(self.test_steps)} steps)"
    
    def __repr__(self) -> str:
        return self.__str__()
