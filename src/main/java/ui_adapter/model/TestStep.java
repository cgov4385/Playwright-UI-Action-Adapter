package ui_adapter.model;

/**
 * Represents a single test step in a test case.
 * Contains the step description, test data, and expected result.
 */
public class TestStep {
    private String stepSummary;
    private String testData;
    private String expectedResult;

    public TestStep() {
    }

    public TestStep(String stepSummary, String testData, String expectedResult) {
        this.stepSummary = stepSummary;
        this.testData = testData;
        this.expectedResult = expectedResult;
    }

    public String getStepSummary() {
        return stepSummary;
    }

    public void setStepSummary(String stepSummary) {
        this.stepSummary = stepSummary;
    }

    public String getTestData() {
        return testData;
    }

    public void setTestData(String testData) {
        this.testData = testData;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (stepSummary != null && !stepSummary.isEmpty()) {
            sb.append("Step: ").append(stepSummary).append("\n");
        }
        if (testData != null && !testData.isEmpty()) {
            sb.append("Test Data: ").append(testData).append("\n");
        }
        if (expectedResult != null && !expectedResult.isEmpty()) {
            sb.append("Expected Result: ").append(expectedResult).append("\n");
        }
        return sb.toString();
    }
}
