package ui_adapter.agent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ui_adapter.model.Action;
import ui_adapter.model.ActionResult;
import ui_adapter.model.Selector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An agent that uses the Groq API (OpenAI compatible) to decide the next action.
 * <p>
 * It sends the conversation history (system prompt + previous results) to the LLM
 * and expects a JSON response corresponding to a deterministic UI Action.
 */
public class LlmAgent implements TestAgent {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    // NOTE: In production, do not hardcode keys. Use environment variables.
    private static final String API_KEY = getApiKey();
    private static final String MODEL = "openai/gpt-oss-safeguard-20b"; // Change this string to switch models (e.g., "mixtral-8x7b-32768")

    private final String goal;
    private final List<JsonObject> messages;
    private final Gson gson;
    private final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    private final HttpClient client;
    private final ExecutorService executorService; // Manage threads manually to avoid warnings
    private boolean isComplete = false;
    private int consecutiveFailures = 0;
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final boolean LLM_WIRE_LOG_ENABLED = true;
    private static final int LLM_MAX_MESSAGES_TO_LOG = 6;

    private static String getApiKey() {
        String key = System.getenv("GROQ_API_KEY");
        if (key == null || key.isEmpty()) {
            key = System.getProperty("GROQ_API_KEY");
        }
        return key;
    }

    public LlmAgent(String goal) {
        this.goal = goal;
        this.gson = new Gson();

        // Use a daemon thread executor so the JVM exits promptly
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        this.client = HttpClient.newBuilder()
                .executor(executorService)
                .build();

        this.messages = new ArrayList<>();

        // 1. Initialize System Prompt
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", getSystemPrompt());
        this.messages.add(systemMessage);

        // 2. Add User Goal
        JsonObject goalMessage = new JsonObject();
        goalMessage.addProperty("role", "user");
        goalMessage.addProperty("content", "GOAL: " + goal);
        this.messages.add(goalMessage);
    }

    @Override
    public Action nextAction(ActionResult previousResult) {
        if (isComplete) {
            return null; // Stop
        }

        // 3. Append result of the previous action (if any)
        if (previousResult != null) {
            JsonObject resultMessage = new JsonObject();
            resultMessage.addProperty("role", "user");
            resultMessage.addProperty("content", "ACTION_RESULT: " + previousResult.toString());
            messages.add(resultMessage);

            if (previousResult.getStatus() == ActionResult.Status.FAIL) {
                consecutiveFailures++;
                System.err.println("Action failed (" + consecutiveFailures + "/" + MAX_CONSECUTIVE_FAILURES + "): " + previousResult.getMessage());

                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    System.err.println("Too many consecutive failures. Stopping.");
                    isComplete = true;
                    return null;
                }

                // Do NOT stop immediately. Let the LLM try to "heal" or try a different approach.
            } else {
                consecutiveFailures = 0; // Reset on success
            }
        }

        // 4. Call LLM to get the NEXT action
        try {
            String jsonResponse;
            try {
                jsonResponse = callGroqApi();
            } catch (RuntimeException apiEx) {
                // If the API rejects a tool-call style generation, attempt to recover using failed_generation.
                String recovered = tryRecoverFailedGenerationJson(apiEx.getMessage());
                if (recovered != null) {
                    jsonResponse = recovered;
                } else {
                    throw apiEx;
                }
            }

            Action action = parseLlmResponse(jsonResponse);

            // Add the assistant's decision to history
            JsonObject assistantMessage = new JsonObject();
            assistantMessage.addProperty("role", "assistant");
            assistantMessage.addProperty("content", gson.toJson(action));
            messages.add(assistantMessage);

            if (LLM_WIRE_LOG_ENABLED) {
                System.out.println("[LLM] NEXT ACTION => " + summarizeAction(action));
            }

            return action;

        } catch (Exception e) {
            // Don't print null; include class for easier debugging
            System.err.println("[LLM] ERROR: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            isComplete = true;
            return null;
        }
    }

    @Override
    public boolean isTestComplete() {
        return isComplete;
    }

