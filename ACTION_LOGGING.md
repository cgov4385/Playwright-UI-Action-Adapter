# Action Logging

## Overview

The UI Action Adapter now includes comprehensive action logging functionality that automatically tracks all browser actions performed during test execution. This feature helps with debugging, auditing, and understanding test execution flow.

## Features

- **Automatic Timestamping**: Each log entry includes precise timestamps (yyyy-MM-dd HH:mm:ss.SSS)
- **Action Tracking**: Logs every action before and after execution
- **Result Recording**: Captures success/failure status, messages, and error details
- **Test Case Organization**: Groups actions by test case for easy navigation
- **Security**: Automatically masks sensitive values (ENV: variables)
- **File-based Logging**: Creates timestamped log files in the `action-logs/` directory

## Log File Location

All action logs are saved in the `action-logs/` directory at the project root:

```
action-logs/
├── action-log-2026-02-25_14-30-45.log
├── action-log-2026-02-25_15-15-22.log
└── ...
```

**Note**: The `action-logs/` directory is automatically added to `.gitignore` to prevent committing log files to version control.

## Log File Format

### Header
```
================================================================================
UI Action Adapter - Browser Action Log
Started: 2026-02-25 14:30:45.123
================================================================================
```

### Test Case Start
```
================================================================================
[2026-02-25 14:30:46.000] TEST CASE START: TC001 - Login Test
================================================================================
```

### Action Execution
```
[2026-02-25 14:30:46.100] ACTION START
  Type: NAVIGATE
  Selector: N/A
  Value: https://example.com

[2026-02-25 14:30:47.200] ACTION RESULT
  Type: NAVIGATE
  Status: SUCCESS
  Message: Navigated to https://example.com
--------------------------------------------------------------------------------

[2026-02-25 14:30:47.300] ACTION START
  Type: TYPE
  Selector: #username
  Value: ENV:****** (secret masked)

[2026-02-25 14:30:47.500] ACTION RESULT
  Type: TYPE
  Status: SUCCESS
  Message: Typed '******' into #username
--------------------------------------------------------------------------------
```

### Action Failure
```
[2026-02-25 14:30:48.000] ACTION START
  Type: CLICK
  Selector: #submit-button
  Value: N/A

[2026-02-25 14:30:49.000] ACTION RESULT
  Type: CLICK
  Status: FAILURE
  Message: Element not found
  Error Type: ELEMENT_NOT_FOUND
  Screenshot: screenshots/failure-abc123.png
--------------------------------------------------------------------------------
```

### Test Case End
```
================================================================================
[2026-02-25 14:30:50.000] TEST CASE END: TC001 - Login Test
  Status: PASSED
================================================================================
```

### Footer
```
================================================================================
Log Ended: 2026-02-25 14:30:50.500
================================================================================
```

## Usage

### Single-Agent Mode (Main.java)

Action logging is **automatically enabled** in the default Main.java entry point:

```bash
# Run with default test cases
java ui_adapter.Main

# Run with custom test cases
java ui_adapter.Main --testcase path/to/test_cases.xlsx
```

The log file path will be printed at the end:
```
Action log saved to: action-logs/action-log-2026-02-25_14-30-45.log
```

### Two-Agent Validation Mode (ValidationMain.java)

Action logging is also **automatically enabled** in ValidationMain:

```bash
# Run with default test cases
java ui_adapter.ValidationMain

# Run with custom test cases
java ui_adapter.ValidationMain --testcase path/to/test_cases.xlsx

# Brief reporting mode
java ui_adapter.ValidationMain --testcase test_cases.xlsx --brief
```

### Programmatic Usage

If you're using the adapter programmatically, you can enable logging manually:

```java
import ui_adapter.adapter.UIActionAdapter;
import ui_adapter.executor.ActionLogger;
import ui_adapter.model.Action;
import ui_adapter.model.ActionResult;

// Create adapter
UIActionAdapter adapter = new UIActionAdapter();

// Create and attach logger
ActionLogger logger = new ActionLogger(true); // true = enabled
adapter.setActionLogger(logger);

// Log test case start
logger.logTestCaseStart("My Test Case");

// Execute actions (automatically logged)
Action action = new Action(Action.ActionType.NAVIGATE, null, "https://example.com");
ActionResult result = adapter.execute(action);

// Log test case end
logger.logTestCaseEnd("My Test Case", result.getStatus() == ActionResult.Status.PASS);

// Close logger (writes footer)
logger.close();

// Get log file path
System.out.println("Log saved to: " + logger.getLogFilePath());

// Close adapter
adapter.close();
```

