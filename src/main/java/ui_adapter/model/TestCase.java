package ui_adapter.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a test case loaded from an external source (e.g., Excel).
 * Contains the test case key, summary, preconditions, and test steps.
 */
public class TestCase {
    private String key;
    private String summary;
    private String preconditions;
    private List<TestStep> testSteps;

    // Additional fields from Excel that might be useful
    private String description;
    private String status;
    private String priority;
    private String testCaseType;
    private String functionalCategory;
    private String componentUnderTest;

    public TestCase() {
        this.testSteps = new ArrayList<>();
    }

    public TestCase(String key, String summary, String preconditions) {
        this.key = key;
        this.summary = summary;
        this.preconditions = preconditions;
        this.testSteps = new ArrayList<>();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(String preconditions) {
        this.preconditions = preconditions;
    }

    public List<TestStep> getTestSteps() {
        return testSteps;
    }

    public void setTestSteps(List<TestStep> testSteps) {
        this.testSteps = testSteps;
    }

    public void addTestStep(TestStep testStep) {
        this.testSteps.add(testStep);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getTestCaseType() {
        return testCaseType;
    }

    public void setTestCaseType(String testCaseType) {
        this.testCaseType = testCaseType;
    }

    public String getFunctionalCategory() {
        return functionalCategory;
    }

    public void setFunctionalCategory(String functionalCategory) {
        this.functionalCategory = functionalCategory;
    }

    public String getComponentUnderTest() {
        return componentUnderTest;
    }

    public void setComponentUnderTest(String componentUnderTest) {
        this.componentUnderTest = componentUnderTest;
    }

    /**
     * Converts the test case into a formatted goal string suitable for ActionAgent.
     * @return A formatted string containing all test case information
     */
    public String toGoalString() {
        StringBuilder goal = new StringBuilder();
        
        goal.append("Test Case: ").append(summary != null ? summary : "").append("\n\n");
        
        if (key != null && !key.isEmpty()) {
            goal.append("Test Case Key: ").append(key).append("\n\n");
        }
        
        if (description != null && !description.isEmpty()) {
            goal.append("Description: ").append(description).append("\n\n");
        }
        
        if (preconditions != null && !preconditions.isEmpty()) {
            goal.append("Preconditions:\n").append(preconditions).append("\n\n");
        }
        
        if (testSteps != null && !testSteps.isEmpty()) {
            goal.append("Test Steps:\n\n");
            for (int i = 0; i < testSteps.size(); i++) {
                TestStep step = testSteps.get(i);
                goal.append("Step ").append(i + 1).append(":\n");
                if (step.getStepSummary() != null && !step.getStepSummary().isEmpty()) {
                    goal.append("  Action: ").append(step.getStepSummary()).append("\n");
                }
                if (step.getTestData() != null && !step.getTestData().isEmpty()) {
                    goal.append("  Test Data: ").append(step.getTestData()).append("\n");
                }
                if (step.getExpectedResult() != null && !step.getExpectedResult().isEmpty()) {
                    goal.append("  Expected Result: ").append(step.getExpectedResult()).append("\n");
                }
                goal.append("\n");
            }
        }
        
        return goal.toString().trim();
    }

    @Override
    public String toString() {
        return "TestCase{" +
                "key='" + key + '\'' +
                ", summary='" + summary + '\'' +
                ", preconditions='" + preconditions + '\'' +
                ", testSteps=" + testSteps.size() +
                '}';
    }
}
