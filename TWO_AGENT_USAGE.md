# Two-Agent Validation Mode - Usage Guide

## Overview

The Playwright UI Action Adapter now supports **Two-Agent Validation Mode**, where test execution and result validation are handled by separate specialized agents:

1. **Action Agent (LlmAgent)**: Interprets test steps and generates UI actions
2. **Validation Agent**: Validates actual results against expected outcomes using intelligent LLM-based matching

## Running Tests

### Two-Agent Validation Mode (Recommended)

Use `ValidationMain` for tests with validation:

```bash
mvn compile exec:java -Dexec.mainClass="ui_adapter.ValidationMain" -Dexec.args="--testcase src/main/resources/test_cases.xlsx"
```

**Options:**
- `--testcase <path>` or `-testc <path>`: Path to Excel test case file
- `--brief`: Disable detailed validation reports (show summary only)

### Single-Agent Mode (Legacy)

Use `Main` for basic execution without validation:

```bash
mvn compile exec:java -Dexec.mainClass="ui_adapter.Main" -Dexec.args="--testcase src/main/resources/test_cases.xlsx"
```

## How It Works

### Execution Flow

```
1. Load Test Case from Excel
   ↓
2. For Each Test Step:
   
   a) Action Agent reads step summary + test data
      → Generates Action (NAVIGATE, CLICK, TYPE, etc.)
   
   b) UIActionAdapter executes action
      → Captures page state (title + visible text)
      → Returns ActionResult
   
   c) Validation Agent receives:
      - Expected Result (from test step)
      - Actual Result (page state, status, message)
      → Uses LLM to compare
      → Returns ValidationResult (PASS/FAIL/UNKNOWN + reasoning + confidence)
   
   d) Report validation outcome
      → Continue if PASS
      → Stop if FAIL
   
3. Generate test summary
```

### Example Output

```
================================================================================
TWO-AGENT TEST EXECUTION
Test Case: TEST-001 - Login Test
Steps: 2
================================================================================

--------------------------------------------------------------------------------
STEP 1: Navigate to login page
--------------------------------------------------------------------------------
[STEP 001] EXECUTE  type=NAVIGATE  selector=<none>  value=https://app.com/login
[STEP 001] RESULT   status=PASS  durationMs=1234

========== VALIDATION REPORT ==========
Status: PASS
Confidence: 95.0%

Expected Result:
  Page loads

Actual Result:
  Page Title: Login - My Application
  Page Content: Login Email Password Sign In...

Reasoning:
  Page successfully loaded with valid login form content

======================================

--------------------------------------------------------------------------------
STEP 2: Enter credentials and login
--------------------------------------------------------------------------------
[STEP 002] EXECUTE  type=CLICK  selector=CSS:input[name='email']
[STEP 002] RESULT   status=PASS  durationMs=234

[STEP 003] EXECUTE  type=TYPE  selector=CSS:input[name='email']  value=user@test.com
[STEP 003] RESULT   status=PASS  durationMs=156

[STEP 004] EXECUTE  type=CLICK  selector=CSS:button[type='submit']
[STEP 004] RESULT   status=PASS  durationMs=789

========== VALIDATION REPORT ==========
Status: PASS
Confidence: 98.0%

Expected Result:
  Login success

Actual Result:
  Page Title: Dashboard - My Application
  Page Content: Welcome, User! Dashboard...

Reasoning:
  Login successful - user redirected to dashboard with welcome message

======================================

================================================================================
TEST EXECUTION SUMMARY
================================================================================
Test Case: TEST-001
Overall Status: ✅ PASS

Action Execution:
  Total Steps: 4
  Passed: 4
  Failed: 0

Validation Results:
  Total Validations: 2
  Passed: 2
  Failed: 0
  Unknown: 0
  Avg Confidence: 96.5%
================================================================================
```

## Excel Test Case Format

Your Excel file should have these columns:

| Issue Key | Summary | Precondition | Step Summary | Test Data | Expected Result |
|-----------|---------|--------------|--------------|-----------|-----------------|
| TEST-001 | Login Test | Valid user exists | Navigate to login | URL: https://app.com/login | Page loads |
| TEST-001 | | | Enter credentials | email: user@test.com, password: pass123 | Login success |

### Expected Result Column

The **Expected Result** column is where you describe what should happen. The Validation Agent uses LLM to interpret these descriptions intelligently:

**Good Expected Results:**
- "Login success" ✅
- "Error message appears" ✅
- "Page loads" ✅
- "User redirected to dashboard" ✅
- "Form validation error shown" ✅
- "Item added to cart" ✅

**Too Vague (but will still work):**
- "Works" ⚠️
- "OK" ⚠️

**Empty:**
- "" ⚠️ (validation will be skipped for this step)

