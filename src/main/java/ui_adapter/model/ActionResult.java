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

    public ActionResult(Status status, String message, String screenshotPath, ErrorType errorType) {
        this.status = status;
        this.message = message;
        this.screenshotPath = screenshotPath;
        this.errorType = errorType;
    }

    public static ActionResult pass(String message) {
        return new ActionResult(Status.PASS, message, null, null);
    }

    public static ActionResult fail(String message, ErrorType errorType, String screenshotPath) {
        return new ActionResult(Status.FAIL, message, screenshotPath, errorType);
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

    @Override
    public String toString() {
        return "ActionResult{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", screenshotPath='" + screenshotPath + '\'' +
                ", errorType=" + errorType +
                '}';
    }
}

