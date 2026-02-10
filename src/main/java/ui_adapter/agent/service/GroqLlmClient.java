package ui_adapter.agent.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Groq implementation using OpenAI-compatible HTTP endpoint.
 */
public class GroqLlmClient implements LlmClient {

    private static final String DEFAULT_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final HttpClient httpClient;
    private final String apiUrl;
    private final String apiKey;

    public GroqLlmClient(HttpClient httpClient, String apiKey) {
        this(httpClient, apiKey, DEFAULT_API_URL);
    }

    public GroqLlmClient(HttpClient httpClient, String apiKey, String apiUrl) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    @Override
    public String chatCompletions(String requestJson) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("GROQ_API_KEY environment variable is missing!");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("API Error: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    @Override
    public String providerName() {
        return "groq";
    }
}

