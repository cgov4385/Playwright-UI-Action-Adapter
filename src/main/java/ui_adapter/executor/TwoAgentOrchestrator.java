package ui_adapter.executor;

import ui_adapter.adapter.UIActionAdapter;
import ui_adapter.agent.LlmAgent;
import ui_adapter.agent.ValidationAgent;
import ui_adapter.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Two-Agent Test Orchestrator
 * 
 * Coordinates the execution flow between:
 * 1. Action Agent (LlmAgent) - decides what actions to perform
 * 2. Validation Agent - validates if results match expectations
 * 
 * This orchestrator implements the two-agent architecture pattern where
 * action execution and result validation are separate responsibilities.
 */
public class TwoAgentOrchestrator {
    
    private final LlmAgent actionAgent;
    private final ValidationAgent validationAgent;
    private final UIActionAdapter adapter;
    private final TestCase testCase;
    
    private final List<ActionResult> actionResults = new ArrayList<>();
    private final List<ValidationResult> validationResults = new ArrayList<>();
    
    private boolean enableDetailedReporting = true;
    
    public TwoAgentOrchestrator(TestCase testCase) {
        this.testCase = testCase;
        this.actionAgent = new LlmAgent(testCase.toGoalString());
        this.validationAgent = new ValidationAgent();
        this.adapter = new UIActionAdapter();
        
        // Enable page state capture for validation
        this.adapter.setCapturePageState(true);
    }
    
    public void setDetailedReporting(boolean enabled) {
        this.enableDetailedReporting = enabled;
    }
    
    /**
     * Executes the test case using the two-agent approach.
     * Returns the overall test run result with both action and validation results.
     */
    public TwoAgentTestResult execute() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TWO-AGENT TEST EXECUTION");
        System.out.println("Test Case: " + testCase.getKey() + " - " + testCase.getSummary());
        System.out.println("Steps: " + testCase.getTestSteps().size());
        System.out.println("=".repeat(80) + "\n");
        
        ActionResult lastActionResult = null;
        int stepNumber = 0;
        
        // Execute test steps
        while (!actionAgent.isTestComplete()) {
            stepNumber++;
            
            // Get current test step for validation (if available)
            TestStep currentStep = stepNumber <= testCase.getTestSteps().size() 
                ? testCase.getTestSteps().get(stepNumber - 1) 
                : null;
            
            // Phase 1: Action Agent decides next action
            Action action = actionAgent.nextAction(lastActionResult);
            if (action == null) {
                break;
            }
            
            System.out.println("\n" + "-".repeat(80));
            System.out.println("STEP " + stepNumber + (currentStep != null ? ": " + currentStep.getStepSummary() : ""));
            System.out.println("-".repeat(80));
            
            // Phase 2: Execute action
            ActionResult actionResult = adapter.execute(action);
            actionResults.add(actionResult);
            lastActionResult = actionResult;
            
            // Phase 3: Validation Agent validates result (if we have an expected result)
            if (currentStep != null && currentStep.getExpectedResult() != null 
                && !currentStep.getExpectedResult().trim().isEmpty()) {
                
                String stepContext = "Step " + stepNumber + ": " + currentStep.getStepSummary();
                ValidationResult validation = validationAgent.validate(
                    currentStep.getExpectedResult(),
                    actionResult,
                    stepContext
                );
                validationResults.add(validation);
                
                // Report validation
                if (enableDetailedReporting) {
                    System.out.println(validation.toDetailedReport());
                } else {
                    System.out.println("[VALIDATION] " + validation.getStatus() + 
                        " (confidence: " + String.format("%.0f%%", validation.getConfidence() * 100) + ")");
                    System.out.println("[VALIDATION] " + validation.getReasoning());
                }
                
                // Stop on validation failure (optional - can be configured)
                if (validation.getStatus() == ValidationResult.Status.FAIL) {
                    System.err.println("\n⚠️  VALIDATION FAILED - Stopping test execution");
                    break;
                }
            } else {
                // No validation for this step
                System.out.println("[VALIDATION] Skipped (no expected result defined)");
            }
        }
        