    @Override
    public void onTestEnd(List<ActionResult> results) {
        System.out.println("LLM Agent finished. Total steps: " + results.size());
        // Cleanup resources
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    // --- Private Helpers ---

    private String callGroqApi() throws Exception {
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalStateException("GROQ_API_KEY environment variable is missing!");
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL);
        requestBody.addProperty("temperature", 0.0);

        // Create request messages as-is, but in logs only show a tail (last N)
        JsonArray messagesArray = new JsonArray();
        for (JsonObject m : messages) {
            messagesArray.add(m);
        }
        requestBody.add("messages", messagesArray);

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        requestBody.add("response_format", responseFormat);

        String jsonBody = gson.toJson(requestBody);

        if (LLM_WIRE_LOG_ENABLED) {
            System.out.println("\n==================================================");
            System.out.println("[LLM] REQUEST");
            System.out.println("  url   : " + API_URL);
            System.out.println("  model : " + MODEL);
            System.out.println("  temp  : 0.0");
            System.out.println("\n[LLM] MESSAGES (last " + LLM_MAX_MESSAGES_TO_LOG + "):");
            logMessageTail();
            System.out.println("\n[LLM] FULL REQUEST JSON:");
            System.out.println(prettyGson.toJson(JsonParser.parseString(jsonBody)));
            System.out.println("==================================================");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (LLM_WIRE_LOG_ENABLED) {
            System.out.println("\n==================================================");
            System.out.println("[LLM] RESPONSE");
            System.out.println("  status : " + response.statusCode());
            System.out.println("\n[LLM] RAW RESPONSE JSON:");
            try {
                System.out.println(prettyGson.toJson(JsonParser.parseString(response.body())));
            } catch (Exception ignore) {
                System.out.println(response.body());
            }
            System.out.println("==================================================");
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("API Error: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    private Action parseLlmResponse(String jsonResponse) {
        // Extract content from Groq response structure
        JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
        JsonArray choices = root.getAsJsonArray("choices");
        String content = choices.get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        // The model should return a JSON object, but some models return a JSON array of actions.
        var parsed = JsonParser.parseString(content);
        JsonObject decision;
        if (parsed.isJsonArray()) {
            JsonArray arr = parsed.getAsJsonArray();
            if (arr.isEmpty() || !arr.get(0).isJsonObject()) {
                throw new IllegalArgumentException("Invalid LLM response: expected non-empty JSON array of objects");
            }
            decision = arr.get(0).getAsJsonObject();
        } else {
            decision = parsed.getAsJsonObject();
        }

        // Some models sometimes wrap the actual payload as a tool-call-like object:
        // { "name": "assistant", "arguments": { ...actual decision... } }
        if (decision.has("arguments") && decision.get("arguments").isJsonObject()) {
            decision = decision.getAsJsonObject("arguments");
        }

        // Print the Agent's reasoning (Chain of Thought)
        if (decision.has("thought")) {
            System.out.println("\n[AGENT THOUGHT]: " + decision.get("thought").getAsString());
        }

        // Check if agent wants to stop
        if (decision.has("isComplete") && decision.get("isComplete").getAsBoolean()) {
            isComplete = true;
            return null;
        }

        // Parse Action Type
        String typeStr = decision.get("type").getAsString();
        Action.Type actionType = Action.Type.valueOf(typeStr.toUpperCase());

        // Parse Selector (nullable)
        Selector selector = null;
        if (decision.has("selector") && !decision.get("selector").isJsonNull()) {
            JsonObject selObj = decision.getAsJsonObject("selector");
            String selTypeStr = selObj.get("type").getAsString();
            String selVal = selObj.get("value").getAsString();

            // If the model emits selector.type=URL, treat it as NAVIGATE url in the action.value.
            if ("URL".equalsIgnoreCase(selTypeStr) && (decision.get("value").isJsonNull() || decision.get("value").getAsString().isEmpty())) {
                // Keep selector null for navigate, value will be read below.
            } else {
                String role = null;
                String nearText = null;
                String description = null;

                if (selObj.has("meta") && !selObj.get("meta").isJsonNull()) {
                    JsonObject meta = selObj.getAsJsonObject("meta");
                    if (meta.has("role") && !meta.get("role").isJsonNull()) {
                        role = meta.get("role").getAsString();
                    }
                    if (meta.has("nearText") && !meta.get("nearText").isJsonNull()) {
                        nearText = meta.get("nearText").getAsString();
                    }
                    if (meta.has("description") && !meta.get("description").isJsonNull()) {
                        description = meta.get("description").getAsString();
                    }
                }

                selector = new Selector(Selector.Type.valueOf(selTypeStr.toUpperCase()), selVal, role, nearText, description);
            }
        }

        // Parse Value (nullable)
        String value = null;
        if (decision.has("value") && !decision.get("value").isJsonNull()) {
            value = decision.get("value").getAsString();
        }

        // If selector.type was URL, value should come from selector.value
        if (value == null && decision.has("selector") && !decision.get("selector").isJsonNull()) {
            JsonObject selObj = decision.getAsJsonObject("selector");
            if (selObj.has("type") && "URL".equalsIgnoreCase(selObj.get("type").getAsString()) && selObj.has("value")) {
                value = selObj.get("value").getAsString();
            }
        }

        return new Action(actionType, selector, value);
    }

    private void logMessageTail() {
        int start = Math.max(0, messages.size() - LLM_MAX_MESSAGES_TO_LOG);
        for (int i = start; i < messages.size(); i++) {
            JsonObject m = messages.get(i);
            String role = m.has("role") ? m.get("role").getAsString() : "?";
            String content = m.has("content") ? m.get("content").getAsString() : "";
            System.out.println(String.format("  - [%s] %s", role, oneLine(content, 240)));
        }
    }

    private static String oneLine(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        String cleaned = s.replace("\r", " ").replace("\n", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= maxLen) {
            return cleaned;
        }
        return cleaned.substring(0, maxLen) + "...";
    }

    private static String summarizeAction(Action action) {
        if (action == null) {
            return "<null>";
        }
        String selector = (action.getSelector() == null) ? "<none>" : action.getSelector().toString();
        String value = (action.getValue() == null) ? "<none>" : action.getValue();
        return "type=" + action.getType() + ", selector=" + selector + ", value=" + value;
    }

    private static String tryRecoverFailedGenerationJson(String errorMessage) {
        try {
            if (errorMessage == null) {
                return null;
            }
            int firstBrace = errorMessage.indexOf("{");
            if (firstBrace < 0) {
                return null;
            }
            String maybeJson = errorMessage.substring(firstBrace);
            JsonObject root = JsonParser.parseString(maybeJson).getAsJsonObject();
            if (!root.has("error")) {
                return null;
            }
            JsonObject err = root.getAsJsonObject("error");
            if (!err.has("failed_generation")) {
                return null;
            }
            String failed = err.get("failed_generation").getAsString();

            // If failed_generation is a tool-call-like wrapper, unwrap it.
            // Example: {"name":"type","arguments":{...}}
            try {
                var failedParsed = JsonParser.parseString(failed);
                if (failedParsed.isJsonObject()) {
                    JsonObject fo = failedParsed.getAsJsonObject();
                    if (fo.has("arguments") && fo.get("arguments").isJsonObject()) {
                        failed = fo.getAsJsonObject("arguments").toString();
                    }
                }
            } catch (Exception ignore) {
                // keep original failed string
            }

            // Wrap into a minimal "choices/message/content" response so existing parsing works.
            JsonObject fake = new JsonObject();
            JsonArray choices = new JsonArray();
            JsonObject choice0 = new JsonObject();
            JsonObject msg = new JsonObject();
            msg.addProperty("content", failed);
            choice0.add("message", msg);
            choices.add(choice0);
            fake.add("choices", choices);
            return fake.toString();
        } catch (Exception ignore) {
            return null;
        }
    }

    private String getSystemPrompt() {
        return "You are a UI Test Automation Agent. Your goal is to navigate a website and perform actions to achieve a user goal.\n" +
                "You must output STRICT JSON ONLY. No markdown, no explanations outside the JSON.\n" +
                "IMPORTANT: Output exactly ONE action JSON object (not an array).\n" +
                "IMPORTANT: Do NOT output tool/function-call wrappers like {\\\"name\\\":..., \\\"arguments\\\":...}.\n" +
                "IMPORTANT: The 'type' field MUST be one of the allowed action types listed below.\n" +
                "IMPORTANT: The selector.type field MUST be one of: CSS | XPATH | TEXT | ROLE | LABEL | PLACEHOLDER | TEST_ID. Do NOT use URL.\n" +
                "For NAVIGATE actions, put the URL in the top-level 'value' field and set selector to null.\n" +
                "\n" +
                "Output format:\n" +
                "{\n" +
                "  \"thought\": \"Brief reasoning about what to do next based on the last result\",\n" +
                "  \"type\": \"NAVIGATE | CLICK | TYPE | WAIT_FOR_VISIBLE | ASSERT_VISIBLE | ASSERT_TEXT | SCREENSHOT | SCROLL | SWITCH_TAB\",\n" +
                "  \"selector\": {\n" +
                "    \"type\": \"CSS | XPATH | TEXT | ROLE | LABEL | PLACEHOLDER | TEST_ID\",\n" +
                "    \"value\": \"selector value\",\n" +
                "    \"meta\": {\n" +
                "      \"role\": \"optional ARIA role like button, link, textbox, checkbox, etc.\",\n" +
                "      \"nearText\": \"optional nearby or surrounding text to help disambiguate\",\n" +
                "      \"description\": \"short natural-language description of the element if helpful\"\n" +
                "    }\n" +
                "  } (or null if not needed),\n" +
                "  \"value\": \"text to type or url or scroll amount or tab index\" (or null),\n" +
                "  \"isComplete\": boolean (true if goal achieved)\n" +
                "}\n" +
                "\n" +
                "Guidance on Selectors:\n" +
                "- Prefer semantic selectors (ROLE/LABEL/PLACEHOLDER/TEST_ID/TEXT). Avoid complex CSS/XPATH unless necessary.\n" +
                "- If multiple elements match a TEXT selector, prefer the element that is a BUTTON or LINK.\n" +
                "\n" +
                "Step-by-step logic:\n" +
                "1. Always NAVIGATE first if history is empty.\n" +
                "2. If a page load is expected, use WAIT_FOR_VISIBLE before interacting.\n" +
                "3. If a popup or new tab opens (e.g., SSO), use SWITCH_TAB.\n" +
                "\n";
    }
}
