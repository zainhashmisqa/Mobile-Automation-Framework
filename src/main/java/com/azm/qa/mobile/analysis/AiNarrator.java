package com.azm.qa.mobile.analysis;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds one AI-narrated paragraph on top of {@link RunAnalyzer}'s deterministic report — the
 * mobile twin of the web framework's {@code ai-narrate-failures.mjs}, and the ONLY place in
 * this entire framework a billed API call can happen.
 *
 * <p><b>Silently skipped whenever {@code ANTHROPIC_API_KEY} is not set.</b> Every other
 * component — self-healing, codegen, reporting, CI — works completely without it, which is
 * how the "no billed API key required anywhere" guarantee stays true while still leaving room
 * for a nicer narrative when a key happens to be configured. No dependency on the Anthropic
 * SDK either: a plain HTTP call keeps this class's absence-of-a-key path trivially free of
 * side effects to reason about.
 */
public final class AiNarrator {

    private static final Logger log = LoggerFactory.getLogger(AiNarrator.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-5";

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    /** Empty when no key is configured, the call fails, or anything else goes wrong -- never throws. */
    public Optional<String> narrate(RunAnalyzer.Report report) {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.info("ANTHROPIC_API_KEY not set -- skipping AI narration (deterministic report above is unaffected)");
            return Optional.empty();
        }

        try {
            String prompt = "One short paragraph, plain language, for a QA lead skimming a CI run. "
                    + "Given this deterministic test-run analysis, say what most likely happened and "
                    + "what to look at first. Do not invent facts not in the data.\n\n" + report.summary();

            String body = "{"
                    + "\"model\":\"" + MODEL + "\","
                    + "\"max_tokens\":300,"
                    + "\"messages\":[{\"role\":\"user\",\"content\":" + jsonString(prompt) + "}]"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("AI narration request failed with HTTP {} -- continuing without it", response.statusCode());
                return Optional.empty();
            }
            return Optional.of(extractText(response.body()));
        } catch (Exception e) {
            log.warn("AI narration failed ({}) -- continuing without it", e.getMessage());
            return Optional.empty();
        }
    }

    /** Minimal, dependency-free extraction of the first text block's content from the Messages API response. */
    private static String extractText(String responseJson) {
        int contentIdx = responseJson.indexOf("\"text\"");
        if (contentIdx < 0) {
            return responseJson;
        }
        int colon = responseJson.indexOf(':', contentIdx);
        int firstQuote = responseJson.indexOf('"', colon + 1);
        int secondQuote = firstQuote;
        do {
            secondQuote = responseJson.indexOf('"', secondQuote + 1);
        } while (secondQuote > 0 && responseJson.charAt(secondQuote - 1) == '\\');
        if (firstQuote < 0 || secondQuote < 0) {
            return responseJson;
        }
        return responseJson.substring(firstQuote + 1, secondQuote)
                .replace("\\n", "\n").replace("\\\"", "\"");
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
