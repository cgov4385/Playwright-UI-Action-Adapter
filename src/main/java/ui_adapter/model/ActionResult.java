package ui_adapter.model;

import ui_adapter.error.ErrorType;

public class ActionResult {
    public enum Status {
        PASS,
        FAIL
    }

    private final Status status;
    private final String message;
    private final String screenshotPath;
    private final ErrorType errorType;
    private final String pageState;  // Added: captures page content/state for validation

    public ActionResult(Status status, String message, String screenshotPath, ErrorType errorType) {
        this(status, message, screenshotPath, errorType, null);
    }

    public ActionResult(Status status, String message, String screenshotPath, ErrorType errorType, String pageState) {
        this.status = status;
        this.message = message;
        this.screenshotPath = screenshotPath;
        this.errorType = errorType;
        this.pageState = pageState;
    }

    public static ActionResult pass(String message) {
        return new ActionResult(Status.PASS, message, null, null, null);
    }

    public static ActionResult pass(String message, String pageState) {
        return new ActionResult(Status.PASS, message, null, null, pageState);
    }

    public static ActionResult fail(String message, ErrorType errorType, String screenshotPath) {
        return new ActionResult(Status.FAIL, message, screenshotPath, errorType, null);
    }

    public static ActionResult fail(String message, ErrorType errorType, String screenshotPath, String pageState) {
        return new ActionResult(Status.FAIL, message, screenshotPath, errorType, pageState);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getPageState() {
        return pageState;
    }

    @Override
    public String toString() {
        return "ActionResult{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", screenshotPath='" + screenshotPath + '\'' +
                ", errorType=" + errorType +
                ", pageState=" + (pageState != null ? "<captured>" : "<none>") +
                '}';
    }
}

