package ui_adapter.agent.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Appends LLM usage (prompt/response token estimates, provider, model, latency, timestamp) to a text file.
 */
public final class LlmUsageLogger {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    private LlmUsageLogger() {
    }

    public static void log(Path file,
                           String provider,
                           String model,
                           int promptTokens,
                           int responseTokens,
                           long latencyMs) {
        try {
            ensureParent(file);
            String line = formatLine(provider, model, promptTokens, responseTokens, latencyMs);
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException ignore) {
            // best-effort logging only
        }
    }

    private static void ensureParent(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static String formatLine(String provider,
                                     String model,
                                     int promptTokens,
                                     int responseTokens,
                                     long latencyMs) {
        return String.format(
                "%s | provider=%s | model=%s | prompt=%s | response=%s | total=%s | latency=%dms",
                TS.format(Instant.now()),
                safe(provider),
                safe(model),
                TokenCounter.humanReadable(promptTokens),
                TokenCounter.humanReadable(responseTokens),
                TokenCounter.humanReadable(promptTokens + responseTokens),
                latencyMs
        );
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "<unknown>" : s;
    }
}

