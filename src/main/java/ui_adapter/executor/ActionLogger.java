package ui_adapter.executor;

import ui_adapter.model.Action;
import ui_adapter.model.ActionResult;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logger for browser actions performed by the UI Action Adapter.
 * Logs all actions and their results to a timestamped log file.
 */
public class ActionLogger {
    private static final String LOG_DIR = "action-logs";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    private final String logFilePath;
    private final boolean enabled;

    /**
     * Creates a new ActionLogger.
     * @param enabled Whether logging is enabled
     */
    public ActionLogger(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            try {
                // Create log directory if it doesn't exist
                Path logDir = Paths.get(LOG_DIR);
                if (!Files.exists(logDir)) {
                    Files.createDirectories(logDir);
                }
                
                // Create timestamped log file
                String timestamp = FILE_DATE_FORMAT.format(new Date());
                this.logFilePath = LOG_DIR + "/action-log-" + timestamp + ".log";
                
                // Write header
                writeToFile("=" + "=".repeat(79));
                writeToFile("UI Action Adapter - Browser Action Log");
                writeToFile("Started: " + DATE_FORMAT.format(new Date()));
                writeToFile("=" + "=".repeat(79));
                writeToFile("");
            } catch (IOException e) {
                System.err.println("Warning: Failed to initialize action logger: " + e.getMessage());
                throw new RuntimeException("Failed to initialize action logger", e);
            }
        } else {
            this.logFilePath = null;
        }
    }

    /**
     * Logs an action before execution.
     * @param action The action to be executed
     */
    public void logActionStart(Action action) {
        if (!enabled) return;
        
        try {
            String timestamp = DATE_FORMAT.format(new Date());
            writeToFile("[" + timestamp + "] ACTION START");
            writeToFile("  Type: " + action.getType());
            writeToFile("  Selector: " + (action.getSelector() != null ? action.getSelector() : "N/A"));
            
            // Mask sensitive values (ENV variables)
            String valueDisplay = action.getValue();
            if (valueDisplay != null && valueDisplay.toUpperCase().trim().startsWith("ENV:")) {
                valueDisplay = "ENV:****** (secret masked)";
            }
            writeToFile("  Value: " + (valueDisplay != null ? valueDisplay : "N/A"));
            writeToFile("");
        } catch (IOException e) {
            System.err.println("Warning: Failed to log action start: " + e.getMessage());
        }
    }

    /**
     * Logs an action result after execution.
     * @param action The action that was executed
     * @param result The result of the execution
     */
    public void logActionResult(Action action, ActionResult result) {
        if (!enabled) return;
        
        try {
            String timestamp = DATE_FORMAT.format(new Date());
            writeToFile("[" + timestamp + "] ACTION RESULT");
            writeToFile("  Type: " + action.getType());
            writeToFile("  Status: " + (result.getStatus() == ActionResult.Status.PASS ? "SUCCESS" : "FAILURE"));
            writeToFile("  Message: " + result.getMessage());
            
            if (result.getStatus() == ActionResult.Status.FAIL) {
                writeToFile("  Error Type: " + (result.getErrorType() != null ? result.getErrorType() : "N/A"));
                writeToFile("  Screenshot: " + (result.getScreenshotPath() != null ? result.getScreenshotPath() : "N/A"));
            }
            
            writeToFile("-".repeat(80));
            writeToFile("");
        } catch (IOException e) {
            System.err.println("Warning: Failed to log action result: " + e.getMessage());
        }
    }

    /**
     * Logs a test case start.
     * @param testCaseName The name of the test case
     */
    public void logTestCaseStart(String testCaseName) {
        if (!enabled) return;
        
        try {
            String timestamp = DATE_FORMAT.format(new Date());
            writeToFile("");
            writeToFile("=" + "=".repeat(79));
            writeToFile("[" + timestamp + "] TEST CASE START: " + testCaseName);
            writeToFile("=" + "=".repeat(79));
            writeToFile("");
        } catch (IOException e) {
            System.err.println("Warning: Failed to log test case start: " + e.getMessage());
        }
    }

    /**
     * Logs a test case end.
     * @param testCaseName The name of the test case
     * @param success Whether the test case succeeded
     */
    public void logTestCaseEnd(String testCaseName, boolean success) {
        if (!enabled) return;
        
        try {
            String timestamp = DATE_FORMAT.format(new Date());
            writeToFile("");
            writeToFile("=" + "=".repeat(79));
            writeToFile("[" + timestamp + "] TEST CASE END: " + testCaseName);
            writeToFile("  Status: " + (success ? "PASSED" : "FAILED"));
            writeToFile("=" + "=".repeat(79));
            writeToFile("");
        } catch (IOException e) {
            System.err.println("Warning: Failed to log test case end: " + e.getMessage());
        }
    }

    /**
     * Logs a general message.
     * @param message The message to log
     */
    public void log(String message) {
        if (!enabled) return;
        
        try {
            String timestamp = DATE_FORMAT.format(new Date());
            writeToFile("[" + timestamp + "] " + message);
        } catch (IOException e) {
            System.err.println("Warning: Failed to log message: " + e.getMessage());
        }
    }

    /**
     * Closes the logger and writes footer.
     */
    public void close() {
        if (!enabled) return;
        
        try {
            writeToFile("");
            writeToFile("=" + "=".repeat(79));
            writeToFile("Log Ended: " + DATE_FORMAT.format(new Date()));
            writeToFile("=" + "=".repeat(79));
        } catch (IOException e) {
            System.err.println("Warning: Failed to close action logger: " + e.getMessage());
        }
    }

    /**
     * Writes a line to the log file.
     */
    private void writeToFile(String line) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(line);
            writer.newLine();
        }
    }

    /**
     * Gets the log file path.
     * @return The path to the log file, or null if logging is disabled
     */
    public String getLogFilePath() {
        return logFilePath;
    }

    /**
     * Checks if logging is enabled.
     * @return true if logging is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
}
