# Two-Agent Orchestrator Architecture

## Overview

The Two-Agent Orchestrator implements a separation of concerns pattern where **action execution** and **result validation** are handled by two independent LLM-powered agents working in coordination.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    TwoAgentOrchestrator                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐         ┌──────────────┐                      │
│  │ ActionAgent  │         │ Validation   │                      │
│  │              │         │    Agent     │                      │
│  │ (Generates   │         │ (Validates   │                      │
│  │  Actions)    │         │  Results)    │                      │
│  └──────────────┘         └──────────────┘                      │
│         │                        │                               │
│         │                        │                               │
│         ▼                        ▼                               │
│  ┌──────────────────────────────────────┐                       │
│  │      UIActionAdapter                 │                       │
│  │   (Executes Browser Actions)         │                       │
│  └──────────────────────────────────────┘                       │
│                   │                                              │
│                   ▼                                              │
│           ┌──────────────┐                                       │
│           │  Playwright  │                                       │
│           │   Browser    │                                       │
│           └──────────────┘                                       │
└─────────────────────────────────────────────────────────────────┘
```

## The Two Agents

### 1. **ActionAgent** (formerly LlmAgent)

**Role:** Decides WHAT actions to perform

**Responsibilities:**
- Interprets natural language test goals and steps
- Analyzes current page state and action history
- Determines the next UI action to execute
- Makes decisions about when the test is complete

**Input:**
- Test goal (e.g., "Login with valid credentials")
- Test steps from test case
- Previous action results (success/failure)
- Current page state

**Output:**
- `Action` objects with:
  - Action type (CLICK, TYPE, NAVIGATE, WAIT, etc.)
  - Selector information (what element to interact with)
  - Input values (for TYPE actions)
  - Expected behavior

**Implementation:** Uses LLM (Gemini Vertex or Groq) to intelligently decide actions

### 2. **ValidationAgent**

**Role:** Decides IF results match expectations

**Responsibilities:**
- Validates actual results against expected outcomes
- Performs fuzzy matching on natural language expectations
- Provides confidence scores (0.0 - 1.0)
- Generates detailed reasoning for validation decisions

**Input:**
- Expected result (from test step)
- Actual result (from action execution)
- Page state captured during action
- Step context information

**Output:**
- `ValidationResult` with:
  - Status: PASS, FAIL, or UNKNOWN
  - Confidence score (0.0 - 1.0)
  - Detailed reasoning explaining the decision
  - Validation timestamp

**Implementation:** Uses LLM for intelligent fuzzy matching of expectations

## Execution Flow

The orchestrator coordinates a three-phase cycle for each test step:

```
┌─────────────────────────────────────────────────────────────────┐
│                      Test Step Execution                         │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
        ┌──────────────────────────────────────────┐
        │  Phase 1: Action Generation              │
        │  ┌────────────────────────────────────┐  │
        │  │  ActionAgent.nextAction()          │  │
        │  │  - Analyze test goal & history     │  │
        │  │  - Determine next action           │  │
        │  │  - Return Action object            │  │
        │  └────────────────────────────────────┘  │
        └──────────────────────────────────────────┘
                               │
                               ▼
        ┌──────────────────────────────────────────┐
        │  Phase 2: Action Execution               │
        │  ┌────────────────────────────────────┐  │
        │  │  UIActionAdapter.execute()         │  │
        │  │  - Execute browser action          │  │
        │  │  - Capture page state              │  │
        │  │  - Return ActionResult             │  │
        │  └────────────────────────────────────┘  │
        └──────────────────────────────────────────┘
                               │
                               ▼
        ┌──────────────────────────────────────────┐
        │  Phase 3: Result Validation              │
        │  ┌────────────────────────────────────┐  │
        │  │  ValidationAgent.validate()        │  │
        │  │  - Compare expected vs actual      │  │
        │  │  - Fuzzy match with LLM            │  │
        │  │  - Return ValidationResult         │  │
        │  └────────────────────────────────────┘  │
        └──────────────────────────────────────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │  Continue to next   │
                    │  step or complete   │
                    └─────────────────────┘
