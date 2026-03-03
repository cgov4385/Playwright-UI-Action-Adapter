package ui_adapter;

import ui_adapter.adapter.UIActionAdapter;
import ui_adapter.agent.ActionAgent;
import ui_adapter.agent.TestAgent;
import ui_adapter.model.Action;
import ui_adapter.model.ActionResult;
import ui_adapter.model.TestCase;
import ui_adapter.model.TestStep;
import ui_adapter.testcase.ExcelTestCaseLoader;
import ui_adapter.testcase.TestCaseLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Example demonstrating how to use the Excel Test Case Loader
 * to load test cases and execute them with LlmAgent.
 */
public class ExcelTestCaseExample {

    public static void main(String[] args) {
        // Example 1: Load all test cases from Excel
        loadAndExecuteAllTestCases("src\\main\\resources\\test_cases_copy.xlsx");
        
        // Example 2: Load a specific test case by key
        // loadAndExecuteSpecificTestCase("path/to/your/testcases.xlsx", "TEST-123");
        
        // Example 3: Create test case programmatically
        // createAndExecuteTestCaseProgrammatically();
    }

    /**
     * Example 1: Load all test cases from an Excel file and execute them
     */
    public static void loadAndExecuteAllTestCases(String excelFilePath) {
        try {
            System.out.println("Loading test cases from: " + excelFilePath);
            
            // Create the loader
            TestCaseLoader loader = new ExcelTestCaseLoader(excelFilePath);
            
            // Load all test cases
            List<TestCase> testCases = loader.loadTestCases();
            System.out.println("Successfully loaded " + testCases.size() + " test case(s)\n");
            
            // Execute each test case
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                
                System.out.println("\n" + "=".repeat(100));
                System.out.println("Executing Test Case " + (i + 1) + " of " + testCases.size());
                System.out.println("Key: " + testCase.getKey());
                System.out.println("Summary: " + testCase.getSummary());
                System.out.println("Steps: " + testCase.getTestSteps().size());
                System.out.println("=".repeat(100) + "\n");
                
                // Convert test case to goal string
                String goal = testCase.toGoalString();
                System.out.println("Goal for LlmAgent:");
                System.out.println(goal);
                System.out.println("\n" + "-".repeat(100) + "\n");
                
                // Execute with LlmAgent
                executeTestWithAgent(goal);
            }
            
        } catch (IOException e) {
            System.err.println("Error loading test cases: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example 2: Load and execute a specific test case by key
     */
    public static void loadAndExecuteSpecificTestCase(String excelFilePath, String testCaseKey) {
        try {
            System.out.println("Loading test case with key: " + testCaseKey);
            
            TestCaseLoader loader = new ExcelTestCaseLoader(excelFilePath);
            TestCase testCase = loader.loadTestCase(testCaseKey);
            
            System.out.println("Found test case: " + testCase.getSummary());
            System.out.println("Number of steps: " + testCase.getTestSteps().size());
            
            // Print test case details
            System.out.println("\n" + "=".repeat(100));
            System.out.println(testCase.toGoalString());
            System.out.println("=".repeat(100) + "\n");
            
            // Execute with LlmAgent
            executeTestWithAgent(testCase.toGoalString());
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example 3: Create a test case programmatically and execute it
     */
    public static void createAndExecuteTestCaseProgrammatically() {
        // Create a test case manually
        TestCase testCase = new TestCase();
        testCase.setKey("MANUAL-001");
        testCase.setSummary("Verify Login Functionality");
        testCase.setPreconditions("User should have valid credentials");
        testCase.setDescription("This test validates the login functionality of the application");
        
        // Add test steps
        TestStep step1 = new TestStep(
            "Navigate to login page",
            "URL: https://example.com/login",
            "Login page should be displayed"
        );
        testCase.addTestStep(step1);
        
        TestStep step2 = new TestStep(
            "Enter username and password",
            "Username: testuser, Password: testpass123",
            "Credentials should be entered in the respective fields"
        );
        testCase.addTestStep(step2);
        
        TestStep step3 = new TestStep(
            "Click login button",
            "",
            "User should be logged in and redirected to dashboard"
        );
        testCase.addTestStep(step3);
        
        // Convert to goal string
        String goal = testCase.toGoalString();
        System.out.println("Programmatically created test case:");
        System.out.println(goal);
        System.out.println("\n" + "=".repeat(100) + "\n");
        
        // Execute with LlmAgent
        executeTestWithAgent(goal);
    }

    /**
     * Helper method to execute a test with the ActionAgent
     */
    private static void executeTestWithAgent(String goal) {
        TestAgent agent = new ActionAgent(goal);
        List<ActionResult> results = new ArrayList<>();
        ActionResult lastResult = null;

        try (UIActionAdapter adapter = new UIActionAdapter()) {
            System.out.println("Starting test execution...\n");
            
            while (!agent.isTestComplete()) {
                Action action = agent.nextAction(lastResult);
                if (action == null) {
                    System.out.println("No more actions to execute.");
                    break;
                }

                System.out.println("Executing action: " + action.getType());
                lastResult = adapter.execute(action);
                results.add(lastResult);
                
                System.out.println("  Result: " + lastResult.getStatus() + " - " + lastResult.getMessage());
            }
            
            agent.onTestEnd(results);
            
        } catch (Exception e) {
            System.err.println("Error during test execution: " + e.getMessage());
            e.printStackTrace();
        }

        // Print final results summary
        System.out.println("\n" + "=".repeat(100));
        System.out.println("Test Execution Summary");
        System.out.println("=".repeat(100));
        System.out.println("Total Actions: " + results.size());
        
        long successCount = results.stream()
            .filter(r -> ActionResult.Status.PASS.equals(r.getStatus()))
            .count();
        long failCount = results.stream()
            .filter(r -> ActionResult.Status.FAIL.equals(r.getStatus()))
            .count();
        
        System.out.println("Successful: " + successCount);
        System.out.println("Failed: " + failCount);
        
        System.out.println("\nDetailed Results:");
        for (int i = 0; i < results.size(); i++) {
            ActionResult r = results.get(i);
            System.out.printf("%d. [%s] %s%n", i + 1, r.getStatus(), r.getMessage());
            if (r.getScreenshotPath() != null) {
                System.out.println("   Screenshot: " + r.getScreenshotPath());
            }
        }
        System.out.println("=".repeat(100) + "\n");
    }
}
