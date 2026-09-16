package com.azm.qa.mobile.selfscore.criteria;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.azm.qa.mobile.selfscore.Criterion;
import com.azm.qa.mobile.selfscore.CriterionScore;
import com.azm.qa.mobile.selfscore.RepoScan;

/**
 * Mobile mapping of the deck's "DOM" axis: since there's no DOM on native/hybrid mobile, this
 * checks locator-strategy discipline instead -- how much of the codebase resorts to fragile
 * xpath, and whether an explicit "never invent a locator" convention is documented and
 * enforced (an honest TODO beats a guessed locator).
 */
public final class LocatorStrategyCriterion implements Criterion {

    private static final Pattern XPATH_LOCATOR = Pattern.compile("\\.xpath\\s*\\(");
    private static final Pattern STABLE_LOCATOR =
            Pattern.compile("(accessibilityId|resourceId|\\.id\\s*\\()\\s*\\(");
    private static final Pattern TODO_CODEGEN = Pattern.compile("TODO\\(codegen\\)");

    @Override
    public String name() {
        return "Locator-strategy discipline";
    }

    @Override
    public String mappedFrom() {
        return "DOM";
    }

    @Override
    public CriterionScore evaluate(Path repoRoot) {
        List<String> evidence = new ArrayList<>();
        double score = 0;

        List<Path> allFiles = RepoScan.javaFiles(repoRoot, "src/main/java", "src/test/java");
        int xpathCount = 0;
        int stableCount = 0;
        for (Path f : allFiles) {
            String stripped = RepoScan.readQuietly(f).map(RepoScan::stripComments).orElse("");
            xpathCount += RepoScan.countMatches(stripped, XPATH_LOCATOR);
            stableCount += RepoScan.countMatches(stripped, STABLE_LOCATOR);
        }

        int total = xpathCount + stableCount;
        if (total == 0) {
            evidence.add("No locator constructions (xpath/accessibilityId/resourceId/id) found at all.");
        } else {
            double stableRate = (double) stableCount / total;
            score += stableRate * 6;
            evidence.add(stableCount + " stable locator(s) (accessibility-id/resource-id) vs " + xpathCount
                    + " xpath locator(s) -- " + Math.round(stableRate * 100) + "% stable.");
        }

        boolean docsNeverInvent = RepoScan.readQuietly(repoRoot.resolve("CLAUDE.md"))
                .map(s -> s.toLowerCase(java.util.Locale.ROOT).contains("never invent a locator"))
                .orElse(false);
        if (docsNeverInvent) {
            score += 2;
            evidence.add("CLAUDE.md explicitly documents a 'never invent a locator' rule.");
        }

        int todoCount = 0;
        for (Path f : RepoScan.javaFiles(repoRoot, "src/test/java")) {
            todoCount += RepoScan.countMatches(RepoScan.readQuietly(f).orElse(""), TODO_CODEGEN);
        }
        if (todoCount > 0) {
            score += 2;
            evidence.add(todoCount + " honest TODO(codegen) marker(s) found -- unresolved locators are flagged, not guessed.");
        }

        String verdict = score >= 8 ? "Locator strategy favors stable identifiers and is enforced honestly."
                : score >= 4 ? "A mix of stable and fragile locators, with limited discipline."
                : "Locator strategy is fragile or undocumented.";
        return new CriterionScore(Math.min(10, score), evidence, verdict);
    }
}
