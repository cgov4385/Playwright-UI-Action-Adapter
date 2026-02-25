package ui_adapter.model;

import java.util.List;

public class TestRunResult {
    private final boolean success;
    private final List<ActionResult> stepResults;
    private final String errorMessage; // Top level error if needed

    public TestRunResult(boolean success, List<ActionResult> stepResults, String errorMessage) {
        this.success = success;
        this.stepResults = stepResults;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<ActionResult> getStepResults() {
        return stepResults;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "TestRunResult{" +
                "success=" + success +
                ", stepResults=" + stepResults +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}

