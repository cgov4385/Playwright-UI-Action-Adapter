package ui_adapter.agent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ui_adapter.agent.service.GeminiVertexLlmClient;
import ui_adapter.agent.service.GroqLlmClient;
import ui_adapter.agent.service.LlmClient;
import ui_adapter.model.ActionResult;
import ui_adapter.model.ValidationResult;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Validation Agent - Validates test results using LLM-powered fuzzy matching.
 * 
 * In the two-agent architecture:
 * - This class is the VALIDATION AGENT that validates actual results against expectations
 * - Works alongside LlmAgent (Action Agent) which generates UI actions
 * 
 * Responsibilities:
 * - Fuzzy matching of natural language expectations vs actual outcomes
 * - Confidence scoring (0.0 - 1.0) for validation results
 * - Detailed reasoning about why validation passed or failed
 * 
 * This agent compares the actual outcome (from ActionResult) with the expected result
 * (from TestStep) and determines if they match. It uses LLM to intelligently interpret
 * fuzzy expectations like "Login success", "Page loads", "Error message appears", etc.
 * 
 * Architecture Role: This is the second agent in the two-agent system.
 * - Action Agent: Decides WHAT actions to perform
 * - Validation Agent: Decides IF the result matches expectations
 * 
 * @see ActionAgent (Action Agent - generates UI actions)
 * @see TwoAgentOrchestrator for the orchestration of both agents
 */
public class ValidationAgent {

    // Provider selection: groq | gemini-vertex
    private static final String LLM_PROVIDER = getConfig("LLM_PROVIDER", "gemini-vertex");
    private static final String GROQ_MODEL = getConfig("GROQ_MODEL", "openai/gpt-oss-safeguard-20b");
    private static final String VERTEX_PROJECT = getConfigAny(new String[]{"VERTEX_PROJECT", "GOOGLE_CLOUD_PROJECT"}, "");
    private static final String VERTEX_LOCATION = getConfigAny(new String[]{"VERTEX_LOCATION", "GOOGLE_CLOUD_LOCATION"}, "us-central1");
    private static final String GEMINI_MODEL = getConfig("GEMINI_MODEL", "gemini-2.5-flash");
    private static final String GROQ_API_KEY = getApiKey();

    private final Gson gson;
    private final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    private final HttpClient client;
    private final ExecutorService executorService;
    private final LlmClient llmClient;

    private static final boolean VALIDATION_WIRE_LOG_ENABLED = 
        Boolean.parseBoolean(getConfig("VALIDATION_WIRE_LOG_ENABLED", "true"));

    private static String getConfig(String key, String defaultVal) {
        String v = System.getenv(key);
        if (v == null || v.isEmpty()) {
            v = System.getProperty(key);
        }
        return (v == null || v.isEmpty()) ? defaultVal : v;
    }

