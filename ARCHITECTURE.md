# Architecture Diagram

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
┌─────────────────────────┐      ┌──────────────────┐
│      TestCase           │      │    TestStep      │
│                         │      │                  │
│ - key                   │      │ - stepSummary    │
│ - summary               │◄─────┤ - testData       │
│ - preconditions         │ has  │ - expectedResult │
│ - testSteps[]           │ many │                  │
│ - description           │      └──────────────────┘
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
│      LlmAgent           │
│                         │
│ - Interprets goal       │
│ - Plans actions         │
│ - Returns Action        │
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


Step 4: LlmAgent interprets and generates actions
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Action { type: NAVIGATE, value: "app.com" }
Action { type: CLICK, selector: "input[name='username']" }
Action { type: TYPE, selector: "input[name='username']", value: "user" }
Action { type: CLICK, selector: "input[name='password']" }
Action { type: TYPE, selector: "input[name='password']", value: "pass" }
Action { type: CLICK, selector: "button[type='submit']" }


Step 5: UIActionAdapter executes with Playwright
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
ActionResult { status: PASS, message: "Navigated to app.com" }
ActionResult { status: PASS, message: "Clicked username field" }
ActionResult { status: PASS, message: "Typed 'user'" }
ActionResult { status: PASS, message: "Clicked password field" }
ActionResult { status: PASS, message: "Typed password" }
ActionResult { status: PASS, message: "Login successful" }
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
│                        Your System                          │
└─────────────────────────────────────────────────────────────┘

   Existing Components              New Components
   ┌─────────────┐                 ┌──────────────────┐
   │   Main      │◄────uses────────│ TestCaseLoader   │
   └──────┬──────┘                 └──────────────────┘
          │                                  │
          │ creates                          │ creates
          ▼                                  ▼
   ┌─────────────┐                 ┌──────────────────┐
   │  LlmAgent   │◄────goal────────│   TestCase       │
   └──────┬──────┘                 └──────────────────┘
          │                                  │
          │ generates                        │ contains
          ▼                                  ▼
   ┌──────────────┐                ┌──────────────────┐
   │   Action     │                │    TestStep      │
   └──────┬───────┘                └──────────────────┘
          │
          │ executes
          ▼
   ┌──────────────────┐
   │ UIActionAdapter  │
   └──────┬───────────┘
          │
          │ returns
          ▼
   ┌──────────────────┐
   │  ActionResult    │
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
