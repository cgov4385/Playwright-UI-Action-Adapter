# Architecture Diagram

## Two-Agent Architecture Overview

The system uses a **dual-agent approach** for test execution:
- **Action Agent**: Interprets test steps and generates UI actions
- **Validation Agent**: Validates actual results against expected outcomes

This separation ensures clean responsibilities and more intelligent test validation.

## Component Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Excel Test Case System                        │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────┐
│   Excel File     │
│  (.xlsx format)  │
│                  │
│ ┌──────────────┐ │
│ │ Issue Key    │ │
│ │ Summary      │ │
│ │ Precondition │ │
│ │ Step Summary │ │
│ │ Test Data    │ │
│ │ Expected Res │ │
│ └──────────────┘ │
└────────┬─────────┘
         │
         │ reads
         ▼
┌─────────────────────────┐
│ ExcelTestCaseLoader     │
│                         │
│ - loadTestCases()       │
│ - loadTestCase(key)     │
│                         │
│ Uses Apache POI         │
└────────┬────────────────┘
         │
         │ creates
         ▼
┌─────────────────────────┐      ┌──────────────────────┐
│      TestCase           │      │    TestStep          │
│                         │      │                      │
│ - key                   │      │ - stepSummary        │
│ - summary               │◄─────┤ - testData           │
│ - preconditions         │ has  │ - expectedResult ◄───┼─── Used for validation
│ - testSteps[]           │ many │                      │
│ - description           │      └──────────────────────┘
│                         │
│ + toGoalString()        │
└────────┬────────────────┘
         │
         │ converts to
         ▼
┌─────────────────────────┐
│   Formatted Goal        │
│       String            │
│                         │
│ "Test Case: Login...    │
│  Preconditions:...      │
│  Step 1: ...            │
│  Step 2: ..."           │
└────────┬────────────────┘
         │
         │ passed to
         ▼
┌─────────────────────────┐
│   Action Agent          │
│   (LlmAgent)            │
│                         │
│ - Interprets test step  │
│ - Plans UI actions      │
│ - Returns Action list   │
└────────┬────────────────┘
         │
         │ generates
         ▼
┌─────────────────────────┐
│       Action            │
│                         │
│ - type (NAVIGATE,       │
│   CLICK, TYPE, etc.)    │
│ - selector              │
│ - value                 │
└────────┬────────────────┘
         │
         │ executed by
         ▼
┌─────────────────────────┐
│   UIActionAdapter       │
│                         │
│ - Uses Playwright       │
│ - Performs actions      │
│ - Captures page state   │
│ - Returns ActionResult  │
└────────┬────────────────┘
         │
         │ returns
         ▼
┌─────────────────────────┐
│    ActionResult         │
│                         │
│ - status (PASS/FAIL)    │
│ - message               │
│ - screenshotPath        │
│ - pageState (HTML/text) │
└────────┬────────────────┘
         │
         │ + expectedResult
         │
         ▼
┌─────────────────────────┐
│  Validation Agent       │
│                         │
│ - Compares actual vs    │
│   expected              │
│ - Interprets fuzzy      │
│   expectations          │
│ - Returns detailed      │
│   ValidationResult      │
└────────┬────────────────┘
         │
         │ produces
         ▼
┌─────────────────────────┐
│  ValidationResult       │
│                         │
│ - status (PASS/FAIL)    │
│ - reasoning             │
│ - confidence            │
│ - details               │
└─────────────────────────┘
```

## Data Flow Example

```
Step 1: Excel File
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Issue Key | Summary      | Precondition | Step Summary | Test Data    | Expected Result
TEST-001  | Login Test   | Valid creds  | Navigate     | URL: app.com | Page loads
TEST-001  |              |              | Enter creds  | user/pass    | Login success


Step 2: ExcelTestCaseLoader reads and groups
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TestCase {
    key: "TEST-001"
    summary: "Login Test"
    preconditions: "Valid creds"
    testSteps: [
        TestStep {
            stepSummary: "Navigate"
            testData: "URL: app.com"
            expectedResult: "Page loads"
        },
        TestStep {
            stepSummary: "Enter creds"
            testData: "user/pass"
            expectedResult: "Login success"
        }
    ]
}


Step 3: toGoalString() converts to formatted text
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Test Case: Login Test

Test Case Key: TEST-001

Preconditions:
Valid creds

Test Steps:

Step 1:
  Action: Navigate
  Test Data: URL: app.com
  Expected Result: Page loads

Step 2:
  Action: Enter creds
  Test Data: user/pass
  Expected Result: Login success


