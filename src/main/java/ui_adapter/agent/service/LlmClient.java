package ui_adapter.agent.service;

/**
 * Thin abstraction over an LLM provider so the agent can switch between Groq and Gemini.
 */
public interface LlmClient {
    /**
     * Executes a chat/completions style request using the agent's existing message history
     * and returns a provider-specific raw JSON response string.
     */
    String chatCompletions(String requestJson) throws Exception;

    /** Display name for logging/debugging. */
    String providerName();
}

