package ui_adapter;

import ui_adapter.model.Action;
import ui_adapter.model.Selector;

/**
 * Demo: How to use the new WAIT action in test cases
 */
public class WaitActionDemo {
    
    public static void main(String[] args) {
        System.out.println("WAIT Action Usage Examples\n");
        System.out.println("=".repeat(80));
        
        // Example 1: Default wait (5 seconds)
        Action wait1 = new Action(Action.Type.WAIT, null, null);
        System.out.println("1. Default Wait:");
        System.out.println("   " + wait1);
        System.out.println("   Usage: Wait for 5 seconds (default)\n");
        
        // Example 2: Custom wait time (3 seconds)
        Action wait2 = new Action(Action.Type.WAIT, null, "3");
        System.out.println("2. Custom Wait (3 seconds):");
        System.out.println("   " + wait2);
        System.out.println("   Usage: Wait for 3 seconds\n");
        
        // Example 3: Longer wait (10 seconds)
        Action wait3 = new Action(Action.Type.WAIT, null, "10");
        System.out.println("3. Longer Wait (10 seconds):");
        System.out.println("   " + wait3);
        System.out.println("   Usage: Wait for 10 seconds\n");
        
        System.out.println("=".repeat(80));
        System.out.println("\nIn Your Test Cases:");
        System.out.println("-------------------");
        System.out.println("Excel: 'wait few seconds' or 'wait 3 seconds'");
        System.out.println("LLM Agent will interpret this and create WAIT action");
        System.out.println("\nTest Step Format:");
        System.out.println("  Action: Wait for a few seconds");
        System.out.println("  Expected Result: System processes the previous action");
        System.out.println("\nOR with specific time:");
        System.out.println("  Action: Wait for 10 seconds");
        System.out.println("  Expected Result: Page loads completely");
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Features:");
        System.out.println("  ✓ Default: 5 seconds if no time specified");
        System.out.println("  ✓ Min: 0 seconds (will default to 5)");
        System.out.println("  ✓ Max: 60 seconds (safety limit)");
        System.out.println("  ✓ Selector: Not required for WAIT action");
        System.out.println("  ✓ Value: Number of seconds to wait");
        System.out.println("=".repeat(80));
    }
}
