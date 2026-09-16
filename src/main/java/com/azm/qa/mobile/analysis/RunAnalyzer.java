package com.azm.qa.mobile.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Deterministic, credential-free diagnosis of one Allure results directory — the mobile twin
 * of the web framework's {@code analyze-failures.mjs}. Reads {@code *-result.json} files
 * (Allure's per-test result format) and produces categories, a systemic-vs-isolated read, and
 * flags anomalies. No model, no API key: this is the analysis every run gets, whether or not
 * AI narration is available.
 */
public final class RunAnalyzer {

    private final ObjectMapper mapper = new ObjectMapper();

    public Report analyze(Path allureResultsDir, Path healLedgerPath) {
        List<JsonNode> results = readResultFiles(allureResultsDir);
        List<TestOutcome> outcomes = results.stream().map(this::toOutcome).collect(Collectors.toList());

        long total = outcomes.size();
        long failed = outcomes.stream().filter(o -> o.status.equals("failed") || o.status.equals("broken")).count();
        long healed = outcomes.stream().filter(o -> o.healed).count();
        long interrupted = outcomes.stream().filter(o -> o.interrupted).count();

        Map<String, Long> failuresByCategory = outcomes.stream()
                .filter(o -> o.status.equals("failed") || o.status.equals("broken"))
                .collect(Collectors.groupingBy(this::categorize, Collectors.counting()));

        boolean systemic = total > 0 && failed > total / 2;

        List<String> anomalies = new ArrayList<>();
        if (total > 0) {
            double failureRate = (double) failed / total;
            if (failureRate > 0.5) {
                anomalies.add(String.format(java.util.Locale.ROOT,
                        "Failure rate %.0f%% is over half the suite -- looks systemic (env/build), not scattered flake.",
                        failureRate * 100));
            }
            double healRate = (double) healed / total;
            if (healRate > 0.2) {
                anomalies.add(String.format(java.util.Locale.ROOT,
                        "%.0f%% of tests healed this run -- a spike this size usually means a real UI change landed, not incidental drift.",
                        healRate * 100));
            }
        }
        if (interrupted > 0) {
            anomalies.add(interrupted + " test(s) were interrupted by an ad/dialog this run -- see the interference guard section of the report.");
        }

        long healEventsInLedger = countLedgerLines(healLedgerPath);

        return new Report(total, failed, healed, interrupted, systemic, failuresByCategory, anomalies, healEventsInLedger);
    }

    private String categorize(TestOutcome outcome) {
        String message = outcome.statusMessage == null ? "" : outcome.statusMessage.toLowerCase(java.util.Locale.ROOT);
        if (outcome.healed) {
            return "self-heal-related";
        }
        if (message.contains("nosuchelement") || (message.contains("timeout") && message.contains("visib"))) {
            return "locator-or-timing";
        }
        if (message.contains("sessionnotcreated") || message.contains("could not create") || message.contains("invalid session id")) {
            return "environment";
        }
        if (message.contains("adactivity") || outcome.interrupted) {
            return "app-interference";
        }
        if (outcome.status.equals("broken")) {
            return "framework-error";
        }
        return "assertion-failure";
    }

    private List<JsonNode> readResultFiles(Path dir) {
        List<JsonNode> nodes = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return nodes;
        }
        try (var stream = Files.list(dir)) {
            for (Path file : stream.filter(p -> p.getFileName().toString().endsWith("-result.json")).toList()) {
                try {
                    nodes.add(mapper.readTree(file.toFile()));
                } catch (IOException ignored) {
                    // A malformed result file shouldn't crash analysis of everything else.
                }
            }
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("Could not list Allure results at " + dir, e);
        }
        return nodes;
    }

    private TestOutcome toOutcome(JsonNode node) {
        String status = node.path("status").asText("unknown");
        String statusMessage = node.path("statusDetails").path("message").asText(null);
        boolean healed = labelEquals(node, "healed", "true");
        boolean interrupted = labelEquals(node, "interrupted", "true");
        return new TestOutcome(status, statusMessage, healed, interrupted);
    }

    private boolean labelEquals(JsonNode result, String name, String value) {
        for (JsonNode label : result.path("labels")) {
            if (label.path("name").asText("").equals(name) && label.path("value").asText("").equals(value)) {
                return true;
            }
        }
        return false;
    }

    private long countLedgerLines(Path ledgerPath) {
        if (ledgerPath == null || !Files.isRegularFile(ledgerPath)) {
            return 0;
        }
        try {
            return Files.readAllLines(ledgerPath).stream().filter(l -> !l.isBlank()).count();
        } catch (IOException e) {
            return 0;
        }
    }

    private record TestOutcome(String status, String statusMessage, boolean healed, boolean interrupted) {
    }

    public record Report(
            long totalTests,
            long failedTests,
            long healedTests,
            long interruptedTests,
            boolean looksSystemic,
            Map<String, Long> failuresByCategory,
            List<String> anomalies,
            long totalHealEventsInLedger) {

        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append(totalTests).append(" test(s), ").append(failedTests).append(" failed, ")
                    .append(healedTests).append(" healed, ").append(interruptedTests).append(" interrupted.\n");
            if (looksSystemic) {
                sb.append("Failure pattern looks SYSTEMIC (environment/build), not scattered flake.\n");
            }
            failuresByCategory.forEach((category, count) ->
                    sb.append("  - ").append(category).append(": ").append(count).append('\n'));
            anomalies.forEach(a -> sb.append("ANOMALY: ").append(a).append('\n'));
            sb.append("Heal ledger has ").append(totalHealEventsInLedger).append(" event(s) total (all-time).\n");
            return sb.toString();
        }
    }
}