## Configuration

### Environment Variables

Set these environment variables or add to `application.properties`:

```properties
# LLM Provider (groq or gemini-vertex)
LLM_PROVIDER=gemini-vertex

# For Groq
GROQ_API_KEY=your_groq_api_key
GROQ_MODEL=openai/gpt-oss-safeguard-20b

# For Gemini Vertex
VERTEX_PROJECT=your-gcp-project-id
VERTEX_LOCATION=us-central1
GEMINI_MODEL=gemini-2.5-flash

# Validation Settings
VALIDATION_WIRE_LOG_ENABLED=true  # Show validation agent LLM calls
```

## Validation Features

### Fuzzy Matching

The Validation Agent understands fuzzy/natural language expectations:

| Expected | Actual Page Content | Result |
|----------|---------------------|--------|
| "Login success" | Page shows "Welcome, John!" | ✅ PASS |
| "Error message" | Page shows "Invalid credentials" | ✅ PASS |
| "Page loads" | Page has valid HTML content | ✅ PASS |
| "Login success" | Page shows "Access Denied" | ❌ FAIL |

### Confidence Scores

Each validation includes a confidence score (0.0 to 1.0):
- **0.9-1.0**: Very confident (exact match, clear indicators)
- **0.7-0.9**: Confident (fuzzy match, good evidence)
- **0.5-0.7**: Moderate (partial match, some ambiguity)
- **0.0-0.5**: Low confidence (unclear/ambiguous)

### Reasoning

Every validation includes human-readable reasoning:
```
"Login successful - user redirected to dashboard with welcome message"
"Expected success but found error message 'Invalid credentials'"
```

## Architecture Benefits

### Why Two Agents?

**Before (Single Agent):**
```
LlmAgent → Generate Actions → Execute → Basic PASS/FAIL
```
❌ No intelligent validation  
❌ Can't interpret fuzzy expected results  
❌ Limited error diagnosis  

**After (Two Agents):**
```
Action Agent → Generate Actions → Execute → Capture Page State
                                               ↓
Validation Agent → Intelligent Comparison → Detailed Result
```
✅ Intelligent fuzzy matching  
✅ Clear reasoning for failures  
✅ Confidence scoring  
✅ Better debugging information  

### Separation of Concerns

- **Action Agent**: "What UI actions should I perform?"
- **Validation Agent**: "Did we get the expected result?"

Each agent focuses on one task, making the system more maintainable and accurate.

## Troubleshooting

### Validation Always Returns UNKNOWN

**Cause**: Expected result is too vague or empty  
**Fix**: Provide clearer expected results in Excel

### Validation Confidence Too Low

**Cause**: Expected result doesn't match page content well  
**Fix**: Check if page content actually contains expected indicators

### Page State Not Captured

**Cause**: `setCapturePageState(false)` or executor issue  
**Fix**: Ensure `TwoAgentOrchestrator` enables page state capture (default: enabled)

### LLM API Errors

**Cause**: Missing or invalid API keys  
**Fix**: Check `GROQ_API_KEY` or `VERTEX_PROJECT` environment variables

## Programmatic Usage

You can also use the two-agent orchestrator programmatically:

```java
TestCase testCase = ... // load from Excel or create programmatically

try (TwoAgentOrchestrator orchestrator = new TwoAgentOrchestrator(testCase)) {
    orchestrator.setDetailedReporting(true);
    
    TwoAgentTestResult result = orchestrator.execute();
    
    if (result.isSuccess()) {
        System.out.println("Test passed!");
    } else {
        System.err.println("Test failed!");
        
        // Inspect individual validation results
        for (ValidationResult v : result.getValidationResults()) {
            if (v.getStatus() == ValidationResult.Status.FAIL) {
                System.err.println("Failed: " + v.getReasoning());
            }
        }
    }
}
```

## Migration from Single-Agent

Existing test cases work without modification!

**Single-Agent Mode** (still works):
```bash
mvn exec:java -Dexec.mainClass="ui_adapter.Main"
```

**Two-Agent Mode** (new):
```bash
mvn exec:java -Dexec.mainClass="ui_adapter.ValidationMain"
```

Just add "Expected Result" column to your Excel file to enable validation.

## Performance Considerations

### LLM API Calls

Each validation makes an LLM API call. For large test suites:
- Use efficient models (Gemini Flash is fast)
- Consider batch validation (future enhancement)
- Monitor token usage via `llm-usage.txt`

### Page State Capture

Page state is captured as text content (not full HTML) to reduce token usage:
- First 3000 characters of visible text
- Page title
- Truncated with indicator

---

For questions or issues, check the implementation documentation in `VALIDATION_AGENT_IMPLEMENTATION.md`.
