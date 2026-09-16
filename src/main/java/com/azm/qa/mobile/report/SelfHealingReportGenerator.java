package com.azm.qa.mobile.report;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.azm.qa.mobile.healing.HealEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Turns the real heal ledger ({@code self-healing-data/heal-events.jsonl}) into a standalone,
 * human-readable static report -- the "what actually broke and what actually recovered it"
 * view that Allure's own per-test attachments don't summarize in one place.
 *
 * <p><b>Honesty, not marketing:</b> every heal here is deterministic -- fingerprint scoring
 * plus {@code StrategyCascade}'s 9 ranked strategies, computed in-process. Nothing in this
 * pipeline calls a model at runtime (see {@code CLAUDE.md}: "Runtime is deterministic"). This
 * report says so explicitly rather than letting "self-healing" read as "AI-healing" by
 * omission -- the exact overclaim the web-framework assessment deck flags other repos for.
 */
public final class SelfHealingReportGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final ObjectMapper mapper = new ObjectMapper();

    public String generate(Path ledgerPath) {
        List<HealEvent> events = readLedger(ledgerPath);
        return render(events);
    }

    public void generateToFile(Path ledgerPath, Path outputHtmlFile) {
        String html = generate(ledgerPath);
        try {
            Path parent = outputHtmlFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputHtmlFile, html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write self-healing report to " + outputHtmlFile, e);
        }
    }

    private List<HealEvent> readLedger(Path ledgerPath) {
        List<HealEvent> events = new ArrayList<>();
        if (ledgerPath == null || !Files.isRegularFile(ledgerPath)) {
            return events;
        }
        try {
            for (String line : Files.readAllLines(ledgerPath, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    events.add(mapper.readValue(line, HealEvent.class));
                } catch (IOException malformed) {
                    // One bad line shouldn't hide every other real event.
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read heal ledger at " + ledgerPath, e);
        }
        return events;
    }

    private String render(List<HealEvent> events) {
        int total = events.size();
        double avgConfidence = events.stream().mapToDouble(HealEvent::confidence).average().orElse(0);
        long distinctLocators = events.stream().map(HealEvent::description).distinct().count();
        long distinctTests = events.stream()
                .map(e -> e.testClass() + "#" + e.testMethod()).distinct().count();

        StringBuilder rows = new StringBuilder();
        for (HealEvent e : events) {
            rows.append("<tr>")
                    .append("<td>").append(escape(TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(e.timestamp())))).append("</td>")
                    .append("<td>").append(escape(e.testClass())).append("<br><span class=\"method\">")
                    .append(escape(e.testMethod())).append("</span></td>")
                    .append("<td>").append(escape(e.description())).append("</td>")
                    .append("<td class=\"strategy broken\">").append(escape(e.originalStrategy())).append("</td>")
                    .append("<td class=\"strategy recovered\">").append(escape(e.healedStrategy())).append("</td>")
                    .append("<td>").append(String.format(Locale.ROOT, "%.2f", e.confidence())).append("</td>")
                    .append("</tr>\n");
        }

        String emptyState = total == 0
                ? "<p class=\"empty\">No heal events recorded yet -- every locator resolved on its declared, "
                + "primary strategy with no cascade fallback needed.</p>"
                : "";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                <meta charset="utf-8">
                <title>Self-Healing Report</title>
                <style>
                  body { font-family: -apple-system, Segoe UI, Roboto, sans-serif; margin: 0; padding: 24px;
                         background: #0f1115; color: #e6e6e6; }
                  h1 { margin-top: 0; }
                  .banner { background: #1b2230; border: 1px solid #2c3648; border-radius: 8px;
                            padding: 14px 18px; margin-bottom: 24px; font-size: 14px; line-height: 1.5; }
                  .banner b { color: #7fd0ff; }
                  .stats { display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }
                  .stat { background: #1b2230; border: 1px solid #2c3648; border-radius: 8px;
                          padding: 12px 18px; min-width: 140px; }
                  .stat .value { font-size: 26px; font-weight: 700; }
                  .stat .label { font-size: 12px; color: #9aa4b2; text-transform: uppercase; letter-spacing: .04em; }
                  table { width: 100%%; border-collapse: collapse; font-size: 13px; }
                  th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #2c3648; vertical-align: top; }
                  th { color: #9aa4b2; font-weight: 600; text-transform: uppercase; font-size: 11px; letter-spacing: .04em; }
                  .method { color: #9aa4b2; font-size: 12px; }
                  .strategy.broken { color: #ff8a8a; }
                  .strategy.recovered { color: #8affb0; }
                  .empty { color: #9aa4b2; }
                </style>
                </head>
                <body>
                <h1>Self-Healing Report</h1>
                <div class="banner">
                  <b>Deterministic, not AI.</b> Every recovery below comes from fingerprint scoring plus a
                  9-strategy cascade computed in-process -- no model call happens at runtime. Only
                  actions (tap/type) ever heal; assertions always use their raw, declared locator.
                </div>
                <div class="stats">
                  <div class="stat"><div class="value">%d</div><div class="label">Heal events</div></div>
                  <div class="stat"><div class="value">%d</div><div class="label">Distinct locators healed</div></div>
                  <div class="stat"><div class="value">%d</div><div class="label">Tests involved</div></div>
                  <div class="stat"><div class="value">%s</div><div class="label">Avg. confidence</div></div>
                </div>
                %s
                <table>
                <thead><tr><th>When</th><th>Test</th><th>Element</th><th>Broken strategy</th><th>Recovered via</th><th>Confidence</th></tr></thead>
                <tbody>
                %s
                </tbody>
                </table>
                </body>
                </html>
                """.formatted(total, distinctLocators, distinctTests,
                total == 0 ? "n/a" : String.format(Locale.ROOT, "%.2f", avgConfidence),
                emptyState, rows);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
