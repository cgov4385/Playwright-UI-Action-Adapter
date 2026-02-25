# Excel Test Case Loader

This document explains how to use the Excel Test Case Loader to load test cases from Excel files and execute them using the LlmAgent.

## Overview

The Excel Test Case Loader provides a structured way to:
1. Define test cases in Excel spreadsheets
2. Load test cases programmatically
3. Convert test cases to formatted goal strings
4. Execute test cases using the LlmAgent

## Components

### 1. TestStep Model
Located at: `ui_adapter.model.TestStep`

Represents a single test step with:
- **stepSummary**: Description of what to do
- **testData**: Data to use in this step
- **expectedResult**: What should happen

### 2. TestCase Model
Located at: `ui_adapter.model.TestCase`

Represents a complete test case with:
- **key**: Unique identifier (e.g., Issue Key from Jira)
- **summary**: Brief description of the test
- **preconditions**: What must be true before test execution
- **testSteps**: List of TestStep objects
- Additional metadata: description, status, priority, etc.

### 3. ExcelTestCaseLoader
Located at: `ui_adapter.testcase.ExcelTestCaseLoader`

Reads test cases from Excel (.xlsx) files.

## Excel File Format

Your Excel file should have these columns (as shown in your example):

| Column Name | Description | Required |
|-------------|-------------|----------|
| Issue Key | Unique test case identifier | Yes |
| Summary | Brief test case description | Yes |
| Description | Detailed description | No |
| Precondition | Prerequisites for the test | No |
| Step Summary | Individual test step description | No |
| Test Data | Data for the test step | No |
| Expected Result | Expected outcome of the step | No |
| Status | Test case status | No |
| Priority | Test case priority | No |
| TestCase Type | Type of test (Manual, Automated, etc.) | No |
| functional category | Functional area being tested | No |
| component under test | Specific component | No |

**Note**: Multiple rows with the same Issue Key will be grouped into a single test case with multiple steps.

## Usage Examples

### Example 1: Load All Test Cases from Excel

```java
import ui_adapter.testcase.ExcelTestCaseLoader;
import ui_adapter.testcase.TestCaseLoader;
import ui_adapter.model.TestCase;
import ui_adapter.agent.LlmAgent;

// Create loader with path to Excel file
String excelPath = "C:/path/to/testcases.xlsx";
TestCaseLoader loader = new ExcelTestCaseLoader(excelPath);

// Load all test cases
List<TestCase> testCases = loader.loadTestCases();

// Execute each test case
for (TestCase testCase : testCases) {
    System.out.println("Executing: " + testCase.getKey());
    
    // Convert to goal string for LlmAgent
    String goal = testCase.toGoalString();
    
    // Create and run agent
    TestAgent agent = new LlmAgent(goal);
    // ... execute test ...
}
```

### Example 2: Load Specific Test Case by Key

```java
String excelPath = "C:/path/to/testcases.xlsx";
TestCaseLoader loader = new ExcelTestCaseLoader(excelPath);

// Load specific test case
TestCase testCase = loader.loadTestCase("TEST-123");

// Convert to goal and execute
String goal = testCase.toGoalString();
TestAgent agent = new LlmAgent(goal);
```

### Example 3: Create Test Case Programmatically

```java
import ui_adapter.model.TestCase;
import ui_adapter.model.TestStep;

TestCase testCase = new TestCase();
testCase.setKey("MANUAL-001");
testCase.setSummary("Login Test");
testCase.setPreconditions("Valid credentials available");

// Add steps
TestStep step1 = new TestStep(
    "Navigate to login page",
    "URL: https://example.com/login",
    "Login page displayed"
);
testCase.addTestStep(step1);

TestStep step2 = new TestStep(
    "Enter credentials and submit",
    "Username: user@test.com, Password: pass123",
    "User logged in successfully"
);
testCase.addTestStep(step2);

// Convert to goal
String goal = testCase.toGoalString();
```

## Integration with Main.java

The `Main.java` file now includes commented examples showing how to use the Excel loader:

```java
// Load test cases from Excel
String excelFilePath = "path/to/your/testcases.xlsx";
TestCaseLoader loader = new ExcelTestCaseLoader(excelFilePath);
List<TestCase> testCases = loader.loadTestCases();

// Execute each test case
for (TestCase testCase : testCases) {
    String goal = testCase.toGoalString();
    executeTestWithAgent(goal);
}
```

## Goal String Format

The `toGoalString()` method converts a TestCase into a formatted string that LlmAgent can understand:

```
Test Case: [Summary]

Test Case Key: [Key]

Description: [Description]

Preconditions:
[Preconditions]

Test Steps:

Step 1:
  Action: [Step Summary]
  Test Data: [Test Data]
  Expected Result: [Expected Result]

Step 2:
  Action: [Step Summary]
  Test Data: [Test Data]
  Expected Result: [Expected Result]
...
```

## Running the Example

1. **Prepare your Excel file** with test cases in the format shown above

2. **Update the file path** in `ExcelTestCaseExample.java`:
   ```java
   loadAndExecuteAllTestCases("C:/path/to/your/testcases.xlsx");
   ```

3. **Run the example**:
   ```bash
   mvn clean compile
   mvn exec:java -Dexec.mainClass="ui_adapter.ExcelTestCaseExample"
   ```

## Dependencies

The Excel loader requires Apache POI libraries, which are already added to `pom.xml`:

```xml
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
```

## Error Handling

The loader handles common errors:
- **File not found**: Throws `IOException` with clear message
- **Empty Excel file**: Throws `IOException` if no header row
- **Test case not found**: Throws `IOException` when loading by key
- **Missing columns**: Returns null for missing column values

## Best Practices

1. **Unique Keys**: Ensure each test case has a unique Issue Key
2. **Clear Steps**: Write clear, actionable step summaries
3. **Test Data**: Include all necessary test data in the Test Data column
4. **Expected Results**: Define clear, verifiable expected results
5. **Preconditions**: Document all prerequisites clearly

## Sample Excel Structure

Based on your provided format:

```
Row 1 (Headers):
Issue Key | Summary | Description | Precondition | Step Summary | Test Data | Expected Result | ...

Row 2 (Test Case 1):
TEST-001 | Login Test | Verify login works | Valid account | Navigate to login | URL: example.com | Login page shown | ...

Row 3 (Test Case 1, Step 2):
TEST-001 | | | | Enter credentials | user@test.com, pass123 | Success message | ...

Row 4 (Test Case 2):
TEST-002 | Search Test | Verify search | User logged in | Enter search term | keyword: selenium | Results displayed | ...
```

## Troubleshooting

### Issue: Excel file not loading
- Verify file path is correct and uses forward slashes or escaped backslashes
- Ensure file has .xlsx extension
- Check file is not open in Excel

### Issue: Test cases not parsing correctly
- Verify column headers match exactly (case-sensitive)
- Ensure Issue Key column has values
- Check for hidden characters in Excel

### Issue: Test steps not grouping
- Verify multiple rows have the same Issue Key
- Check Issue Key values don't have trailing spaces

## Additional Resources

- See `ExcelTestCaseExample.java` for complete working examples
- Check `Main.java` for integration patterns
- Review `TestCase.java` for all available methods
