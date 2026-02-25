package ui_adapter.agent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ui_adapter.model.Action;
import ui_adapter.model.ActionResult;
import ui_adapter.model.Selector;
import ui_adapter.agent.service.GeminiVertexLlmClient;
import ui_adapter.agent.service.GroqLlmClient;
import ui_adapter.agent.service.LlmClient;
import ui_adapter.agent.service.LlmUsageLogger;
import ui_adapter.agent.service.TokenCounter;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    // Provider selection: groq | gemini-vertex
    private static final String LLM_PROVIDER = getConfig("LLM_PROVIDER", "gemini-vertex");

    private static final String GROQ_MODEL = getConfig("GROQ_MODEL", "openai/gpt-oss-safeguard-20b");

    private static final String VERTEX_PROJECT = getConfigAny(new String[]{"VERTEX_PROJECT", "GOOGLE_CLOUD_PROJECT"}, "");
    private static final String VERTEX_LOCATION = getConfigAny(new String[]{"VERTEX_LOCATION", "GOOGLE_CLOUD_LOCATION"}, "us-central1");
    private static final String GEMINI_MODEL = getConfig("GEMINI_MODEL", "gemini-2.5-flash");

    // NOTE: In production, do not hardcode keys. Use environment variables.
    private static final String GROQ_API_KEY = getApiKey();

    private final String goal;
    private final List<JsonObject> messages;
    private final Gson gson;
    private final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    private final HttpClient client;
    private final ExecutorService executorService;
    private final LlmClient llmClient;

    private boolean isComplete = false;
    private int consecutiveFailures = 0;
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final boolean LLM_WIRE_LOG_ENABLED = true;
    private static final int LLM_MAX_MESSAGES_TO_LOG = 6;
    private static final boolean LLM_USAGE_LOG_ENABLED = Boolean.parseBoolean(getConfig("LLM_USAGE_LOG_ENABLED", "true"));
    private static final String LLM_USAGE_LOG_FILE = getConfig("LLM_USAGE_LOG_FILE", "llm-usage.txt");

    /** Only send the most recent execution messages to the LLM to reduce tokens. */
    private static final int LLM_MAX_EXECUTION_MESSAGES_TO_SEND = Integer.parseInt(getConfig("LLM_MAX_EXECUTION_MESSAGES_TO_SEND", "10"));

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

    public LlmAgent(String goal) {
        this.goal = goal;
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
                jsonResponse = callLlm();
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

    private String callLlm() throws Exception {
        JsonObject requestBody = new JsonObject();

        // Keep OpenAI-chat compatible shape for Groq; Gemini client will adapt it.
        String provider = llmClient.providerName();
        if (provider.startsWith("groq")) {
            requestBody.addProperty("model", GROQ_MODEL);
        } else {
            // Not used by Vertex SDK, but keep it for logging/debug.
            requestBody.addProperty("model", GEMINI_MODEL);
        }

        requestBody.addProperty("temperature", 0.0);

        // Build a trimmed message list: keep system + goal, plus only the last N execution messages.
        JsonArray messagesArray = new JsonArray();
        for (JsonObject m : buildMessagesForRequest()) {
            messagesArray.add(m);
        }
        requestBody.add("messages", messagesArray);

        // Groq supports response_format; Gemini will ignore.
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        requestBody.add("response_format", responseFormat);

        String jsonBody = gson.toJson(requestBody);

        int promptTokensEst = 0;
        if (LLM_USAGE_LOG_ENABLED) {
            // Estimate tokens based on the exact request payload being sent.
            promptTokensEst = TokenCounter.estimateTokens(jsonBody);
        }

        long startNs = System.nanoTime();
        String raw = llmClient.chatCompletions(jsonBody);
        long latencyMs = (System.nanoTime() - startNs) / 1_000_000;

        if (LLM_USAGE_LOG_ENABLED) {
            int responseTokensEst = TokenCounter.estimateTokens(raw);
            Path out = Paths.get(LLM_USAGE_LOG_FILE);
            String model = requestBody.has("model") ? requestBody.get("model").getAsString() : null;
            LlmUsageLogger.log(out, llmClient.providerName(), model, promptTokensEst, responseTokensEst, latencyMs);
        }

        if (LLM_WIRE_LOG_ENABLED) {
            System.out.println("\n==================================================");
            System.out.println("[LLM] RESPONSE");
            System.out.println("  provider : " + llmClient.providerName());
            System.out.println("\n[LLM] RAW RESPONSE JSON:");
            try {
                System.out.println(prettyGson.toJson(JsonParser.parseString(raw)));
            } catch (Exception ignore) {
                System.out.println(raw);
            }
            System.out.println("==================================================");
        }

        return raw;
    }

    private List<JsonObject> buildMessagesForRequest() {
        // Expected layout in 'messages':
        // [0]=system, [1]=goal(user), then alternating user ACTION_RESULT and assistant ACTION decisions
        if (messages.size() <= 2) {
            return messages;
        }

        int keepExecution = Math.max(0, LLM_MAX_EXECUTION_MESSAGES_TO_SEND);
        int start = Math.max(2, messages.size() - keepExecution);

        List<JsonObject> out = new ArrayList<>();
        // Always include system + goal
        out.add(messages.get(0));
        out.add(messages.get(1));
        // Include tail execution history
        for (int i = start; i < messages.size(); i++) {
            out.add(messages.get(i));
        }
        return out;
    }

    private Action parseLlmResponse(String jsonResponse) {
        JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
        JsonArray choices = root.getAsJsonArray("choices");
        String content = choices.get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        // Gemini (and some other providers) may wrap JSON in markdown fences.
        String cleaned = extractJsonPayload(content);

        var parsed = JsonParser.parseString(cleaned);
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

    private static String extractJsonPayload(String content) {
        if (content == null) {
            return "{}";
        }
        String s = content.trim();

        // Strip markdown fences if present
        if (s.startsWith("```")) {
            // Remove leading ```json or ```
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) {
                s = s.substring(firstNewline + 1);
            }
            // Remove trailing ```
            int lastFence = s.lastIndexOf("```");
            if (lastFence >= 0) {
                s = s.substring(0, lastFence);
            }
            s = s.trim();
        }

        // Best-effort: extract the first JSON object/array substring.
        int obj = s.indexOf('{');
        int arr = s.indexOf('[');
        int start;
        if (obj < 0) {
            start = arr;
        } else if (arr < 0) {
            start = obj;
        } else {
            start = Math.min(obj, arr);
        }

        if (start > 0) {
            s = s.substring(start).trim();
        }

        // Trim any leading/trailing junk after the closing brace/bracket (common with chatty models)
        int endObj = s.lastIndexOf('}');
        int endArr = s.lastIndexOf(']');
        int end = Math.max(endObj, endArr);
        if (end >= 0 && end + 1 < s.length()) {
            s = s.substring(0, end + 1).trim();
        }
        return s;
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
                "  \\\"thought\\\": \\\"Brief reasoning about what to do next based on the last result\\\",\n" +
                "  \\\"type\\\": \\\"NAVIGATE | CLICK | TYPE | WAIT | WAIT_FOR_VISIBLE | ASSERT_VISIBLE | ASSERT_TEXT | SCREENSHOT | SCROLL | SWITCH_TAB | MAXIMIZE_WINDOW | CLICK_CHECKBOX | CLOSE_TAB\\\",\n" +
                "  \\\"selector\\\": {\n" +
                "    \\\"type\\\": \\\"CSS | XPATH | TEXT | ROLE | LABEL | PLACEHOLDER | TEST_ID\\\",\n" +
                "    \\\"value\\\": \\\"selector value\\\",\n" +
                "    \\\"meta\\\": {\n" +
                "      \\\"role\\\": \\\"optional ARIA role like button, link, textbox, checkbox, etc.\\\",\n" +
                "      \\\"nearText\\\": \\\"optional nearby or surrounding text to help disambiguate\\\",\n" +
                "      \\\"description\\\": \\\"short natural-language description of the element if helpful\\\"\n" +
                "    }\n" +
                "  } (or null if not needed),\n" +
                "  \\\"value\\\": \\\"text to type or url or scroll amount or tab index or viewport size (e.g. 1920,1080)\\\" (or null),\n" +
                "  \\\"isComplete\\\": boolean (true if goal achieved)\n" +
                "}\n" +
                "\n" +
                "Action Details:\n" +
                "- WAIT: Static delay/sleep. Set 'value' to number of seconds (e.g., \\\"3\\\", \\\"10\\\"). Default is 5 seconds if value is null. Selector should be null.\n" +
                "  Use when: Test explicitly says 'wait few seconds' or 'wait N seconds' or when a brief pause is needed for background processing.\n" +
                "- WAIT_FOR_VISIBLE: Dynamic wait for an element to become visible. Requires a selector.\n" +
                "  Use when: Waiting for a specific element to appear before proceeding.\n" +
                "\n" +
                "Guidance on Selectors:\n" +
                "- Prefer semantic selectors (ROLE/LABEL/PLACEHOLDER/TEST_ID/TEXT). Avoid complex CSS/XPATH unless necessary.\n" +
                "- If multiple elements match a TEXT selector, prefer the element that is a BUTTON or LINK.\n" +
                "\n" +
                "Step-by-step logic:\n" +
                "1. Always NAVIGATE first if history is empty.\n" +
                "2. If a page load is expected, use WAIT_FOR_VISIBLE before interacting.\n" +
                "3. Use WAIT for static delays when the test requires waiting a few seconds (default: 5 seconds, or specify seconds in value field).\n" +
                "4. If a popup or new tab opens (e.g., SSO), use SWITCH_TAB.\n" +
                "5. Use MAXIMIZE_WINDOW early if the test wants full-screen.\n" +
                "6. Use CLICK_CHECKBOX when the target is a checkbox/toggle that must be enabled (it is idempotent).\n" +
                "7. Use CLOSE_TAB to close the current tab; optionally set value to a tab index to close.\n" +
                "\n";
    }
}
