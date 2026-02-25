package ui_adapter;

import ui_adapter.adapter.UIActionAdapter;
import ui_adapter.agent.LlmAgent;
import ui_adapter.agent.TestAgent;
import ui_adapter.executor.ActionLogger;
import ui_adapter.model.Action;
import ui_adapter.model.ActionResult;
import ui_adapter.model.TestCase;
import ui_adapter.testcase.ExcelTestCaseLoader;
import ui_adapter.testcase.TestCaseLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {

        // Load application.properties variables into System Properties so they can be accessed via System.getProperty()
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("application.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.out.println("Sorry, unable to find application.properties");
            } else {
                prop.load(input);
                prop.forEach((key, value) -> System.setProperty((String) key, (String) value));
                System.out.println("Loaded application.properties successfully.");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        // ========================================================================
        // Load test cases from Excel file and execute with LlmAgent
        // 
        // NOTE: This uses single-agent mode (Action Agent only, no validation).
        // For two-agent validation mode, use ValidationMain instead.
        // ========================================================================
        
        // Parse command line arguments
        String excelFilePath = "src/main/resources/test_cases.xlsx"; // Default
        
        for (int i = 0; i < args.length; i++) {
            if (("-testc".equals(args[i]) || "--testcase".equals(args[i])) && i + 1 < args.length) {
                excelFilePath = args[i + 1];
                break;
            }
        }
        
        System.out.println("Loading test cases from: " + excelFilePath);
        
        try {
            // Create loader
            TestCaseLoader loader = new ExcelTestCaseLoader(excelFilePath);
            
            // Load all test cases
            List<TestCase> testCases = loader.loadTestCases();
            System.out.println("Loaded " + testCases.size() + " test case(s) from Excel");
            
            // Execute each test case
            for (TestCase testCase : testCases) {
                System.out.println("\n" + "=".repeat(80));
                System.out.println("Executing Test Case: " + testCase.getKey());
                System.out.println("Summary: " + testCase.getSummary());
                System.out.println("Step Count: " + testCase.getTestSteps().size());
                System.out.println("=".repeat(80) + "\n");
                
                // Convert test case to goal string and pass to LlmAgent
                String goal = testCase.toGoalString();
                System.out.println("Goal for LLM:");
                System.out.println(goal);
                System.out.println("\n" + "-".repeat(80) + "\n");
                
                executeTestWithAgent(goal);
            }
            
        } catch (IOException e) {
            System.err.println("Error loading test cases from Excel: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Helper method to execute a test with the LlmAgent.
     * @param goal The test goal string
     */
    private static void executeTestWithAgent(String goal) {
        TestAgent agent = new LlmAgent(goal);
        List<ActionResult> results = new ArrayList<>();
        ActionResult lastResult = null;

        // Create action logger
        ActionLogger logger = new ActionLogger(true);
        logger.log("Starting test execution with goal: " + goal);

        try (UIActionAdapter adapter = new UIActionAdapter()) {
            // Enable action logging
            adapter.setActionLogger(logger);

            while (!agent.isTestComplete()) {
                Action action = agent.nextAction(lastResult);
                if (action == null) {
                    break;
                }

                lastResult = adapter.execute(action);
                results.add(lastResult);
            }
        }

        agent.onTestEnd(results);
        logger.log("Test execution completed");
        logger.close();

        // Print results
        System.out.println("\nTest Execution Results:");
        for (ActionResult r : results) {
            System.out.printf("[%s] %s%n", r.getStatus(), r.getMessage());
            if (r.getScreenshotPath() != null) {
                System.out.println("  Screenshot: " + r.getScreenshotPath());
            }
        }
        System.out.println("\nAction log saved to: " + logger.getLogFilePath());
    }
}