        // Notify action agent of completion
        actionAgent.onTestEnd(actionResults);
        
        // Generate summary
        return generateTestResult();
    }
    
    private TwoAgentTestResult generateTestResult() {
        int totalSteps = actionResults.size();
        int passedActions = (int) actionResults.stream()
            .filter(r -> r.getStatus() == ActionResult.Status.PASS)
            .count();
        int failedActions = totalSteps - passedActions;
        
        int totalValidations = validationResults.size();
        int passedValidations = (int) validationResults.stream()
            .filter(v -> v.getStatus() == ValidationResult.Status.PASS)
            .count();
        int failedValidations = (int) validationResults.stream()
            .filter(v -> v.getStatus() == ValidationResult.Status.FAIL)
            .count();
        int unknownValidations = totalValidations - passedValidations - failedValidations;
        
        boolean overallSuccess = failedActions == 0 && failedValidations == 0;
        
        TwoAgentTestResult result = new TwoAgentTestResult(
            testCase.getKey(),
            overallSuccess,
            actionResults,
            validationResults,
            totalSteps,
            passedActions,
            failedActions,
            totalValidations,
            passedValidations,
            failedValidations,
            unknownValidations
        );
        
        // Print summary
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST EXECUTION SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println(result.getSummary());
        System.out.println("=".repeat(80) + "\n");
        
        return result;
    }
    
    public void close() {
        if (adapter != null) {
            adapter.close();
        }
        if (validationAgent != null) {
            validationAgent.close();
        }
    }
    
    /**
     * Result object containing both action and validation results.
     */
    public static class TwoAgentTestResult {
        private final String testCaseKey;
        private final boolean success;
        private final List<ActionResult> actionResults;
        private final List<ValidationResult> validationResults;
        
        private final int totalSteps;
        private final int passedActions;
        private final int failedActions;
        private final int totalValidations;
        private final int passedValidations;
        private final int failedValidations;
        private final int unknownValidations;
        
        public TwoAgentTestResult(String testCaseKey, boolean success,
                                 List<ActionResult> actionResults,
                                 List<ValidationResult> validationResults,
                                 int totalSteps, int passedActions, int failedActions,
                                 int totalValidations, int passedValidations, 
                                 int failedValidations, int unknownValidations) {
            this.testCaseKey = testCaseKey;
            this.success = success;
            this.actionResults = actionResults;
            this.validationResults = validationResults;
            this.totalSteps = totalSteps;
            this.passedActions = passedActions;
            this.failedActions = failedActions;
            this.totalValidations = totalValidations;
            this.passedValidations = passedValidations;
            this.failedValidations = failedValidations;
            this.unknownValidations = unknownValidations;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public List<ActionResult> getActionResults() {
            return actionResults;
        }
        
        public List<ValidationResult> getValidationResults() {
            return validationResults;
        }
        
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Test Case: ").append(testCaseKey).append("\n");
            sb.append("Overall Status: ").append(success ? "✅ PASS" : "❌ FAIL").append("\n\n");
            
            sb.append("Action Execution:\n");
            sb.append("  Total Steps: ").append(totalSteps).append("\n");
            sb.append("  Passed: ").append(passedActions).append("\n");
            sb.append("  Failed: ").append(failedActions).append("\n\n");
            
            sb.append("Validation Results:\n");
            sb.append("  Total Validations: ").append(totalValidations).append("\n");
            sb.append("  Passed: ").append(passedValidations).append("\n");
            sb.append("  Failed: ").append(failedValidations).append("\n");
            sb.append("  Unknown: ").append(unknownValidations).append("\n");
            
            if (totalValidations > 0) {
                double avgConfidence = validationResults.stream()
                    .mapToDouble(ValidationResult::getConfidence)
                    .average()
                    .orElse(0.0);
                sb.append("  Avg Confidence: ").append(String.format("%.1f%%", avgConfidence * 100)).append("\n");
            }
            
            return sb.toString();
        }
    }
}
