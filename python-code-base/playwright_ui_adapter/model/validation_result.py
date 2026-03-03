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
        reasoning: LLM-provided reasoning for the decision
        confidence: Confidence score (0.0 to 1.0)
        expected_result: The expected outcome
        actual_result: The actual outcome
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
            confidence=0.5,
            expected_result=expected_result,
            actual_result=actual_result
        )
    
    def to_detailed_report(self) -> str:
        """
        Generate a detailed validation report.
        
        Returns:
            A formatted string with validation details
        """
        lines = []
        lines.append(f"Validation Status: {self.status}")
        lines.append(f"Confidence: {self.confidence:.0%}")
        lines.append(f"Reasoning: {self.reasoning}")
        lines.append("")
        lines.append(f"Expected Result: {self.expected_result}")
        lines.append(f"Actual Result: {self.actual_result}")
        lines.append("")
        lines.append(f"Timestamp: {self.timestamp.strftime('%Y-%m-%d %H:%M:%S')}")
        return "\n".join(lines)
    
    def __str__(self) -> str:
        return f"{self.status} (confidence: {self.confidence:.0%}): {self.reasoning}"
    
    def __repr__(self) -> str:
        return f"ValidationResult({self.status}, confidence={self.confidence:.2f})"
