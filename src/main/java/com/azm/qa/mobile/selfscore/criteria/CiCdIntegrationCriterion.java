package com.azm.qa.mobile.selfscore.criteria;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.azm.qa.mobile.selfscore.Criterion;
import com.azm.qa.mobile.selfscore.CriterionScore;
import com.azm.qa.mobile.selfscore.RepoScan;

/** Is CI/CD actually wired, and does it do more than just compile. */
public final class CiCdIntegrationCriterion implements Criterion {

    @Override
    public String name() {
        return "CI/CD pipeline integration";
    }

    @Override
    public String mappedFrom() {
        return "Reporting (CI-facing)";
    }

    @Override
    public CriterionScore evaluate(Path repoRoot) {
        List<String> evidence = new ArrayList<>();
        double score = 0;

        List<Path> workflows = RepoScan.findByNameContains(repoRoot.resolve(".github"), ".yml");
        workflows = workflows.isEmpty() ? RepoScan.findByNameContains(repoRoot.resolve(".github"), ".yaml") : workflows;

        if (workflows.isEmpty()) {
            evidence.add("No .github/workflows/*.yml file found.");
            return new CriterionScore(0, evidence, "No CI pipeline found.");
        }
        score += 3;
        evidence.add(workflows.size() + " CI workflow file(s) found under .github/workflows.");

        StringBuilder combined = new StringBuilder();
        for (Path w : workflows) {
            combined.append(RepoScan.readQuietly(w).orElse("")).append('\n');
        }
        String content = combined.toString();
        String lower = content.toLowerCase(java.util.Locale.ROOT);

        int jobCount = RepoScan.countMatches(content, java.util.regex.Pattern.compile("(?m)^  [a-zA-Z0-9_-]+:\\s*$"));
        if (jobCount > 0) {
            score += 1;
            evidence.add("~" + jobCount + " job(s) defined.");
        }

        if (lower.contains("test-reporter") || lower.contains("upload-artifact")) {
            score += 2;
            evidence.add("Test results are surfaced (PR reporter and/or artifact upload) -- not just a pass/fail badge.");
        } else {
            evidence.add("No PR-visible test reporting or artifact upload step found.");
        }

        if (lower.contains("allure")) {
            score += 2;
            evidence.add("The pipeline generates/publishes an Allure report, not just raw logs.");
        }

        if (lower.contains("self-hosted") && !lower.contains("shell: bash") && !lower.contains("shell:bash")) {
            evidence.add("Uses a self-hosted runner without an explicit 'shell: bash' default -- a real risk if that runner is Windows (defaults to PowerShell there).");
        } else if (lower.contains("self-hosted")) {
            score += 1;
            evidence.add("Uses a self-hosted runner with an explicit shell declared.");
        }

        if (lower.contains("compile") || lower.contains("mvn -b compile") || lower.contains("mvnw -b compile")) {
            score += 1;
        }

        String verdict = score >= 8 ? "CI does real work: builds, tests, and surfaces results visibly."
                : score >= 4 ? "CI exists but is thin -- likely compile/test only, with limited visibility."
                : "CI is minimal or absent.";
        return new CriterionScore(Math.min(10, score), evidence, verdict);
    }
}
