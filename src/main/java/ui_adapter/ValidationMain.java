package ui_adapter;

import ui_adapter.executor.TwoAgentOrchestrator;
import ui_adapter.model.TestCase;
import ui_adapter.testcase.ExcelTestCaseLoader;
import ui_adapter.testcase.TestCaseLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Main entry point for Two-Agent Validation Mode.
 * 
 * This uses the dual-agent architecture:
 * - Action Agent: Decides what UI actions to perform
 * - Validation Agent: Validates if results match expectations
 * 
 * Usage:
 *   java ui_adapter.ValidationMain --testcase path/to/test_cases.xlsx
 */
public class ValidationMain {
    public static void main(String[] args) {

        // Load application.properties variables into System Properties
        try (InputStream input = ValidationMain.class.getClassLoader().getResourceAsStream("application.properties")) {
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
        
        // Parse command line arguments
        String excelFilePath = "src/main/resources/test_cases.xlsx"; // Default
        boolean detailedReporting = true; // Default
        
        for (int i = 0; i < args.length; i++) {
            if (("-testc".equals(args[i]) || "--testcase".equals(args[i])) && i + 1 < args.length) {
                excelFilePath = args[i + 1];
            }
            if ("--brief".equals(args[i])) {
                detailedReporting = false;
            }
        }
        
        System.out.println("\n" + "█".repeat(80));
        System.out.println("█" + " ".repeat(78) + "█");
        System.out.println("█" + centerText("TWO-AGENT VALIDATION MODE", 78) + "█");
        System.out.println("█" + " ".repeat(78) + "█");
        System.out.println("█".repeat(80) + "\n");
        
        System.out.println("Loading test cases from: " + excelFilePath);
        
        try {
            // Create loader
            TestCaseLoader loader = new ExcelTestCaseLoader(excelFilePath);
            
            // Load all test cases
            List<TestCase> testCases = loader.loadTestCases();
            System.out.println("Loaded " + testCases.size() + " test case(s) from Excel\n");
            
            // Track overall results
            List<TwoAgentOrchestrator.TwoAgentTestResult> allResults = new ArrayList<>();
            
            // Execute each test case with two-agent orchestrator
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                
                System.out.println("\n" + "▓".repeat(80));
                System.out.println("▓ TEST CASE " + (i + 1) + "/" + testCases.size());
                System.out.println("▓".repeat(80));
                
                TwoAgentOrchestrator orchestrator = null;
                try {
                    orchestrator = new TwoAgentOrchestrator(testCase);
                    orchestrator.setDetailedReporting(detailedReporting);
                    
                    TwoAgentOrchestrator.TwoAgentTestResult result = orchestrator.execute();
                    allResults.add(result);
                    
                } catch (Exception e) {
                    System.err.println("ERROR executing test case " + testCase.getKey() + ": " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    if (orchestrator != null) {
                        orchestrator.close();
                    }
                }
            }
            
            // Print overall summary
            printOverallSummary(allResults);
            
        } catch (IOException e) {
            System.err.println("Error loading test cases from Excel: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void printOverallSummary(List<TwoAgentOrchestrator.TwoAgentTestResult> results) {
        if (results.isEmpty()) {
            return;
        }
        
        System.out.println("\n\n" + "█".repeat(80));
        System.out.println("█" + " ".repeat(78) + "█");
        System.out.println("█" + centerText("OVERALL TEST SUITE SUMMARY", 78) + "█");
        System.out.println("█" + " ".repeat(78) + "█");
        System.out.println("█".repeat(80));
        
        int totalTests = results.size();
        int passedTests = (int) results.stream().filter(TwoAgentOrchestrator.TwoAgentTestResult::isSuccess).count();
        int failedTests = totalTests - passedTests;
        
        int totalActions = results.stream()
            .mapToInt(r -> r.getActionResults().size())
            .sum();
        int totalValidations = results.stream()
            .mapToInt(r -> r.getValidationResults().size())
            .sum();
        
        System.out.println("\nTest Cases Executed: " + totalTests);
        System.out.println("  ✅ Passed: " + passedTests);
        System.out.println("  ❌ Failed: " + failedTests);
        System.out.println("\nTotal Actions Executed: " + totalActions);
        System.out.println("Total Validations Performed: " + totalValidations);
        
        if (totalValidations > 0) {
            double avgConfidence = results.stream()
                .flatMap(r -> r.getValidationResults().stream())
                .mapToDouble(v -> v.getConfidence())
                .average()
                .orElse(0.0);
            System.out.println("Average Validation Confidence: " + String.format("%.1f%%", avgConfidence * 100));
        }
        
        System.out.println("\n" + "█".repeat(80));
        System.out.println("█" + " ".repeat(78) + "█");
        
        if (failedTests == 0) {
            System.out.println("█" + centerText("🎉 ALL TESTS PASSED! 🎉", 78) + "█");
        } else {
            System.out.println("█" + centerText("⚠️  SOME TESTS FAILED  ⚠️", 78) + "█");
        }
        
        System.out.println("█" + " ".repeat(78) + "█");
        System.out.println("█".repeat(80) + "\n");
        
        // Exit with appropriate code
        System.exit(failedTests > 0 ? 1 : 0);
    }
    
    private static String centerText(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int leftPad = (width - text.length()) / 2;
        int rightPad = width - text.length() - leftPad;
        return " ".repeat(leftPad) + text + " ".repeat(rightPad);
    }
}
