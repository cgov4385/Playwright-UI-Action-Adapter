package ui_adapter.model;

/**
 * Represents the result of validation performed by the Validation Agent.
 * Contains detailed information about whether the actual result matches the expected result.
 */
public class ValidationResult {
    public enum Status {
        PASS,    // Actual matches expected
        FAIL,    // Actual does not match expected
        UNKNOWN  // Unable to determine (e.g., insufficient information)
    }

    private final Status status;
    private final String reasoning;
    private final double confidence;
    private final String details;
    private final String expectedResult;
    private final String actualResult;

    public ValidationResult(Status status, String reasoning, double confidence, String details, 
                           String expectedResult, String actualResult) {
        this.status = status;
        this.reasoning = reasoning;
        this.confidence = confidence;
        this.details = details;
        this.expectedResult = expectedResult;
        this.actualResult = actualResult;
    }

    /**
     * Creates a PASS validation result.
     */
    public static ValidationResult pass(String reasoning, double confidence, String expectedResult, String actualResult) {
        return new ValidationResult(Status.PASS, reasoning, confidence, null, expectedResult, actualResult);
    }

    /**
     * Creates a FAIL validation result.
     */
    public static ValidationResult fail(String reasoning, double confidence, String details, 
                                       String expectedResult, String actualResult) {
        return new ValidationResult(Status.FAIL, reasoning, confidence, details, expectedResult, actualResult);
    }

    /**
     * Creates an UNKNOWN validation result.
     */
    public static ValidationResult unknown(String reasoning, String expectedResult, String actualResult) {
        return new ValidationResult(Status.UNKNOWN, reasoning, 0.0, null, expectedResult, actualResult);
    }

    public Status getStatus() {
        return status;
    }

    public String getReasoning() {
        return reasoning;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getDetails() {
        return details;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public String getActualResult() {
        return actualResult;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationResult{");
        sb.append("status=").append(status);
        sb.append(", confidence=").append(String.format("%.2f", confidence));
        sb.append(", reasoning='").append(reasoning).append('\'');
        
        if (details != null && !details.isEmpty()) {
            sb.append(", details='").append(details).append('\'');
        }
        
        sb.append(", expected='").append(expectedResult).append('\'');
        sb.append(", actual='").append(truncate(actualResult, 100)).append('\'');
        sb.append('}');
        
        return sb.toString();
    }

    /**
     * Formats a detailed, human-readable report of the validation.
     */
    public String toDetailedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== VALIDATION REPORT ==========\n");
        sb.append("Status: ").append(status).append("\n");
        sb.append("Confidence: ").append(String.format("%.1f%%", confidence * 100)).append("\n");
        sb.append("\nExpected Result:\n  ").append(expectedResult).append("\n");
        sb.append("\nActual Result:\n  ").append(truncate(actualResult, 500)).append("\n");
        sb.append("\nReasoning:\n  ").append(reasoning).append("\n");
        
        if (details != null && !details.isEmpty()) {
            sb.append("\nAdditional Details:\n  ").append(details).append("\n");
        }
        
        sb.append("======================================\n");
        return sb.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "<null>";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... (truncated)";
    }
}