```

## Detailed Step-by-Step Process

### 1. **Initialization**

```java
TwoAgentOrchestrator orchestrator = new TwoAgentOrchestrator(testCase);
orchestrator.setDetailedReporting(true);
orchestrator.setActionLogger(logger);
```

The orchestrator creates:
- ActionAgent with test goal
- ValidationAgent for result validation
- UIActionAdapter for browser control

### 2. **Test Execution Loop**

```java
while (!actionAgent.isTestComplete()) {
    // Phase 1: Get next action
    Action action = actionAgent.nextAction(lastActionResult);
    
    // Phase 2: Execute action
    ActionResult actionResult = adapter.execute(action);
    
    // Phase 3: Validate result
    ValidationResult validation = validationAgent.validate(
        expectedResult, 
        actionResult, 
        stepContext
    );
    
    // Check validation status
    if (validation.getStatus() == FAIL) {
        break; // Stop on validation failure
    }
}
```

### 3. **Action Generation (ActionAgent)**

The ActionAgent uses LLM to analyze:
- **Test Goal:** What is the overall objective?
- **Action History:** What has been done so far?
- **Last Result:** Did the previous action succeed?
- **Page State:** What elements are available?

Example Decision Process:
```
Goal: "Login with valid credentials"
Current Step: "Enter username"
Page State: [input with placeholder "Username", button "Login"]

ActionAgent Decision:
→ Action: TYPE
→ Selector: PLACEHOLDER='Username'
→ Value: 'admin'
→ Reasoning: "Found username input field, typing credentials"
```

### 4. **Action Execution (UIActionAdapter)**

The adapter executes the browser action:
- Resolves selectors (PLACEHOLDER, LABEL, ROLE, TEXT, CSS, XPATH)
- Performs action via Playwright
- Captures page state after action
- Returns ActionResult with status and details

### 5. **Result Validation (ValidationAgent)**

The ValidationAgent uses LLM to intelligently compare:
- **Expected Result:** "User should be logged in"
- **Actual Result:** "Navigated to /dashboard, found Welcome message"
- **Page State:** Current page content

Example Validation:
```
Expected: "User should be logged in"
Actual: "Page navigated to /dashboard, element 'Welcome, Admin' visible"

ValidationAgent Decision:
→ Status: PASS
→ Confidence: 0.95
→ Reasoning: "Dashboard navigation and welcome message indicate successful login"
```

### 6. **Result Aggregation**

After all steps complete, the orchestrator generates:
- **TwoAgentTestResult** containing:
  - Overall pass/fail status
  - All action results
  - All validation results
  - Statistics (passed/failed actions and validations)
  - Average validation confidence score

## Why Two Agents?

### Separation of Concerns

**Single-Agent Approach:**
```
LLM → "Click login button and verify success"
↓
Did action succeed? ✅ or ❌
```
Problem: Simple binary pass/fail, no nuanced validation

**Two-Agent Approach:**
```
ActionAgent → "Click the login button"
↓
Execute action
↓
ValidationAgent → "Does the result match 'User logged in'?"
↓
PASS (95% confidence) with reasoning
```
Benefit: Intelligent validation with confidence scoring

### Benefits

1. **Intelligent Validation**
   - Fuzzy matching of natural language expectations
   - Confidence scoring (not just binary pass/fail)
   - Detailed reasoning for debugging

2. **Better Debugging**
   - Separate visibility into action decisions vs validation
   - Clear distinction between execution failures and expectation mismatches
   - Confidence scores help identify flaky tests

3. **Flexibility**
   - Can skip validation for certain steps
   - Can adjust validation strictness per test
   - Can use different LLMs for each agent

4. **Clearer Architecture**
   - ActionAgent focuses on "what to do"
   - ValidationAgent focuses on "did it work"
   - Each agent has single responsibility

## Usage Example

### Basic Usage

```java
// Load test case from Excel
TestCase testCase = loader.loadTestCase("TC-001");

// Create orchestrator
TwoAgentOrchestrator orchestrator = new TwoAgentOrchestrator(testCase);
orchestrator.setDetailedReporting(true);

// Create and attach logger
ActionLogger logger = new ActionLogger(true);
orchestrator.setActionLogger(logger);

// Execute test
TwoAgentTestResult result = orchestrator.execute();

// Check results
if (result.isSuccess()) {
    System.out.println("✅ Test PASSED");
} else {
    System.out.println("❌ Test FAILED");
    System.out.println(result.getSummary());
}

// Cleanup
orchestrator.close();
logger.close();
```

### Output Example

```
================================================================================
TWO-AGENT TEST EXECUTION
Test Case: TC-001 - Login with valid credentials
Steps: 3
================================================================================

--------------------------------------------------------------------------------
STEP 1: Navigate to login page
--------------------------------------------------------------------------------
[ACTION] NAVIGATE to https://example.com/login
[RESULT] ✅ PASS - Page loaded successfully
[VALIDATION] Skipped (no expected result defined)

--------------------------------------------------------------------------------
STEP 2: Enter username and password
--------------------------------------------------------------------------------
[ACTION] TYPE into input[placeholder='Username']
[RESULT] ✅ PASS - Text entered successfully
[VALIDATION] ✅ PASS (confidence: 98%)
Expected: "Credentials entered"
Actual: "Text 'admin' typed into username field"
Reasoning: "Username field successfully populated with expected value"

