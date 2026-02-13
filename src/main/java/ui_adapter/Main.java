package ui_adapter;

import ui_adapter.adapter.UIActionAdapter;
import ui_adapter.agent.LlmAgent;
import ui_adapter.agent.TestAgent;
import ui_adapter.model.Action;
import ui_adapter.model.ActionResult;
import ui_adapter.model.TestCase;
import ui_adapter.testcase.ExcelTestCaseLoader;
import ui_adapter.testcase.TestCaseLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        
        // ========================================================================
        // EXAMPLE 1: Load test cases from Excel file and execute with LlmAgent
        // ========================================================================
        // Uncomment below to load test cases from Excel
        /*
        try {
            // Path to your Excel file
            String excelFilePath = "path/to/your/testcases.xlsx";
            
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
                System.out.println("=".repeat(80) + "\n");
                
                // Convert test case to goal string and pass to LlmAgent
                String goal = testCase.toGoalString();
                executeTestWithAgent(goal);
            }
            
        } catch (IOException e) {
            System.err.println("Error loading test cases from Excel: " + e.getMessage());
            e.printStackTrace();
        }
        */
        
        // ========================================================================
        // EXAMPLE 2: Load a specific test case by key
        // ========================================================================
        // Uncomment below to load a specific test case
        /*
        try {
            String excelFilePath = "path/to/your/testcases.xlsx";
            TestCaseLoader loader = new ExcelTestCaseLoader(excelFilePath);
            
            // Load specific test case by key
            TestCase testCase = loader.loadTestCase("TEST-123");
            
            System.out.println("Loaded test case: " + testCase.getKey());
            System.out.println(testCase.toGoalString());
            
            // Execute the test case
            executeTestWithAgent(testCase.toGoalString());
            
        } catch (IOException e) {
            System.err.println("Error loading test case: " + e.getMessage());
            e.printStackTrace();
        }
        */
        
        // ========================================================================
        // EXAMPLE 3: Manual test case execution (original approach)
        // ========================================================================
        
//        TestAgent agent = new SimpleRuleAgent("Navigate to CSE and take a screenshot");
//        TestAgent agent = new LlmAgent("Navigate to CSE.lk and search for 'TEEJAY LANKA PLC' in the search bar placesholder have text as 'Search' and there will be serach result with text 'TEEJAY LANKA PLC' or 'TJL.N0000' (assert this) go inside this result (click it) check an new page availbale with text 'TEEJAY LANKA PLC' on the new web page then scroll get the General Information from that page and sreen shot of it.then there will be financial tab (text visible as 'Financials') on that page down and take a screenshot.");
//        TestAgent agent = new LlmAgent("go to https://dashboard-qa.000-rpa-np.centralus.azr.sysco.net/  click 'OK' text on the pop up alert (it has text 'Click OK to get new version') then click login button on the web top right \n" +
//                "\n" +
//                "there will be a sso authentication (https://login.microsoftonline.com/b7aa4308-bf33-414f-9971-6e0c972cbe5d/oauth2/v2.0/authorize?client_id=cb684804-0268-449b-9364-5c52f522c8d0&scope=openid%20profile%20User.Read%20offline_access&redirect_uri=https%3A%2F%2Fdashboard-qa.000-rpa-np.centralus.azr.sysco.net%2F&client-request-id=794e478b-c730-4345-b200-fdd3163681fb&response_mode=fragment&response_type=code&x-client-SKU=msal.js.browser&x-client-VER=2.39.0&client_info=1&code_challenge=5ot7U0obQlIl4FCYMetvN8kraquWGLt3q85F0y1-MrQ&code_challenge_method=S256&nonce=9a9b468a-a0bd-4096-8dce-f46ccd9b8838&state=eyJpZCI6Ijc4NjhjMGRkLWVlNjYtNGUzYy05MDRmLWY3MzYxNzU0MjJiNSIsIm1ldGEiOnsiaW50ZXJhY3Rpb25UeXBlIjoicG9wdXAifX0%3D&sso_reload=true) you have to pass to that use following creadentials \n" +
//                "\n" +
//                "placeholder is 'email,phone, or Skype' : \"Chiran.Govinnage@sysco.com\"then click 'Next button' then there will be paceholder userID : \"cgov4385\", then click 'Next button' then Password : \"BscAI!2Data$2\"\n" +
//                "\n"
//        );