Step 4: Action Agent interprets and generates actions
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Action { type: NAVIGATE, value: "app.com" }
Action { type: CLICK, selector: "input[name='username']" }
Action { type: TYPE, selector: "input[name='username']", value: "user" }
Action { type: CLICK, selector: "input[name='password']" }
Action { type: TYPE, selector: "input[name='password']", value: "pass" }
Action { type: CLICK, selector: "button[type='submit']" }


Step 5: UIActionAdapter executes with Playwright
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
ActionResult { 
    status: PASS, 
    message: "Navigated to app.com",
    pageState: "<html>...</html>"
}
ActionResult { status: PASS, message: "Clicked username field" }
ActionResult { status: PASS, message: "Typed 'user'" }
ActionResult { status: PASS, message: "Clicked password field" }
ActionResult { status: PASS, message: "Typed password" }
ActionResult { 
    status: PASS, 
    message: "Clicked submit button",
    pageState: "<html><body>Welcome, user!</body></html>"
}


Step 6: Validation Agent validates expected results
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TestStep 1 Validation:
    Expected: "Page loads"
    Actual: pageState contains valid HTML with content
    ValidationResult { 
        status: PASS, 
        reasoning: "Page successfully loaded with valid content",
        confidence: 0.95
    }

TestStep 2 Validation:
    Expected: "Login success"
    Actual: pageState contains "Welcome, user!"
    ValidationResult { 
        status: PASS, 
        reasoning: "Login successful - welcome message displayed",
        confidence: 0.98
    }
```

## Class Relationships

```
┌──────────────────────┐
│  TestCaseLoader      │
│   <<interface>>      │
│                      │
│ + loadTestCases()    │
│ + loadTestCase(key)  │
└──────────▲───────────┘
           │
           │ implements
           │
┌──────────┴───────────┐
│ ExcelTestCaseLoader  │
│                      │
│ - filePath           │
│ + loadTestCases()    │
│ + loadTestCase(key)  │
│ - createColumnMap()  │
│ - getCellValue()     │
│ - isEmptyRow()       │
└──────────────────────┘


┌──────────────────────┐      ┌──────────────────┐
│     TestCase         │      │    TestStep      │
├──────────────────────┤      ├──────────────────┤
│ - key                │      │ - stepSummary    │
│ - summary            │◄────┤│ - testData       │
│ - preconditions      │  1:N ││ - expectedResult │
│ - testSteps[]        │      │└──────────────────┘
│ - description        │      
│ - status             │      
│ - priority           │      
├──────────────────────┤      
│ + toGoalString()     │      
│ + addTestStep()      │      
└──────────────────────┘      
```

## Integration with Existing System

```
┌─────────────────────────────────────────────────────────────┐
│                        Test Execution System                 │
└─────────────────────────────────────────────────────────────┘

   Components
   ┌─────────────┐                 ┌──────────────────┐
   │   Main      │◄────uses────────│ TestCaseLoader   │
   └──────┬──────┘                 └──────────────────┘
          │                                  │
          │ orchestrates                     │ creates
          ▼                                  ▼
   ┌─────────────────────────────────────────────────┐
   │              TestCase (with TestSteps)          │
   └──┬────────────────────────────────────────────┬─┘
      │                                            │
      │ stepSummary + testData                     │ expectedResult
      ▼                                            │
   ┌─────────────┐                                 │
   │ Action      │                                 │
   │ Agent       │                                 │
   └──────┬──────┘                                 │
          │                                        │
          │ generates                              │
          ▼                                        │
   ┌──────────────┐                                │
   │   Action     │                                │
   └──────┬───────┘                                │
          │                                        │
          │ executes                               │
          ▼                                        │
   ┌──────────────────┐                            │
   │ UIActionAdapter  │                            │
   └──────┬───────────┘                            │
          │                                        │
   Two-Agent Execution Flow

