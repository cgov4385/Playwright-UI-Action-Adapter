"""Excel test case loader implementation."""

from pathlib import Path
from typing import List, Dict, Any, Optional

import openpyxl
from openpyxl.worksheet.worksheet import Worksheet

from ..model.test_case import TestCase
from ..model.test_step import TestStep
from .test_case_loader import TestCaseLoader


class ExcelTestCaseLoader(TestCaseLoader):
    """
    Loads test cases from Excel files.
    
    Expected Excel format:
    - Each row represents a test step
    - Rows with the same "Issue Key" are grouped into one test case
    - Required columns: Issue Key, Test Summary, Test Step, Expected Result
    - Optional columns: Preconditions, Test Data, Priority, etc.
    
    Example:
        >>> loader = ExcelTestCaseLoader()
        >>> test_cases = loader.load_test_cases(Path("testcases.xlsx"))
        >>> for test_case in test_cases:
        ...     print(f"Test: {test_case.summary}")
    """
    
    # Default column mappings (can be customized)
    DEFAULT_COLUMNS = {
        "issue_key": "Issue Key",
        "summary": "Test Summary",
        "preconditions": "Preconditions",
        "test_step": "Test Step",
        "test_data": "Test Data",
        "expected_result": "Expected Result",
        "description": "Description",
        "status": "Status",
        "priority": "Priority",
        "test_case_type": "Test Case Type",
        "functional_category": "Functional Category",
        "component": "Component Under Test",
    }
    
    def __init__(self, column_mapping: Optional[Dict[str, str]] = None):
        """
        Initialize the Excel loader.
        
        Args:
            column_mapping: Optional custom column name mapping
        """
        self.column_mapping = column_mapping or self.DEFAULT_COLUMNS
    
    def load_test_cases(self, file_path: Path) -> List[TestCase]:
        """
        Load test cases from an Excel file.
        
        Args:
            file_path: Path to the Excel file
            
        Returns:
            List of loaded test cases
            
        Raises:
            FileNotFoundError: If file doesn't exist
            ValueError: If file format is invalid
        """
        if not file_path.exists():
            raise FileNotFoundError(f"Test case file not found: {file_path}")
        
        # Load workbook and get first sheet
        workbook = openpyxl.load_workbook(file_path)
        sheet = workbook.active
        
        # Parse rows into test cases
        test_cases = self._parse_sheet(sheet)
        
        workbook.close()
        return test_cases
    
    def _parse_sheet(self, sheet: Worksheet) -> List[TestCase]:
        """
        Parse worksheet into test cases.
        
        Args:
            sheet: The worksheet to parse
            
        Returns:
            List of test cases
        """
        # Get header row and create column index mapping
        headers = [cell.value for cell in sheet[1]]
        col_indices = self._create_column_indices(headers)
        
        # Group rows by Issue Key
        test_case_dict: Dict[str, TestCase] = {}
        
        for row_idx in range(2, sheet.max_row + 1):
            row = sheet[row_idx]
            row_data = self._extract_row_data(row, col_indices)
            
            issue_key = row_data.get("issue_key")
            if not issue_key:
                continue  # Skip rows without Issue Key
            
            # Create or update test case
            if issue_key not in test_case_dict:
                test_case_dict[issue_key] = self._create_test_case(row_data)
            
            # Add test step
            test_step = self._create_test_step(row_data)
            if test_step:
                test_case_dict[issue_key].add_test_step(test_step)
        
        return list(test_case_dict.values())
    
    def _create_column_indices(self, headers: List[Any]) -> Dict[str, int]:
        """
        Create mapping from field names to column indices.
        
        Args:
            headers: List of column headers
            
        Returns:
            Dictionary mapping field names to column indices
        """
        col_indices = {}
        for field_name, column_name in self.column_mapping.items():
            try:
                col_indices[field_name] = headers.index(column_name)
            except ValueError:
                # Column not found, will be None in row data
                pass
        return col_indices
    
    def _extract_row_data(self, row, col_indices: Dict[str, int]) -> Dict[str, str]:
        """
        Extract data from a row based on column indices.
        
        Args:
            row: The worksheet row
            col_indices: Column index mapping
            
        Returns:
            Dictionary of field values
        """
        row_data = {}
        for field_name, col_idx in col_indices.items():
            cell_value = row[col_idx].value
            row_data[field_name] = str(cell_value).strip() if cell_value else None
        return row_data
    
    def _create_test_case(self, row_data: Dict[str, str]) -> TestCase:
        """
        Create a TestCase from row data.
        
        Args:
            row_data: Dictionary of row values
            
        Returns:
            TestCase instance
        """
        return TestCase(
            key=row_data.get("issue_key", ""),
            summary=row_data.get("summary", ""),
            preconditions=row_data.get("preconditions"),
            description=row_data.get("description"),
            status=row_data.get("status"),
            priority=row_data.get("priority"),
            test_case_type=row_data.get("test_case_type"),
            functional_category=row_data.get("functional_category"),
            component_under_test=row_data.get("component"),
        )
    
    def _create_test_step(self, row_data: Dict[str, str]) -> Optional[TestStep]:
        """
        Create a TestStep from row data.
        
        Args:
            row_data: Dictionary of row values
            
        Returns:
            TestStep instance or None if no step data
        """
        step_summary = row_data.get("test_step")
        if not step_summary:
            return None
        
        return TestStep(
            step_summary=step_summary,
            test_data=row_data.get("test_data"),
            expected_result=row_data.get("expected_result"),
        )
