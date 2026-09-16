package com.azm.qa.mobile.selfscore.criteria;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.azm.qa.mobile.selfscore.Criterion;
import com.azm.qa.mobile.selfscore.CriterionScore;
import com.azm.qa.mobile.selfscore.RepoScan;

/** Is real reporting infrastructure wired in (Allure or equivalent), not just console logs. */
public final class ReportingInfrastructureCriterion implements Criterion {

    @Override
    public String name() {
        return "Reporting infrastructure";
    }

    @Override
    public String mappedFrom() {
        return "Reporting Methods";
    }

    @Override
    public CriterionScore evaluate(Path repoRoot) {
        List<String> evidence = new ArrayList<>();
        double score = 0;

        String pomContent = RepoScan.readQuietly(repoRoot.resolve("pom.xml")).orElse("");
        boolean allureInPom = pomContent.toLowerCase(java.util.Locale.ROOT).contains("allure");
        if (allureInPom) {
            score += 4;
            evidence.add("pom.xml declares an Allure dependency/plugin.");
        } else {
            evidence.add("pom.xml has no Allure dependency/plugin.");
        }

        boolean hasCategories = RepoScan.anyFileNameContains(repoRoot, "categories.json");
        if (hasCategories) {
            score += 2;
            evidence.add("A categories.json is present -- failures get grouped meaningfully, not left as a flat list.");
        } else {
            evidence.add("No categories.json found.");
        }

        boolean hasEnvWriter = RepoScan.anyFileNameContains(repoRoot.resolve("src/main"), "environment")
                || RepoScan.anyFileNameContains(repoRoot, "environment.properties");
        if (hasEnvWriter) {
            score += 2;
            evidence.add("An environment.properties (or a class that writes one) is present -- the report can show real run context.");
        } else {
            evidence.add("No environment.properties or environment-writing class found.");
        }

        boolean hasListener = RepoScan.anyFileNameContains(repoRoot.resolve("src/test"), "testlistener")
                || RepoScan.anyFileNameContains(repoRoot.resolve("src/test"), "listener");
        if (hasListener) {
            score += 2;
            evidence.add("A TestNG/JUnit listener class is wired for lifecycle events (screenshots, logging).");
        } else {
            evidence.add("No test listener class found.");
        }

        String verdict = score >= 8 ? "Reporting infrastructure is real and reasonably complete."
                : score >= 4 ? "Basic reporting exists but key pieces (categories/environment) are missing."
                : "Little to no structured reporting infrastructure.";
        return new CriterionScore(Math.min(10, score), evidence, verdict);
    }
}
