package ui_adapter.adapter;

import ui_adapter.driver.BrowserDriver;
import ui_adapter.driver.PlaywrightDriver;
import ui_adapter.executor.ActionExecutor;
import ui_adapter.model.Action;
import ui_adapter.model.ActionResult;
import ui_adapter.model.TestRunResult;
import ui_adapter.selector.SelectorResolver;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UIActionAdapter implements AutoCloseable {

    private final BrowserDriver driver;
    private final ActionExecutor executor;

    private long stepCounter = 0;
    private boolean loggingEnabled = true;

    /**
     * Default constructor that creates a Playwright-backed driver.
     * The driver is started immediately so actions can be executed one-by-one.
     */
    public UIActionAdapter() {
        this(new PlaywrightDriver());
    }

    /**
     * Constructor for dependency injection / testing.
     */
    public UIActionAdapter(BrowserDriver driver) {
        this.driver = driver;
        this.driver.start();
        this.executor = new ActionExecutor(this.driver, new SelectorResolver());
    }

    /** Enable/disable adapter-level logging (executor remains unchanged). */
    public void setLoggingEnabled(boolean enabled) {
        this.loggingEnabled = enabled;
    }

    /** Enable/disable page state capture for validation. */
    public void setCapturePageState(boolean capture) {
        this.executor.setCapturePageState(capture);
    }

    /**
     * Execute exactly one action and return its result.
     * No retries, no decision logic.
     */
    public ActionResult execute(Action action) {
        stepCounter++;

        Instant start = Instant.now();
        if (loggingEnabled) {
            System.out.println(formatHeader(stepCounter, action));
        }

        ActionResult result = executor.execute(action);

        if (loggingEnabled) {
            long ms = Duration.between(start, Instant.now()).toMillis();
            System.out.println(formatResult(stepCounter, result, ms));
        }

        return result;
    }

    /**
     * Executes a list of UI actions sequentially.
     * Stops execution on the first failure.
     */
    public TestRunResult runActions(List<Action> actions) {
        if (actions == null || actions.isEmpty()) {
            return new TestRunResult(true, new ArrayList<>(), "No actions to execute");
        }

        List<ActionResult> stepResults = new ArrayList<>();
        boolean overallSuccess = true;
        String errorMessage = null;

        for (Action action : actions) {
            ActionResult result = execute(action);
            stepResults.add(result);

            if (result.getStatus() == ActionResult.Status.FAIL) {
                overallSuccess = false;
                errorMessage = "Execution stopped due to failure at step " + stepResults.size() + ": " + result.getMessage();
                break;
            }
        }

        return new TestRunResult(overallSuccess, stepResults, errorMessage);
    }

    @Override
    public void close() {
        if (loggingEnabled) {
            System.out.println("[ADAPTER] Closing browser driver");
        }
        if (driver != null) {
            driver.close();
        }
    }

    private String formatHeader(long step, Action action) {
        String selector = (action.getSelector() == null) ? "<none>" : action.getSelector().toString();
        String value = (action.getValue() == null) ? "<none>" : action.getValue();
        return String.format("%n[STEP %03d] EXECUTE  type=%s  selector=%s  value=%s", step, action.getType(), selector, value);
    }

    private String formatResult(long step, ActionResult result, long durationMs) {
        String errorType = (result.getErrorType() == null) ? "<none>" : result.getErrorType().name();
        String screenshot = (result.getScreenshotPath() == null) ? "<none>" : result.getScreenshotPath();
        return String.format("[STEP %03d] RESULT   status=%s  errorType=%s  durationMs=%d  screenshot=%s%n           message=%s", step, result.getStatus(), errorType, durationMs, screenshot, result.getMessage());
    }
}
