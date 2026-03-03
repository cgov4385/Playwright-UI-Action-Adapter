"""Test run result model for complete test execution results."""

from dataclasses import dataclass, field
from datetime import datetime
from typing import List

from .action_result import ActionResult, ActionStatus
from .validation_result import ValidationResult, ValidationStatus


@dataclass
class TestRunResult:
    """
    Result of running a complete test case.
    
    Aggregates action results and validation results for a test run.
    
    Attributes:
        test_key: Test case identifier
        test_summary: Test case summary
        action_results: List of action execution results
        validation_results: List of validation results (for two-agent mode)
        start_time: When the test started
        end_time: When the test completed
        overall_status: Overall test status (PASS/FAIL)
    """
    
    test_key: str
    test_summary: str
    action_results: List[ActionResult] = field(default_factory=list)
    validation_results: List[ValidationResult] = field(default_factory=list)
    start_time: datetime = field(default_factory=datetime.now)
    end_time: datetime = field(default_factory=datetime.now)
    overall_status: str = "UNKNOWN"
    
    def calculate_overall_status(self) -> str:
        """
        Calculate the overall test status based on action and validation results.
        
        Returns:
            "PASS" if all actions passed (and validations if present), "FAIL" otherwise
        """
        # Check action results
        action_failed = any(r.status == ActionStatus.FAIL for r in self.action_results)
        
        # Check validation results if present
        validation_failed = any(r.status == ValidationStatus.FAIL for r in self.validation_results)
        
        if action_failed or validation_failed:
            self.overall_status = "FAIL"
        elif self.action_results:
            self.overall_status = "PASS"
        else:
            self.overall_status = "UNKNOWN"
        
        return self.overall_status
    
    def get_duration_seconds(self) -> float:
        """
        Get test execution duration in seconds.
        
        Returns:
            Duration in seconds
        """
        return (self.end_time - self.start_time).total_seconds()
    
    def to_summary_string(self) -> str:
        """
        Generate a summary string for the test run.
        
        Returns:
            A formatted string with test run summary
        """
        lines = []
        lines.append("=" * 80)
        lines.append(f"Test Run Summary: {self.test_key}")
        lines.append(f"Test: {self.test_summary}")
        lines.append("=" * 80)
        lines.append(f"Status: {self.overall_status}")
        lines.append(f"Duration: {self.get_duration_seconds():.2f} seconds")
        lines.append(f"Start Time: {self.start_time.strftime('%Y-%m-%d %H:%M:%S')}")
        lines.append(f"End Time: {self.end_time.strftime('%Y-%m-%d %H:%M:%S')}")
        lines.append("")
        lines.append(f"Actions Executed: {len(self.action_results)}")
        
        action_pass = sum(1 for r in self.action_results if r.status == ActionStatus.PASS)
        action_fail = sum(1 for r in self.action_results if r.status == ActionStatus.FAIL)
        lines.append(f"  - Passed: {action_pass}")
        lines.append(f"  - Failed: {action_fail}")
        
        if self.validation_results:
            lines.append("")
            lines.append(f"Validations Performed: {len(self.validation_results)}")
            val_pass = sum(1 for r in self.validation_results if r.status == ValidationStatus.PASS)
            val_fail = sum(1 for r in self.validation_results if r.status == ValidationStatus.FAIL)
            val_unknown = sum(1 for r in self.validation_results if r.status == ValidationStatus.UNKNOWN)
            lines.append(f"  - Passed: {val_pass}")
            lines.append(f"  - Failed: {val_fail}")
            lines.append(f"  - Unknown: {val_unknown}")
        
        lines.append("=" * 80)
        return "\n".join(lines)
    
    def __str__(self) -> str:
        return f"TestRunResult({self.test_key}, status={self.overall_status}, actions={len(self.action_results)})"
    
    def __repr__(self) -> str:
        return self.__str__()
