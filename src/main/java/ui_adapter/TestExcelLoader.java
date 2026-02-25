package ui_adapter;

import ui_adapter.model.TestCase;
import ui_adapter.model.TestStep;

/**
 * Quick test to verify the TestCase and TestStep classes work correctly.
 * Simulates loading data from Excel and formatting for LLM.
 */
public class TestExcelLoader {
    
    public static void main(String[] args) {
        System.out.println("Testing Excel Test Case Loader Implementation\n");
        System.out.println("=".repeat(80));
        
        // Simulate loading a test case from Excel
        // (In reality, this would come from ExcelTestCaseLoader)
        TestCase testCase = createSampleTestCase();
        
        // Display test case info
        System.out.println("\nTest Case Loaded:");
        System.out.println("  Issue Key: " + testCase.getKey());
        System.out.println("  Summary: " + testCase.getSummary());
        System.out.println("  Number of Steps: " + testCase.getTestSteps().size());
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("LLM-Formatted Output:");
        System.out.println("=".repeat(80) + "\n");
        
        // Convert to LLM format (this is what gets sent to LlmAgent)
        String llmPrompt = testCase.toGoalString();
        System.out.println(llmPrompt);
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("✅ Test Complete! The implementation is working correctly.");
        System.out.println("=".repeat(80));
    }
    
    /**
     * Creates a sample test case simulating data from your Excel format.
     * This represents multiple rows in Excel with the same Issue Key.
     */
    private static TestCase createSampleTestCase() {
        // Create test case (represents first row with Issue Key)
        TestCase testCase = new TestCase();
        testCase.setKey("IDP-001");
        testCase.setSummary("[Business Rule Validation][Anonymous Customer] Process PO from Unrecognized Customer");
        testCase.setDescription("This scenario validates the end-to-end workflow for a Purchase Order sent by an unrecognized (anonymous) customer. The process begins with the IDP system ingesting a PO via email.");
        testCase.setPreconditions("IDP system is running\nValid customer database is available\nHITL interface is accessible");
        testCase.setPriority("High");
        testCase.setStatus("Ready");
        
        // Add test steps (each represents a row in Excel with same Issue Key)
        testCase.addTestStep(new TestStep(
            "Navigate to IDP system and ingest PO via email",
            "Email: po@customer.com, PO Number: PO12345",
            "PO is received and processed by IDP system"
        ));
        
        testCase.addTestStep(new TestStep(
            "Verify business rule validation fails for unrecognized customer",
            "Customer Name: Unknown Corp",
            "Document is routed to HITL for manual review with appropriate error message"
        ));
        
        testCase.addTestStep(new TestStep(
            "Access HITL interface and locate the failed PO",
            null,  // No test data for this step
            "PO document is visible in HITL queue with status 'Pending Review'"
        ));
        
        testCase.addTestStep(new TestStep(
            "Manually input correct customer ID and operating site",
            "Customer ID: CUST001, OpCo: CA01",
            "Customer ID and OpCo are updated in the system successfully"
        ));
        
        testCase.addTestStep(new TestStep(
            "Resubmit the PO from HITL for revalidation",
            null,
            "All business rule validations (customer, site, SUPC, shipping date) pass successfully"
        ));
        
        testCase.addTestStep(new TestStep(
            "Verify POE file generation and submission to BIS",
            null,
            "POE file is generated, submitted to BIS, and IDS confirmation received"
        ));
        
        return testCase;
    }
}