### Disabling Logging

To disable logging, pass `false` to the ActionLogger constructor:

```java
ActionLogger logger = new ActionLogger(false); // Logging disabled
adapter.setActionLogger(logger);
```

## Security Features

### Sensitive Value Masking

The logger automatically masks sensitive values that use the `ENV:` prefix. The system reads these values from environment variables OR from the `application.properties` file:

**Action definition:**
```java
Action action = new Action(Action.ActionType.TYPE, "#password", "ENV:MY_SSO_PASSWORD");
```

**Logged output:**
```
[2026-02-25 14:30:47.300] ACTION START
  Type: TYPE
  Selector: #password
  Value: ENV:****** (secret masked)

[2026-02-25 14:30:47.500] ACTION RESULT
  Type: TYPE
  Status: SUCCESS
  Message: Typed '******' into #password
```

This ensures that secrets loaded from environment variables are never exposed in log files.

## How ENV: Resolution Works

When you use `ENV:MY_SSO_PASSWORD` in your test actions:

1. **First Check**: System looks for `MY_SSO_PASSWORD` in **environment variables**
2. **Second Check**: If not found in environment, system looks in **System Properties** (which includes `application.properties` loaded at startup)
3. **Value Retrieved**: The actual password value (e.g., `BscAI!2Data$27`) is securely retrieved
4. **Logging Protection**: Log files only show `ENV:******` to protect the secret

**Example configuration in `application.properties`:**
```properties
MY_SSO_PASSWORD=BscAI!2Data$27
MY_APP_PASSWORD=BscAI!2Data$27
```

**Example usage in test (LLM Agent will output this):**
```json
{
  "type": "TYPE",
  "selector": {"type": "CSS", "value": "#password"},
  "value": "ENV:MY_SSO_PASSWORD"
}
```

The system automatically resolves `ENV:MY_SSO_PASSWORD` to the actual value from `application.properties` and types it into the password field, while the logs only show masked output.

## ActionLogger API

### Constructor
```java
public ActionLogger(boolean enabled)
```
Creates a logger. If enabled is false, all logging operations are no-ops.

### Methods

#### logActionStart
```java
public void logActionStart(Action action)
```
Logs an action before execution. Records type, selector, and masked value.

#### logActionResult
```java
public void logActionResult(Action action, ActionResult result)
```
Logs an action result after execution. Records status, message, error details, and screenshots.

#### logTestCaseStart
```java
public void logTestCaseStart(String testCaseName)
```
Logs the start of a test case with a prominent header.

#### logTestCaseEnd
```java
public void logTestCaseEnd(String testCaseName, boolean success)
```
Logs the end of a test case with pass/fail status.

#### log
```java
public void log(String message)
```
Logs a general timestamped message.

#### close
```java
public void close()
```
Writes the footer and finalizes the log file. Should be called when testing is complete.

#### getLogFilePath
```java
public String getLogFilePath()
```
Returns the absolute path to the log file, or null if logging is disabled.

#### isEnabled
```java
public boolean isEnabled()
```
Returns true if logging is enabled, false otherwise.

## Integration Points

The ActionLogger is integrated at multiple levels:

1. **ActionExecutor**: Logs each action before and after execution
2. **UIActionAdapter**: Provides `setActionLogger()` method to attach logger
3. **TwoAgentOrchestrator**: Passes logger to adapter for validation mode
4. **Main**: Creates logger and attaches to adapter in single-agent mode
5. **ValidationMain**: Creates logger and attaches to orchestrator in two-agent mode

## Log File Management

### Automatic Cleanup

Log files are **not** automatically deleted. You should implement your own cleanup strategy based on your needs:

```bash
# Delete logs older than 7 days (Windows PowerShell)
Get-ChildItem -Path action-logs -Filter *.log | Where-Object {$_.LastWriteTime -lt (Get-Date).AddDays(-7)} | Remove-Item

# Delete logs older than 7 days (Linux/Mac)
find action-logs -name "*.log" -mtime +7 -delete
```

