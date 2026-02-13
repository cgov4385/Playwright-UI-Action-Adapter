package ui_adapter.testcase;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ui_adapter.model.TestCase;
import ui_adapter.model.TestStep;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Loads test cases from Excel files.
 * Supports reading test cases with multiple steps from Excel format.
 */
public class ExcelTestCaseLoader implements TestCaseLoader {

    // Column names as they appear in the Excel file
    private static final String COL_ISSUE_KEY = "Issue Key";
    private static final String COL_SUMMARY = "Summary";
    private static final String COL_DESCRIPTION = "Description";
    private static final String COL_PRECONDITION = "Precondition";
    private static final String COL_STATUS = "Status";
    private static final String COL_PRIORITY = "Priority";
    private static final String COL_STEP_SUMMARY = "Step Summary";
    private static final String COL_TEST_STEP = "Test Step";  // Alternative name for Step Summary
    private static final String COL_TEST_DATA = "Test Data";
    private static final String COL_EXPECTED_RESULT = "Expected Result";
    private static final String COL_TESTCASE_TYPE = "TestCase Type";
    private static final String COL_FUNCTIONAL_CATEGORY = "functional category";
    private static final String COL_COMPONENT_UNDER_TEST = "component under test";

    private final String filePath;

    public ExcelTestCaseLoader(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public List<TestCase> loadTestCases() throws IOException {
        List<TestCase> testCases = new ArrayList<>();
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Excel file not found: " + filePath);
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0); // Read first sheet
            
            // Read header row to get column indices
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("Excel file is empty or has no header row");
            }
            
            Map<String, Integer> columnMap = createColumnMap(headerRow);
            
            // Read data rows
            Map<String, TestCase> testCaseMap = new LinkedHashMap<>();
            String lastIssueKey = null;
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }
                
                String issueKey = getCellValue(row, columnMap.get(COL_ISSUE_KEY));
                
                // If issue key is empty, try to use the last seen issue key (continuation of test case)
                if (issueKey == null || issueKey.trim().isEmpty()) {
                    if (lastIssueKey != null) {
                        issueKey = lastIssueKey;
                    } else {
                        // Skip if no issue key and no previous issue key
                        continue;
                    }
                } else {
                    lastIssueKey = issueKey;
                }
                
                // Get or create test case
                TestCase testCase = testCaseMap.get(issueKey);
                if (testCase == null) {
                    testCase = new TestCase();
                    testCase.setKey(issueKey);
                    testCase.setSummary(getCellValue(row, columnMap.get(COL_SUMMARY)));
                    testCase.setDescription(getCellValue(row, columnMap.get(COL_DESCRIPTION)));
                    testCase.setPreconditions(getCellValue(row, columnMap.get(COL_PRECONDITION)));
                    testCase.setStatus(getCellValue(row, columnMap.get(COL_STATUS)));
                    testCase.setPriority(getCellValue(row, columnMap.get(COL_PRIORITY)));
                    testCase.setTestCaseType(getCellValue(row, columnMap.get(COL_TESTCASE_TYPE)));
                    testCase.setFunctionalCategory(getCellValue(row, columnMap.get(COL_FUNCTIONAL_CATEGORY)));
                    testCase.setComponentUnderTest(getCellValue(row, columnMap.get(COL_COMPONENT_UNDER_TEST)));
                    
                    testCaseMap.put(issueKey, testCase);
                }
                
                // Add test step if any step data exists
                // Support both "Step Summary" and "Test Step" column names
                String stepSummary = getCellValue(row, columnMap.get(COL_STEP_SUMMARY));
                if (stepSummary == null || stepSummary.trim().isEmpty()) {
                    stepSummary = getCellValue(row, columnMap.get(COL_TEST_STEP));
                }
                
                String testData = getCellValue(row, columnMap.get(COL_TEST_DATA));
                String expectedResult = getCellValue(row, columnMap.get(COL_EXPECTED_RESULT));
                
                // Add step if there's at least an action or expected result
                if ((stepSummary != null && !stepSummary.trim().isEmpty()) ||
                    (expectedResult != null && !expectedResult.trim().isEmpty())) {
                    
                    TestStep testStep = new TestStep(stepSummary, testData, expectedResult);
                    testCase.addTestStep(testStep);
                }
            }
            
            testCases.addAll(testCaseMap.values());
        }
        
        return testCases;
    }

    @Override
    public TestCase loadTestCase(String testCaseKey) throws IOException {
        List<TestCase> allTestCases = loadTestCases();
        
        for (TestCase testCase : allTestCases) {
            if (testCase.getKey() != null && testCase.getKey().equals(testCaseKey)) {
                return testCase;
            }
        }
        
        throw new IOException("Test case not found with key: " + testCaseKey);
    }

    /**
     * Creates a map of column names to their indices
     */
    private Map<String, Integer> createColumnMap(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String columnName = cell.getStringCellValue().trim();
                columnMap.put(columnName, i);
            }
        }
        
        return columnMap;
    }

    /**
     * Gets cell value as string, handling different cell types
     */
    private String getCellValue(Row row, Integer columnIndex) {
        if (columnIndex == null) {
            return null;
        }
        
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BLANK:
                return null;
            default:
                return null;
        }
    }

    /**
     * Checks if a row is empty
     */
    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValue(row, i);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Utility method to print test case summary for debugging
     */
    public static void printTestCaseSummary(TestCase testCase) {
        System.out.println("=".repeat(80));
        System.out.println("Test Case: " + testCase.getKey());
        System.out.println("Summary: " + testCase.getSummary());
        System.out.println("Preconditions: " + testCase.getPreconditions());
        System.out.println("Number of Steps: " + testCase.getTestSteps().size());
        System.out.println("=".repeat(80));
        System.out.println();
    }
}

