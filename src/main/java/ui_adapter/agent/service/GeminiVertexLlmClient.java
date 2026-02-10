package ui_adapter.agent.service;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Gemini implementation using Vertex AI (REST) with Google Auth.
 *
 * Auth (ADC):
 *  - GOOGLE_APPLICATION_CREDENTIALS or gcloud auth application-default login
 * Optional:
 *  - GOOGLE_APPLICATION_CREDENTIALS_JSON: path to a service account JSON
 *
 * Config:
 *  - VERTEX_PROJECT / GOOGLE_CLOUD_PROJECT
 *  - VERTEX_LOCATION / GOOGLE_CLOUD_LOCATION (e.g. us-central1)
 *  - GEMINI_MODEL (e.g. gemini-1.5-pro)
 */
public class GeminiVertexLlmClient implements LlmClient {

    private final String projectId;
    private final String location;
    private final String model;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public GeminiVertexLlmClient(String projectId, String location, String model) {
        this.projectId = projectId;
        this.location = location;
        this.model = model;
    }

    @Override
    public String chatCompletions(String requestJson) throws Exception {
        JsonObject req = JsonParser.parseString(requestJson).getAsJsonObject();

        double temperature = 0.0;
        if (req.has("temperature")) {
            try {
                temperature = req.get("temperature").getAsDouble();
            } catch (Exception ignore) {
            }
        }

        String systemPrompt = extractSystemPrompt(req);
        String prompt = flattenMessages(req);

        String vertexText = callVertexGenerateContent(systemPrompt, prompt, temperature);

        // Convert to OpenAI-like shape so the existing parser keeps working.
        JsonObject fake = new JsonObject();
        JsonArray choices = new JsonArray();
        JsonObject choice0 = new JsonObject();
        JsonObject msg = new JsonObject();
        msg.addProperty("content", vertexText == null ? "" : vertexText);
        choice0.add("message", msg);
        choices.add(choice0);
        fake.add("choices", choices);
        return fake.toString();
    }

    @Override
    public String providerName() {
        return "gemini-vertex";
    }

    private String callVertexGenerateContent(String systemPrompt, String prompt, double temperature) throws Exception {
        String url = String.format(
                "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
                location, projectId, location, model
        );

        JsonObject body = new JsonObject();

        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        JsonArray parts = new JsonArray();

        StringBuilder text = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            text.append(systemPrompt).append("\n\n");
        }
        text.append(prompt);

        JsonObject part0 = new JsonObject();
        part0.addProperty("text", text.toString());
        parts.add(part0);
        userContent.add("parts", parts);
        contents.add(userContent);
        body.add("contents", contents);

        JsonObject genCfg = new JsonObject();
        genCfg.addProperty("temperature", temperature);
        body.add("generationConfig", genCfg);

        String accessToken = getAccessToken();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("Vertex API Error: " + response.statusCode() + " " + response.body());
        }

        // Response shape: { candidates: [ { content: { parts:[{text:"..."}] } } ] }
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!root.has("candidates") || !root.get("candidates").isJsonArray() || root.getAsJsonArray("candidates").isEmpty()) {
            return "";
        }
        JsonObject cand0 = root.getAsJsonArray("candidates").get(0).getAsJsonObject();
        if (!cand0.has("content")) {
            return "";
        }
        JsonObject content = cand0.getAsJsonObject("content");
        if (!content.has("parts") || !content.get("parts").isJsonArray() || content.getAsJsonArray("parts").isEmpty()) {
            return "";
        }
        JsonObject p0 = content.getAsJsonArray("parts").get(0).getAsJsonObject();
        return p0.has("text") ? p0.get("text").getAsString() : "";
    }

    private static String getAccessToken() throws Exception {
        GoogleCredentials creds = loadCredentials().createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        creds.refreshIfExpired();
        AccessToken token = creds.getAccessToken();
        if (token == null || token.getTokenValue() == null) {
            throw new IllegalStateException("Unable to obtain Google access token via ADC.");
        }
        return token.getTokenValue();
    }

    private static GoogleCredentials loadCredentials() throws Exception {
        String credPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS_JSON");
        if (credPath != null && !credPath.isBlank()) {
            return GoogleCredentials.fromStream(new FileInputStream(credPath));
        }
        return GoogleCredentials.getApplicationDefault();
    }

    private static String extractSystemPrompt(JsonObject req) {
        if (!req.has("messages") || !req.get("messages").isJsonArray()) {
            return null;
        }
        for (var el : req.getAsJsonArray("messages")) {
            if (!el.isJsonObject()) continue;
            JsonObject m = el.getAsJsonObject();
            if (m.has("role") && "system".equalsIgnoreCase(m.get("role").getAsString())) {
                return m.has("content") ? m.get("content").getAsString() : null;
            }
        }
        return null;
    }

    private static String flattenMessages(JsonObject req) {
        if (!req.has("messages") || !req.get("messages").isJsonArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (var el : req.getAsJsonArray("messages")) {
            if (!el.isJsonObject()) continue;
            JsonObject m = el.getAsJsonObject();
            String role = m.has("role") ? m.get("role").getAsString() : "";
            if ("system".equalsIgnoreCase(role)) {
                continue;
            }
            String content = m.has("content") && !m.get("content").isJsonNull() ? m.get("content").getAsString() : "";
            sb.append(role).append(": ").append(content).append("\n");
        }
        return sb.toString().trim();
    }
}
