"""Abstract base class for test case loaders."""

from abc import ABC, abstractmethod
from pathlib import Path
from typing import List

from ..model.test_case import TestCase


class TestCaseLoader(ABC):
    """
    Abstract base class for test case loaders.
    
    Defines the contract for loading test cases from various sources.
    """
    
    @abstractmethod
    def load_test_cases(self, file_path: Path) -> List[TestCase]:
        """
        Load test cases from a file.
        
        Args:
            file_path: Path to the test case file
            
        Returns:
            List of loaded test cases
            
        Raises:
            FileNotFoundError: If file doesn't exist
            ValueError: If file format is invalid
        """
        pass