    private static String getConfigAny(String[] keys, String defaultVal) {
        for (String k : keys) {
            String v = getConfig(k, "");
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return defaultVal;
    }

    private static String getApiKey() {
        String key = System.getenv("GROQ_API_KEY");
        if (key == null || key.isEmpty()) {
            key = System.getProperty("GROQ_API_KEY");
        }
        return key;
    }

    public ValidationAgent() {
        this.gson = new Gson();

        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        this.client = HttpClient.newBuilder()
                .executor(executorService)
                .build();

        this.llmClient = createClient();
    }

    private LlmClient createClient() {
        String provider = (LLM_PROVIDER == null ? "groq" : LLM_PROVIDER).trim().toLowerCase();
        switch (provider) {
            case "gemini":
            case "gemini-vertex":
            case "vertex":
            case "vertex-gemini":
                if (VERTEX_PROJECT == null || VERTEX_PROJECT.isEmpty()) {
                    throw new IllegalStateException("Vertex project missing. Set VERTEX_PROJECT or GOOGLE_CLOUD_PROJECT.");
                }
                return new GeminiVertexLlmClient(VERTEX_PROJECT, VERTEX_LOCATION, GEMINI_MODEL);
            case "groq":
            default:
                return new GroqLlmClient(client, GROQ_API_KEY);
        }
    }

    /**
     * Validates if the actual result matches the expected result.
     * 
     * @param expectedResult The expected outcome described in natural language (from TestStep)
     * @param actionResult The actual outcome from executing actions (from UIActionAdapter)
     * @param stepContext Additional context about the test step (optional)
     * @return ValidationResult with status, reasoning, and confidence
     */
    public ValidationResult validate(String expectedResult, ActionResult actionResult, String stepContext) {
        if (expectedResult == null || expectedResult.trim().isEmpty()) {
            return ValidationResult.unknown("No expected result specified", expectedResult, "N/A");
        }

        if (actionResult == null) {
            return ValidationResult.fail("No action result available", 1.0, 
                "Cannot validate without action result", expectedResult, "null");
        }

        // If the action itself failed, that's typically a validation failure
        if (actionResult.getStatus() == ActionResult.Status.FAIL) {
            return ValidationResult.fail(
                "Action execution failed: " + actionResult.getMessage(),
                0.95,
                "Action error: " + (actionResult.getErrorType() != null ? actionResult.getErrorType() : "UNKNOWN"),
                expectedResult,
                actionResult.getMessage()
            );
        }

        // Prepare context for LLM validation
        String actualContext = buildActualContext(actionResult);

        try {
            ValidationResult result = callValidationLlm(expectedResult, actualContext, stepContext);
            
            if (VALIDATION_WIRE_LOG_ENABLED) {
                System.out.println("[VALIDATION] " + result.getStatus() + 
                    " (confidence: " + String.format("%.0f%%", result.getConfidence() * 100) + ")");
                System.out.println("[VALIDATION] " + result.getReasoning());
            }
            
            return result;

        } catch (Exception e) {
            System.err.println("[VALIDATION] ERROR: " + e.getMessage());
            return ValidationResult.unknown(
                "Validation failed due to error: " + e.getMessage(),
                expectedResult,
                actualContext
            );
        }
    }

    /**
     * Convenience method for validation without step context.
     */
    public ValidationResult validate(String expectedResult, ActionResult actionResult) {
        return validate(expectedResult, actionResult, null);
    }

    /**
     * Builds a comprehensive context string from the ActionResult.
     */
    private String buildActualContext(ActionResult actionResult) {
        StringBuilder context = new StringBuilder();
        
        context.append("Action Status: ").append(actionResult.getStatus()).append("\n");
        context.append("Action Message: ").append(actionResult.getMessage()).append("\n");
        
        if (actionResult.getPageState() != null && !actionResult.getPageState().isEmpty()) {
            // Truncate page state if too long (LLM token limits)
            String pageState = actionResult.getPageState();
            int maxLength = 5000; // Reasonable limit for page content
            if (pageState.length() > maxLength) {
                pageState = pageState.substring(0, maxLength) + "... (truncated)";
            }
            context.append("Page Content: ").append(pageState).append("\n");
        }
        
        if (actionResult.getScreenshotPath() != null) {
            context.append("Screenshot: ").append(actionResult.getScreenshotPath()).append("\n");
        }
        
        return context.toString();
    }

    /**
     * Calls the LLM to validate expected vs actual results.
     */
    private ValidationResult callValidationLlm(String expectedResult, String actualContext, String stepContext) throws Exception {
        List<JsonObject> messages = new ArrayList<>();

        // System prompt for validation
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", getValidationSystemPrompt());
        messages.add(systemMessage);

        // User message with validation request
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", buildValidationPrompt(expectedResult, actualContext, stepContext));
        messages.add(userMessage);

        // Build request JSON in the same format as LlmAgent
        JsonObject requestBody = new JsonObject();
        
        // Use the appropriate model based on provider
        String provider = llmClient.providerName();
        if (provider.startsWith("groq")) {
            requestBody.addProperty("model", GROQ_MODEL);
        } else {
            requestBody.addProperty("model", GEMINI_MODEL);
        }
        
        requestBody.addProperty("temperature", 0.0);
        
        JsonArray messagesArray = new JsonArray();
        for (JsonObject m : messages) {
            messagesArray.add(m);
        }
        requestBody.add("messages", messagesArray);
        
        // Response format for structured output
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        requestBody.add("response_format", responseFormat);
        
        String jsonBody = gson.toJson(requestBody);
        
        // Call LLM
        String jsonResponse = llmClient.chatCompletions(jsonBody);
        
        // Parse response
        return parseValidationResponse(jsonResponse, expectedResult, actualContext);
    }

    private String getValidationSystemPrompt() {
        return "You are a Test Validation Agent. Your job is to determine if an actual test result matches an expected result.\n" +
                "\n" +
                "The expected result is described in natural language (often fuzzy/imprecise).\n" +
                "The actual result includes action status, messages, and page content.\n" +
                "\n" +
                "Your task:\n" +
                "1. Interpret the expected result (what does \"Login success\" mean? \"Page loads\"? \"Error appears\"?)\n" +
                "2. Analyze the actual result (page content, status, messages)\n" +
                "3. Determine if they match (PASS/FAIL/UNKNOWN)\n" +
                "4. Provide clear reasoning\n" +
                "5. Assign a confidence score (0.0 to 1.0)\n" +
                "\n" +
                "Respond ONLY with valid JSON in this format:\n" +
                "{\n" +
                "  \"status\": \"PASS\" or \"FAIL\" or \"UNKNOWN\",\n" +
                "  \"reasoning\": \"Brief explanation of why it passed/failed\",\n" +
                "  \"confidence\": 0.95,\n" +
                "  \"details\": \"Optional additional details\"\n" +
                "}\n" +
                "\n" +
                "Guidelines:\n" +
                "- Be lenient with fuzzy matches (e.g., \"Login success\" matches \"Welcome, User!\")\n" +
                "- Look for keywords, patterns, and context clues\n" +
                "- If page contains error text when expecting success -> FAIL\n" +
                "- If page shows success indicators when expecting success -> PASS\n" +
                "- Use UNKNOWN only when truly ambiguous\n" +
                "- Confidence should reflect certainty (0.9+ for clear cases, 0.5-0.8 for fuzzy matches)";
    }

    private String buildValidationPrompt(String expectedResult, String actualContext, String stepContext) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("EXPECTED RESULT:\n");
        prompt.append(expectedResult).append("\n\n");
        
        prompt.append("ACTUAL RESULT:\n");
        prompt.append(actualContext).append("\n\n");
        
        if (stepContext != null && !stepContext.trim().isEmpty()) {
            prompt.append("STEP CONTEXT:\n");
            prompt.append(stepContext).append("\n\n");
        }
        
        prompt.append("Does the actual result match the expected result? Respond with JSON.");
        
        return prompt.toString();
    }

    private ValidationResult parseValidationResponse(String jsonResponse, String expectedResult, String actualContext) {
        try {
            JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            String statusStr = response.has("status") ? response.get("status").getAsString() : "UNKNOWN";
            ValidationResult.Status status = ValidationResult.Status.valueOf(statusStr.toUpperCase());
            
            String reasoning = response.has("reasoning") ? response.get("reasoning").getAsString() : "No reasoning provided";
            
            double confidence = response.has("confidence") ? response.get("confidence").getAsDouble() : 0.5;
            
            String details = response.has("details") ? response.get("details").getAsString() : null;
            
            // Extract a reasonable actual result summary from context
            String actualSummary = extractSummary(actualContext, 200);
            
            return new ValidationResult(status, reasoning, confidence, details, expectedResult, actualSummary);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse validation response: " + jsonResponse, e);
        }
    }

    private String extractSummary(String text, int maxLength) {
        if (text == null) return "";
        text = text.trim();
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /**
     * Closes resources used by the agent.
     */
    public void close() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
