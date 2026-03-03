"""
Example script demonstrating basic usage of the framework.

This script shows how to:
1. Load test cases from Excel
2. Create and start a browser driver
3. Access test case data

For full test execution, see main.py after implementing the executor layer.
"""

import sys
from pathlib import Path

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from playwright_ui_adapter import PlaywrightDriver, TestCase, TestStep
from playwright_ui_adapter.testcase import ExcelTestCaseLoader


def main():
    """Example usage."""
    print("=" * 80)
    print("Playwright UI Action Adapter - Example Usage")
    print("=" * 80)
    
    # Example: Create a test case programmatically
    print("\n1. Creating a test case programmatically:")
    test_case = TestCase(
        key="TC-001",
        summary="Login Test",
        preconditions="User is on login page"
    )
    
    test_case.add_test_step(TestStep(
        step_summary="Enter username",
        test_data="admin",
        expected_result="Username field is populated"
    ))
    
    test_case.add_test_step(TestStep(
        step_summary="Enter password",
        test_data="admin123",
        expected_result="Password field is populated"
    ))
    
    test_case.add_test_step(TestStep(
        step_summary="Click login button",
        expected_result="User is redirected to dashboard"
    ))
    
    print(f"Test Case: {test_case.key} - {test_case.summary}")
    print(f"Steps: {len(test_case.test_steps)}")
    print("\nGoal String:")
    print(test_case.to_goal_string())
    
    # Example: Start browser driver
    print("\n2. Starting browser driver:")
    driver = PlaywrightDriver(headless=False)
    
    try:
        driver.start()
        print("✓ Browser started successfully!")
        
        page = driver.get_page()
        print(f"✓ Got page: {page}")
        
        # Navigate to example page
        print("\n3. Navigating to example.com:")
        page.goto("https://example.com")
        print(f"✓ Page title: {page.title()}")
        
        # Take screenshot
        print("\n4. Taking screenshot:")
        screenshot_bytes = driver.take_screenshot()
        print(f"✓ Screenshot captured: {len(screenshot_bytes)} bytes")
        
    finally:
        print("\n5. Closing browser:")
        driver.close()
        print("✓ Browser closed")
    
    print("\n" + "=" * 80)
    print("Example completed successfully!")
    print("=" * 80)


if __name__ == "__main__":
    main()
