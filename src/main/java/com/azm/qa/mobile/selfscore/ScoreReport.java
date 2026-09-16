package com.azm.qa.mobile.selfscore;

import java.util.LinkedHashMap;
import java.util.Map;

/** One repo's full scorecard: every criterion's result, plus the overall average. */
public record ScoreReport(String repoLabel, Map<Criterion, CriterionScore> byCriterion, double overall) {

    public static ScoreReport of(String repoLabel, Map<Criterion, CriterionScore> byCriterion) {
        double overall = byCriterion.values().stream().mapToDouble(CriterionScore::score).average().orElse(0);
        return new ScoreReport(repoLabel, new LinkedHashMap<>(byCriterion), Math.round(overall * 10) / 10.0);
    }

    public String toConsoleTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(repoLabel).append(" -- overall ").append(overall).append("/10 ===\n");
        byCriterion.forEach((criterion, cs) -> {
            sb.append(String.format(java.util.Locale.ROOT, "%-38s %4.1f/10  (was: %s)  %s%n",
                    criterion.name(), cs.score(), criterion.mappedFrom(), cs.verdict()));
        });
        return sb.toString();
    }

    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# selfscore -- ").append(repoLabel).append("\n\n");
        sb.append("**Overall: ").append(overall).append("/10**\n\n");
        sb.append("| Criterion | Web-deck axis | Score | Verdict |\n|---|---|---|---|\n");
        byCriterion.forEach((criterion, cs) -> sb.append("| ").append(criterion.name())
                .append(" | ").append(criterion.mappedFrom())
                .append(" | ").append(oneDecimal(cs.score()))
                .append(" | ").append(cs.verdict()).append(" |\n"));

        sb.append("\n## Evidence\n\n");
        byCriterion.forEach((criterion, cs) -> {
            sb.append("### ").append(criterion.name())
                    .append(" (").append(oneDecimal(cs.score())).append("/10)\n\n");
            cs.evidence().forEach(e -> sb.append("- ").append(e).append('\n'));
            sb.append('\n');
        });
        return sb.toString();
    }

    private static String oneDecimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