//        TestAgent agent = new LlmAgent(
//                "Verify Company Navigation and Financial Information – TEEJAY LANKA PLC (CSE Website)\n" +
//                        "Objective\n" +
//                        "\n" +
//                        "Validate that a user can search for TEEJAY LANKA PLC on the CSE.lk website, navigate to the correct company details page, and successfully view the General Information and Financials sections.\n" +
//                        "\n" +
//                        "Preconditions\n" +
//                        "\n" +
//                        "Web browser is available\n" +
//                        "\n" +
//                        "Internet connection is active\n" +
//                        "\n" +
//                        "Test Steps and Expected Results\n" +
//                        "\n" +
//                        "Navigate to CSE Website\n" +
//                        "\n" +
//                        "Action: Open a web browser and go to https://www.cse.lk\n" +
//                        "\n" +
//                        "Expected Result: CSE homepage loads successfully\n" +
//                        "\n" +
//                        "Verify Search Input Field\n" +
//                        "\n" +
//                        "Action: Locate the search input field on the homepage\n" +
//                        "\n" +
//                        "Expected Result: Search input field is visible and contains the placeholder text \"Search\"\n" +
//                        "\n" +
//                        "Search for Company\n" +
//                        "\n" +
//                        "Action: Enter \"TEEJAY LANKA PLC\" into the search input field\n" +
//                        "\n" +
//                        "Expected Result: Search suggestions or results are displayed\n" +
//                        "\n" +
//                        "Validate Search Results\n" +
//                        "\n" +
//                        "Action: Inspect the displayed search results\n" +
//                        "\n" +
//                        "Expected Result: At least one result contains \"TEEJAY LANKA PLC\" or the stock code \"TJL.N0000\"\n" +
//                        "\n" +
//                        "Select Company Result\n" +
//                        "\n" +
//                        "Action: Click on the search result labeled \"TEEJAY LANKA PLC\"\n" +
//                        "\n" +
//                        "Expected Result: User is navigated to the company details page\n" +
//                        "\n" +
//                        "Verify Company Details Page\n" +
//                        "\n" +
//                        "Action: Observe the page content after navigation\n" +
//                        "\n" +
//                        "Expected Result: The company name \"TEEJAY LANKA PLC\" is clearly visible on the page\n" +
//                        "\n" +
//                        "Locate General Information Section\n" +
//                        "\n" +
//                        "Action: Scroll down the page to the General Information section\n" +
//                        "\n" +
//                        "Expected Result: General Information section is visible and fully loaded\n" +
//                        "\n" +
//                        "Capture General Information Screenshot\n" +
//                        "\n" +
//                        "Action: Capture a screenshot of the General Information section\n" +
//                        "\n" +
//                        "Expected Result: Screenshot is successfully captured\n" +
//                        "\n" +
//                        "Locate Financials Section\n" +
//                        "\n" +
//                        "Action: Continue scrolling or locate the tab labeled \"Financials\" and click that tab there \n" +
//                        "\n" +
//                        "Expected Result: Financials tab/section is visible on the page\n" +
//                        "\n" +
//                        "Capture Financials Screenshot\n" +
//                        "\n" +
//                        "Action: Capture a screenshot of the Financials section or tab\n" +
//                        "\n" +
//                        "Expected Result: Screenshot is successfully captured"
//        );

        TestAgent agent = new LlmAgent(
                "Test Case: Verify Microsoft SSO Login and Bot Navigation – RPA Dashboard QA\n" +
                        "\n" +
                        "Objective:\n" +
                        "Validate that a user can access the RPA Dashboard QA environment, complete Microsoft SSO authentication, and navigate to a specific bot details page.\n" +
                        "\n" +
                        "Preconditions:\n" +
                        "- Browser is available\n" +
                        "- Internet connection is active\n" +
                        "\n" +
                        "Test Steps:\n" +
                        "1. Open a web browser and navigate to https://dashboard-qa.000-rpa-np.centralus.azr.sysco.net/\n" +
                        "   Expected: RPA Dashboard QA homepage loads successfully.\n" +
                        "\n" +
                        "2. Verify a version update popup is displayed.\n" +
                        "   Action: Click the button ( Xpath : //*[@id=\"root\"]/div/div[1]/div/div/div/p/a) which is visible on the popup alert .\n" +
                        "   Expected: Popup closes and application continues without errors.\n" +
                        "\n" +
                        "3. Locate the 'Login' text button at the top-right corner of the page and click it.\n" +
                        "   Expected: User is redirected to Microsoft SSO authentication in a new popup window.\n" +
                        "\n" +
                        "4. On the Microsoft SSO page which is a new popup page so you have to have navigate the tab, locate the input field with placeholder 'Email, phone, or Skype'.\n" +
                        "   Action: Enter 'Chiran.Govinnage@sysco.com' and click 'Next'.\n" +
                        "\n" +
                        "5. Locate the Enter your Sysco Network ID input field. \n" +
                        "   Action: On the Microsoft SSO page which is a new popup page so you have to have navigate that tab, Enter 'cgov4385' and click 'Next' wait there few moment. There will not be any place holders there. just one input field\n" +
                        "   Expected: Username is accepted without validation errors.\n" +
                        "\n" +
                        "6. Locate the password input field.\n" +
                        "   Action: On the Microsoft SSO page which is a new popup page so you have to have navigate that tab, Enter 'BscAI!2Data$27' and click the 'Sign in' button. There will not be any place holders there. just one input field\n" +
                        "\n" +
                        "7. If a confirmation popup appears, click the 'Yes' button to complete authentication.\n" +
                        "Action: On the Microsoft SSO page which is a new popup page so you have to have navigate that tab, click the 'Yes' button. \n"+
                        "   Expected: Authentication completes successfully.\n" +
                        "\n" +
                        "9. Close the second pop up screen still available in the screen. Otherviese it is ok\n" +
                        "Action: check the avaialility of the the second pop up screen if it is then close that TAB\n"+
                        "   Expected: there should be only one tab available now\n" +
                        "\n" +
                        "8. Verify the user is redirected back to the RPA Dashboard QA environment.\n" +
                        "Action: if you see any pop up alert on there click it on the 'OK' button .And if you see it is still login button visible click it. \n"+
                        "   Expected: User should successfully see the home page.\n" +
                        "\n" +
                        "9. Navigate to the Bots page.\n" +
                        "   Action: Click the 'Bots' link or Bots icon from the left-side navigation menu.\n" +
                        "   Expected: Bots page loads successfully.\n" +
                        "\n" +
                        "10. Locate the search input field with placeholder 'Search bot'.\n" +
                        "    Action: Enter 'CECE' and select the result.\n" +
                        "    Expected: CECE bot details page loads successfully.\n" +
                        "\n" +
                        "Overall Expected Result:\n" +
                        "- User successfully logs in via Microsoft SSO.\n" +
                        "- RPA Dashboard QA loads without errors after authentication.\n" +
                        "- Bots page is accessible.\n" +
                        "- CECE bot details page is displayed correctly.\n"+
                "\n"
        );



        List<ActionResult> results = new ArrayList<>();
        ActionResult lastResult = null;

        try (UIActionAdapter adapter = new UIActionAdapter()) {
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

        for (ActionResult r : results) {
            System.out.printf("[%s] %s%n", r.getStatus(), r.getMessage());
            if (r.getScreenshotPath() != null) {
                System.out.println("  Screenshot: " + r.getScreenshotPath());
            }
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

        try (UIActionAdapter adapter = new UIActionAdapter()) {
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

        // Print results
        System.out.println("\nTest Execution Results:");
        for (ActionResult r : results) {
            System.out.printf("[%s] %s%n", r.getStatus(), r.getMessage());
            if (r.getScreenshotPath() != null) {
                System.out.println("  Screenshot: " + r.getScreenshotPath());
            }
        }
    }
}