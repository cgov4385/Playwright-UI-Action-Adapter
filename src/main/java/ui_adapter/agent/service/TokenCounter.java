package ui_adapter.agent.service;

import java.nio.charset.StandardCharsets;

/**
 * Lightweight token estimation.
 *
 * This is NOT an exact tokenizer for Groq/OpenAI/Gemini models; it's a readable approximation
 * good enough for rough cost estimation.
 */
public final class TokenCounter {

    private TokenCounter() {
    }

    /**
     * Heuristic: ~4 characters per token in English.
     * Uses UTF-8 byte length as a proxy for characters.
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int bytes = text.getBytes(StandardCharsets.UTF_8).length;
        return Math.max(1, (int) Math.ceil(bytes / 4.0));
    }

    public static String humanReadable(int tokens) {
        if (tokens < 1000) {
            return tokens + " tokens";
        }
        double k = tokens / 1000.0;
        return String.format("%.2fK tokens (%d)", k, tokens);
    }
}