### Size Considerations

Each log file typically ranges from a few KB to several MB depending on test complexity and duration. Monitor disk space if running large test suites frequently.

## Troubleshooting

### Log file not created

**Cause**: Logger might be disabled or directory creation failed.

**Solution**: 
- Check that `new ActionLogger(true)` is used (not false)
- Ensure write permissions exist for the project directory
- Check console for error messages starting with "Warning: Failed to initialize action logger"

### Incomplete log files

**Cause**: `logger.close()` was not called, usually due to test crash or interruption.

**Solution**: 
- Always call `logger.close()` in a finally block or use try-with-resources pattern
- The log file will still contain all data written before the crash

### Sensitive data in logs

**Cause**: Values not using the `ENV:` prefix.

**Solution**: 
- Always use `ENV:SECRET_NAME` format for sensitive values
- Review test data to ensure proper prefixing
- Never commit action-logs/ directory (it's in .gitignore by default)

## Best Practices

1. ✅ **Always close the logger**: Call `logger.close()` when done to write the footer
2. ✅ **Use ENV: prefix**: Mark all sensitive values with `ENV:` prefix for automatic masking
3. ✅ **One logger per test run**: Create a single logger for the entire test suite, not per action
4. ✅ **Check log file path**: Print or save the log file path for later reference
5. ✅ **Implement cleanup**: Set up automatic log file cleanup for old logs
6. ✅ **Don't commit logs**: Verify `action-logs/` is in `.gitignore`

## Example Output

Here's a complete example of a login test log:

```
================================================================================
UI Action Adapter - Browser Action Log
Started: 2026-02-25 14:30:45.123
================================================================================

[2026-02-25 14:30:45.200] Starting test execution with goal: Login to application

================================================================================
[2026-02-25 14:30:45.250] TEST CASE START: TC001 - User Login Test
================================================================================

[2026-02-25 14:30:45.300] ACTION START
  Type: NAVIGATE
  Selector: N/A
  Value: https://app.example.com/login

[2026-02-25 14:30:46.500] ACTION RESULT
  Type: NAVIGATE
  Status: SUCCESS
  Message: Navigated to https://app.example.com/login
--------------------------------------------------------------------------------

[2026-02-25 14:30:46.600] ACTION START
  Type: TYPE
  Selector: #username
  Value: testuser@example.com

[2026-02-25 14:30:46.800] ACTION RESULT
  Type: TYPE
  Status: SUCCESS
  Message: Typed 'testuser@example.com' into #username
--------------------------------------------------------------------------------

[2026-02-25 14:30:46.900] ACTION START
  Type: TYPE
  Selector: #password
  Value: ENV:****** (secret masked)

[2026-02-25 14:30:47.100] ACTION RESULT
  Type: TYPE
  Status: SUCCESS
  Message: Typed '******' into #password
--------------------------------------------------------------------------------

[2026-02-25 14:30:47.200] ACTION START
  Type: CLICK
  Selector: button[type="submit"]
  Value: N/A

[2026-02-25 14:30:48.500] ACTION RESULT
  Type: CLICK
  Status: SUCCESS
  Message: Clicked element button[type="submit"]
--------------------------------------------------------------------------------

[2026-02-25 14:30:48.600] ACTION START
  Type: WAIT_FOR_VISIBLE
  Selector: .dashboard-header
  Value: N/A

[2026-02-25 14:30:49.200] ACTION RESULT
  Type: WAIT_FOR_VISIBLE
  Status: SUCCESS
  Message: Element .dashboard-header is visible
--------------------------------------------------------------------------------

================================================================================
[2026-02-25 14:30:49.300] TEST CASE END: TC001 - User Login Test
  Status: PASSED
================================================================================

[2026-02-25 14:30:49.400] Test execution completed

================================================================================
Log Ended: 2026-02-25 14:30:49.500
================================================================================
```

## See Also

- [ARCHITECTURE.md](ARCHITECTURE.md) - Overall system architecture
- [TWO_AGENT_USAGE.md](TWO_AGENT_USAGE.md) - Two-agent validation mode documentation
- [EXCEL_TEST_CASE_LOADER_README.md](EXCEL_TEST_CASE_LOADER_README.md) - Test case Excel format
