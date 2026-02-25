package ui_adapter.executor;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.ViewportSize;
import com.microsoft.playwright.options.WaitForSelectorState;
import ui_adapter.driver.BrowserDriver;
import ui_adapter.error.ErrorType;
import ui_adapter.model.Action;
import ui_adapter.model.ActionResult;
import ui_adapter.selector.SelectorResolver;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ActionExecutor {
    private final BrowserDriver driver;
    private final SelectorResolver selectorResolver;

    private static final int AMBIGUOUS_CANDIDATE_LIMIT = 5;

    public ActionExecutor(BrowserDriver driver, SelectorResolver selectorResolver) {
        this.driver = driver;
        this.selectorResolver = selectorResolver;
    }

    private String resolveValue(String value) {
        if (value != null) {
            String trimmed = value.trim();
            if (trimmed.toUpperCase().startsWith("ENV:")) {
                String envKey = trimmed.substring(4).trim();
                String envVal = System.getenv(envKey);
                if (envVal == null || envVal.isEmpty()) {
                    // Fallback: System Property (loaded from application.properties in Main)
                    envVal = System.getProperty(envKey);
                }

                if (envVal != null && !envVal.isEmpty()) {
                    System.out.println("[SECRET] Resolved secret for key: " + envKey);
                    return envVal;
                }
                
                System.err.println("CRITICAL ERROR: Secret '" + envKey + "' not found in Environment or System Properties (application.properties).");
                // Return empty string to prevent typing the placeholder into the UI
                return ""; 
            }
        }
        return value;
    }

    public ActionResult execute(Action action) {
        Locator locator = null;
        try {
            Page page = driver.getPage();
            locator = selectorResolver.resolve(page, action.getSelector());

            switch (action.getType()) {
                case NAVIGATE:
                    String navUrl = resolveValue(action.getValue());
                    if (navUrl == null || navUrl.isEmpty()) {
                        return ActionResult.fail("Navigation URL is missing", ErrorType.NAVIGATION_FAILED, null);
                    }
                    page.navigate(navUrl);
                    return ActionResult.pass("Navigated to " + action.getValue());

                case CLICK:
                    locator.click();
                    return ActionResult.pass("Clicked element " + action.getSelector());

                case TYPE:
                    String textToType = resolveValue(action.getValue());
                    locator.fill(textToType);
                    // Mask the value in the result if it was an ENV var to prevent leakage in logs/LLM history
                    String displayedValue = (action.getValue() != null && action.getValue().startsWith("ENV:")) 
                            ? "******" 
                            : action.getValue();
                    return ActionResult.pass("Typed '" + displayedValue + "' into " + action.getSelector());

                case WAIT:
                    // Static wait/sleep for specified seconds (default: 5 seconds)
                    int waitSeconds = 5; // Default
                    try {
                        if (action.getValue() != null && !action.getValue().isEmpty()) {
                            waitSeconds = Integer.parseInt(action.getValue().trim());
                        }
                    } catch (NumberFormatException e) {
                        // Keep default if parsing fails
                    }
                    
                    // Ensure wait time is reasonable (max 60 seconds)
                    if (waitSeconds < 0) waitSeconds = 5;
                    if (waitSeconds > 60) waitSeconds = 60;
                    
                    Thread.sleep(waitSeconds * 1000L);
                    return ActionResult.pass("Waited for " + waitSeconds + " second(s)");

                case WAIT_FOR_VISIBLE:
                    locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
                    return ActionResult.pass("Element " + action.getSelector() + " is visible");

                case ASSERT_VISIBLE:
                    assertThat(locator).isVisible();
                    return ActionResult.pass("Assertion passed: Element " + action.getSelector() + " is visible");

                case ASSERT_TEXT:
                    assertThat(locator).containsText(action.getValue());
                    return ActionResult.pass("Assertion passed: Element " + action.getSelector() + " contains text '" + action.getValue() + "'");

                case SCREENSHOT:
                    String path = "screenshot-" + UUID.randomUUID() + ".png";
                    page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(path)));
                    return ActionResult.pass("Screenshot taken: " + path);

                case SCROLL:
                    // If selector is present, scroll into view of that element
                    if (action.getSelector() != null) {
                        locator.scrollIntoViewIfNeeded();
                        return ActionResult.pass("Scrolled element " + action.getSelector() + " into view");
                    }
                    // Otherwise, generic scroll down (e.g., 500px)
                    else {
                        page.evalOnSelector("body", "body => window.scrollBy(0, 500)");
                        return ActionResult.pass("Scrolled down 500px");
                    }

                case SWITCH_TAB:
                    try {
                        // The 'value' field can hold the index (0, 1, 2)
                        int index = Integer.parseInt(action.getValue());
                        driver.switchToPage(index);
                        return ActionResult.pass("Switched to tab index " + index);
                    } catch (NumberFormatException e) {
                        // Default to switching to the last opened tab (likely the popup) if no valid index is provided
                        java.util.List<Page> pages = driver.getAllPages();
                        if (pages.size() > 1) {
                            driver.switchToPage(pages.size() - 1);
                            return ActionResult.pass("Switched to latest tab (total tabs: " + pages.size() + ")");
                        } else {
                            return ActionResult.pass("Only one tab open. No switch performed.");
                        }
                    }

                case MAXIMIZE_WINDOW:
                    // Playwright Java doesn't provide true OS-level maximize; use a large viewport to fill most screens.
                    // Use action.value as "width,height" optionally.
                    int w = 1920;
                    int h = 1080;
                    try {
                        if (action.getValue() != null && action.getValue().contains(",")) {
                            String[] parts = action.getValue().split(",");
                            w = Integer.parseInt(parts[0].trim());
                            h = Integer.parseInt(parts[1].trim());
                        }
                    } catch (Exception ignore) {
                        // keep defaults
                    }
                    page.setViewportSize(w, h);
                    return ActionResult.pass("Viewport set to " + w + "x" + h);

                case CLICK_CHECKBOX:
                    // Ensure the checkbox is checked (safe to call multiple times).
                    locator.check();
                    return ActionResult.pass("Checked checkbox " + action.getSelector());

                case CLOSE_TAB:
                    // Close current tab by default. If value is an index, close that tab.
                    Integer indexToClose = null;
                    try {
                        if (action.getValue() != null && !action.getValue().isBlank()) {
                            indexToClose = Integer.parseInt(action.getValue().trim());
                        }
                    } catch (Exception ignore) {
                        indexToClose = null;
                    }

                    java.util.List<Page> pagesBefore = driver.getAllPages();
                    int currentIndex = pagesBefore.indexOf(page);
                    int closeIndex = (indexToClose != null) ? indexToClose : currentIndex;
                    if (closeIndex < 0 || closeIndex >= pagesBefore.size()) {
                        return ActionResult.fail("Invalid tab index to close: " + closeIndex + ". Total tabs: " + pagesBefore.size(), ErrorType.UNKNOWN_ERROR, null);
                    }

                    Page toClose = pagesBefore.get(closeIndex);
                    toClose.close();

                    // Switch to a remaining tab if any
                    java.util.List<Page> pagesAfter = driver.getAllPages();
                    if (!pagesAfter.isEmpty()) {
                        int newIndex = Math.min(closeIndex, pagesAfter.size() - 1);
                        driver.switchToPage(newIndex);
                    }

                    return ActionResult.pass("Closed tab index " + closeIndex + " (tabs now: " + pagesAfter.size() + ")");

                default:
                    return ActionResult.fail("Unsupported action type: " + action.getType(), ErrorType.UNKNOWN_ERROR, null);
            }
        } catch (TimeoutError e) {
            return handleFailure(e, ErrorType.TIMEOUT, "Timeout executing " + action.getType());
        } catch (PlaywrightException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();

            // Basic error mapping
            if (msg.contains("Target closed") || msg.contains("browser has been closed")) {
                return handleFailure(e, ErrorType.UNKNOWN_ERROR, "Browser closed unexpectedly");
            }
            if (msg.contains("NS_ERROR_UNKNOWN_HOST")) {
                return handleFailure(e, ErrorType.NAVIGATION_FAILED, "Navigation failed: Unknown Host");
            }
            if (msg.contains("navigating to")) {
                return handleFailure(e, ErrorType.NAVIGATION_FAILED, "Navigation failed");
            }

            // Strict-mode ambiguity handling: return candidates to help disambiguate.
            if (msg.contains("strict mode violation") || msg.contains("resolved to") || msg.contains("resolved to 2 elements")) {
                String details = buildAmbiguityDetails(locator);
                return handleFailure(e, ErrorType.AMBIGUOUS_SELECTOR,
                        "Ambiguous selector. Found multiple elements." + (details == null ? "" : (" " + details)));
            }

            if (msg.contains("Element is not attached to the DOM")) {
                return handleFailure(e, ErrorType.ELEMENT_NOT_FOUND, "Element not found or detached");
            }
            return handleFailure(e, ErrorType.UNKNOWN_ERROR, msg);
        } catch (AssertionError e) {
            // Playwright assertions throw AssertionError on failure
            return handleFailure(new RuntimeException(e.getMessage()), ErrorType.ASSERTION_FAILED, "Assertion failed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return handleFailure(new RuntimeException(e.getMessage()), ErrorType.UNKNOWN_ERROR, "Wait interrupted");
        } catch (Exception e) {
            return handleFailure(e, ErrorType.UNKNOWN_ERROR, "Unexpected error: " + e.getMessage());
        }
    }

    private String buildAmbiguityDetails(Locator locator) {
        if (locator == null) {
            return null;
        }
        try {
            int count = locator.count();
            int limit = Math.min(count, AMBIGUOUS_CANDIDATE_LIMIT);
            if (count <= 1 || limit <= 0) {
                return null;
            }

            List<String> candidates = new ArrayList<>();
            for (int i = 0; i < limit; i++) {
                Locator nth = locator.nth(i);
                ElementHandle h = nth.elementHandle();
                if (h == null) {
                    continue;
                }

                // Collect a few stable hints
                String tag = safeEval(h, "e => e.tagName.toLowerCase()");
                String text = safeEval(h, "e => (e.innerText || e.textContent || '').trim()");
                if (text != null && text.length() > 120) {
                    text = text.substring(0, 120) + "...";
                }
                String aria = safeEval(h, "e => e.getAttribute('aria-label') || ''");
                String placeholder = safeEval(h, "e => e.getAttribute('placeholder') || ''");
                String testId = safeEval(h, "e => e.getAttribute('data-testid') || e.getAttribute('data-test-id') || ''");

                candidates.add(String.format("#%d tag=%s text='%s' ariaLabel='%s' placeholder='%s' testId='%s'", i,
                        emptyToDash(tag), emptyToDash(text), emptyToDash(aria), emptyToDash(placeholder), emptyToDash(testId)));
            }

            return "Candidates(" + count + "): " + String.join(" | ", candidates) +
                    ". Hint: refine using selector.meta.nearText, selector.meta.role, LABEL/PLACEHOLDER, or choose an index.";
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String safeEval(ElementHandle h, String expression) {
        try {
            Object v = h.evalOnSelector(":scope", expression);
            return v == null ? "" : String.valueOf(v);
        } catch (Exception e) {
            try {
                Object v = h.evaluate(expression);
                return v == null ? "" : String.valueOf(v);
            } catch (Exception ignored) {
                return "";
            }
        }
    }

    private static String emptyToDash(String s) {
        if (s == null) {
            return "-";
        }
        String t = s.trim();
        return t.isEmpty() ? "-" : t;
    }

    private ActionResult handleFailure(Exception e, ErrorType type, String message) {
        String screenshotPath = null;
        try {
            // Attempt screenshot on failure
            screenshotPath = "screenshots/failure-" + UUID.randomUUID() + ".png";
            if (driver != null) {
                driver.getPage().screenshot(new Page.ScreenshotOptions().setPath(Paths.get(screenshotPath)));
            }
        } catch (Exception ignored) {
            // If screenshot fails (e.g. browser closed), we still want to return the original error
            screenshotPath = "screenshot-failed";
        }
        return ActionResult.fail(message + " | Cause: " + e.getMessage(), type, screenshotPath);
    }
}