```
┌──────────────┐
│  TestStep    │
│              │
│ - summary    │
│ - testData   │
│ - expected   │
└──────┬───────┘
       │
       ├─────────────────────────────────────────┐
       │                                         │
       │ Step Summary + Test Data                │ Expected Result
       │                                         │
       ▼                                         │
┌──────────────┐                                 │
│ Action Agent │                                 │
│              │                                 │
│ "What UI     │                                 │
│  actions     │                                 │
│  to perform?"│                                 │
└──────┬───────┘                                 │
       │                                         │
       │ Generates                               │
       ▼                                         │
┌──────────────┐                                 │
│ Action List  │                                 │
└──────┬───────┘                                 │
       │                                         │
       │ Execute                                 │
       ▼                                         │
┌──────────────┐                                 │
│ UI Adapter   │                                 │
└──────┬───────┘                                 │
       │                                         │
       │ Captures State                          │
       ▼                                         │
┌──────────────┐                                 │
│ActionResult  │                                 │
│+ page state  │                                 │
└──────┬───────┘                                 │
       │                                         │
       └──────────────┐                          │
                      │                          │
                      ▼                          ▼
                ┌────────────────────────────────┐
                │     Validation Agent           │
                │                                │
                │ "Does actual match expected?"  │
                │                                │
                │ - Page content analysis        │
                │ - Text matching                │
                │ - Element state checking       │
                │ - Fuzzy interpretation         │
                └────────┬───────────────────────┘
                         │
                         ▼
                ┌────────────────────┐
                │ ValidationResult   │
## Key Architectural Benefits

### Separation of Concerns
- **Action Agent**: Focuses solely on "HOW" to execute test steps
- **Validation Agent**: Focuses solely on "DID IT WORK" verification
- Each agent has a single, clear responsibility

### Intelligent Validation
Instead of simple string matching, the Validation Agent can:
- Handle fuzzy expectations: "Login success" matches various success indicators
- Check multiple criteria: text content, element state, page structure
- Provide reasoning: explain WHY a test passed or failed
- Calculate confidence: how certain is the validation?

### Enhanced Error Reporting
```
Traditional: FAIL - "Login successful" not found
With Validation Agent: 
  FAIL - Expected login success
  Reasoning: Found error message "Invalid credentials" instead
  Confidence: 0.98
  Suggestion: Check test data credentials
```

### Flexibility
This architecture makes it easy to:
- Add new test case sources (JSON, CSV, Database)
- Modify goal string format
- Extend test case metadata
- Integrate with different execution engines
- Swap out Action Agent strategies (rule-based, LLM-based)
- Customize validation logic per test type
- Add new validation criteria without changing execution logic
```

## Agent Responsibilities

### Action Agent
**Purpose**: Translate test step descriptions into concrete UI actions

**Input**:
- Step Summary: "Navigate to login page"
- Test Data: "URL: https://app.com/login"

**Output**:
- `Action { type: NAVIGATE, value: "https://app.com/login" }`

**Responsibilities**:
- Interpret natural language test instructions
- Identify appropriate selectors
- Generate action sequences
- Handle dynamic test data

### Validation Agent
**Purpose**: Intelligently validate actual outcomes against expected results

**Input**:
- Expected Result: "Login success"
- Action Result: Page state, DOM, messages
- Context: Test step information

**Output**:
- `ValidationResult { status: PASS, reasoning: "...", confidence: 0.95 }`

**Responsibilities**:
- Interpret fuzzy expected results ("success", "error", "page loads")
- Compare page state with expectations
- Check for text, elements, conditions
- Provide detailed pass/fail reasoning
- Calculate confidence scores
- Handle partial matches and edge cases

## Usage Patterns

### Pattern 1: Batch Execution with Dual Agents
```
Excel File → Load All → Loop → 
  For Each Step:
    Action Agent → Generate Actions → Execute → 
    Validation Agent → Validate Result → 
  Collect Results
```

### Pattern 2: Selective Execution
```
Excel File → Load by Key → 
  For Each Step:
    Action Agent → Execute →
    Validation Agent → Validate
```

### Pattern 3: Step-by-Step Validation
```
TestStep → Action Agent → UIAdapter → Validation Agent → Report
                        │ ValidationResult │
                        └──────────────────┘
```

## Usage Patterns

### Pattern 1: Batch Execution
```
Excel File → Load All → Loop → Execute Each → Collect Results
```

### Pattern 2: Selective Execution
```
Excel File → Load by Key → Execute One → Return Result
```

### Pattern 3: Programmatic Creation
```
Create TestCase → Add Steps → Convert to Goal → Execute
```

## Error Handling Flow

```
┌─────────────────┐
│ Excel File      │
└────────┬────────┘
         │
         ▼
    ┌────────┐    File not found?
    │ Exists?├───────────────────► IOException
    └───┬────┘
        │ Yes
        ▼
┌────────────────┐
│ Read Headers   │
└────────┬───────┘
         │
         ▼
    ┌──────────┐  No headers?
    │ Headers? ├─────────────────► IOException
    └────┬─────┘
         │ Yes
         ▼
┌────────────────┐
│ Parse Rows     │
└────────┬───────┘
         │
         ▼
    ┌──────────┐  Missing columns?
    │ Validate?├─────────────────► Return null for field
    └────┬─────┘
         │ Valid
         ▼
┌────────────────┐
│ Create Objects │
└────────┬───────┘
         │
         ▼
┌────────────────┐
│ Return Results │
└────────────────┘
```

This architecture provides a clean separation of concerns and makes it easy to:
- Add new test case sources (JSON, CSV, Database)
- Modify goal string format
- Extend test case metadata
- Integrate with different execution engines