--------------------------------------------------------------------------------
STEP 3: Click login button
--------------------------------------------------------------------------------
[ACTION] CLICK on button with text 'Login'
[RESULT] ✅ PASS - Element clicked successfully
[VALIDATION] ✅ PASS (confidence: 95%)
Expected: "User should be logged in"
Actual: "Navigation to /dashboard, Welcome message visible"
Reasoning: "Dashboard navigation and welcome message indicate successful login"

================================================================================
TEST EXECUTION SUMMARY
================================================================================
Test Case: TC-001
Overall Status: ✅ PASS

Action Execution:
  Total Steps: 3
  Passed: 3
  Failed: 0

Validation Results:
  Total Validations: 2
  Passed: 2
  Failed: 0
  Unknown: 0
  Avg Confidence: 96.5%
================================================================================
```

## Configuration Options

### Detailed Reporting

```java
orchestrator.setDetailedReporting(true);  // Full validation details
orchestrator.setDetailedReporting(false); // Compact output
```

### Action Logging

```java
ActionLogger logger = new ActionLogger(true); // Auto-generate log file
orchestrator.setActionLogger(logger);
```

Logs all browser actions to timestamped file:
- `action-logs/action-log-2026-02-26_14-30-45.log`

### Validation Failure Behavior

By default, the orchestrator **stops** on validation failure:
```java
if (validation.getStatus() == ValidationResult.Status.FAIL) {
    System.err.println("⚠️  VALIDATION FAILED - Stopping test execution");
    break;
}
```

This can be customized in the orchestrator implementation.

## Comparison: Single-Agent vs Two-Agent

| Aspect | Single-Agent (Main.java) | Two-Agent (ValidationMain.java) |
|--------|--------------------------|----------------------------------|
| **Action Generation** | ActionAgent only | ActionAgent only |
| **Execution** | UIActionAdapter | UIActionAdapter |
| **Validation** | Simple PASS/FAIL from action | LLM-powered fuzzy matching |
| **Confidence Scoring** | ❌ No | ✅ Yes (0.0 - 1.0) |
| **Reasoning** | ❌ No | ✅ Detailed explanation |
| **Best For** | Quick smoke tests | Comprehensive validation |
| **LLM Calls** | 1 per step | 2 per step (action + validation) |
| **Complexity** | Simpler | More sophisticated |

## When to Use Which Mode?

### Use **Single-Agent Mode** when:
- Running quick smoke tests
- Only care about action execution success
- Cost/speed is priority (fewer LLM calls)
- Binary pass/fail is sufficient

### Use **Two-Agent Mode** when:
- Need intelligent result validation
- Testing complex workflows with nuanced expectations
- Want confidence scores and reasoning
- Debugging test failures
- Need detailed reporting

## Technical Implementation

### Core Classes

1. **TwoAgentOrchestrator.java**
   - Coordinates both agents
   - Manages execution flow
   - Aggregates results

2. **ActionAgent.java** (formerly LlmAgent)
   - Implements `TestAgent` interface
   - Uses LLM to generate actions
   - Maintains action history

3. **ValidationAgent.java**
   - Performs LLM-powered validation
   - Returns ValidationResult with confidence
   - Provides detailed reasoning

4. **UIActionAdapter.java**
   - Executes browser actions via Playwright
   - Captures page state for validation
   - Returns ActionResult

### Key Methods

**TwoAgentOrchestrator:**
```java
public TwoAgentTestResult execute()  // Main execution loop
public void close()                   // Cleanup resources
```

**ActionAgent:**
```java
public Action nextAction(ActionResult previousResult)  // Get next action
public boolean isTestComplete()                        // Check if done
public void onTestEnd(List<ActionResult> results)     // Cleanup
```

**ValidationAgent:**
```java
public ValidationResult validate(
    String expectedResult,
    ActionResult actualResult,
    String stepContext
)
```

## See Also

- [ACTION_LOGGING.md](ACTION_LOGGING.md) - Action logging documentation
- [VALIDATION_AGENT_IMPLEMENTATION.md](VALIDATION_AGENT_IMPLEMENTATION.md) - Validation agent details
- [TWO_AGENT_USAGE.md](TWO_AGENT_USAGE.md) - Usage examples
- [ARCHITECTURE.md](ARCHITECTURE.md) - Overall system architecture

## Entry Points

- **Single-Agent Mode:** `Main.java`
- **Two-Agent Mode:** `ValidationMain.java`

Both modes share the same ActionAgent for action generation, but differ in validation approach.
