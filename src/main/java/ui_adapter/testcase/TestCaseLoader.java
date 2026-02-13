package ui_adapter.testcase;

import ui_adapter.model.TestCase;

import java.io.IOException;
import java.util.List;

/**
 * Interface for loading test cases from various sources.
 */
public interface TestCaseLoader {
    
    /**
     * Loads all test cases from the source.
     * @return List of all test cases
     * @throws IOException if there's an error reading the source
     */
    List<TestCase> loadTestCases() throws IOException;
    
    /**
     * Loads a specific test case by its key.
     * @param testCaseKey The unique identifier for the test case
     * @return The test case with the given key
     * @throws IOException if the test case is not found or there's an error reading
     */
    TestCase loadTestCase(String testCaseKey) throws IOException;
}
