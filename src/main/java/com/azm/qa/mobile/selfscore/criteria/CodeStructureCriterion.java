package com.azm.qa.mobile.selfscore.criteria;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.azm.qa.mobile.selfscore.Criterion;
import com.azm.qa.mobile.selfscore.CriterionScore;
import com.azm.qa.mobile.selfscore.RepoScan;

/**
 * Checks that a layered architecture is real, not aspirational: a separate driver/core layer,
 * a separate screen/page layer, and no test file directly constructing a driver (the tell-tale
 * sign a "layered" framework is layered in name only).
 */
public final class CodeStructureCriterion implements Criterion {

    private static final Pattern RAW_DRIVER_CONSTRUCTION =
            Pattern.compile("new\\s+(AndroidDriver|IOSDriver|RemoteWebDriver|AppiumDriver)\\s*\\(");

    @Override
    public String name() {
        return "Code structure & layering";
    }

    @Override
    public String mappedFrom() {
        return "Code Structure";
    }

    @Override
    public CriterionScore evaluate(Path repoRoot) {
        List<String> evidence = new ArrayList<>();
        double score = 0;

        boolean hasCoreOrDriverLayer = RepoScan.anyFileNameContains(repoRoot.resolve("src/main"), "driverfactory")
                || RepoScan.anyFileNameContains(repoRoot.resolve("src/main"), "capabilitiesbuilder");
        if (hasCoreOrDriverLayer) {
            score += 4;
            evidence.add("Found a DriverFactory/CapabilitiesBuilder-style class under src/main -- driver construction is centralized.");
        } else {
            evidence.add("No DriverFactory/CapabilitiesBuilder-style class found under src/main.");
        }

        List<Path> testFiles = RepoScan.javaFiles(repoRoot, "src/test/java");
        int violatingTestFiles = 0;
        for (Path f : testFiles) {
            String stripped = RepoScan.readQuietly(f).map(RepoScan::stripComments).orElse("");
            if (RAW_DRIVER_CONSTRUCTION.matcher(stripped).find()) {
                violatingTestFiles++;
            }
        }
        if (!testFiles.isEmpty()) {
            double violationRate = (double) violatingTestFiles / testFiles.size();
            if (violationRate == 0) {
                score += 4;
                evidence.add("0 of " + testFiles.size() + " test file(s) construct a driver directly (new AndroidDriver/... ) -- construction stays behind the factory.");
            } else {
                score += Math.max(0, 4 * (1 - violationRate));
                evidence.add(violatingTestFiles + " of " + testFiles.size() + " test file(s) construct a driver directly, bypassing the factory layer.");
            }
        } else {
            evidence.add("No test files found under src/test/java to check.");
        }

        List<Path> mainFiles = RepoScan.javaFiles(repoRoot, "src/main/java");
        long oversized = mainFiles.stream()
                .map(f -> RepoScan.readQuietly(f).map(s -> s.lines().count()).orElse(0L))
                .filter(lines -> lines > 400)
                .count();
        if (mainFiles.isEmpty()) {
            evidence.add("No main source files found.");
        } else if (oversized == 0) {
            score += 2;
            evidence.add("No main-source file exceeds 400 lines (" + mainFiles.size() + " file(s) checked) -- no obvious god-classes.");
        } else {
            evidence.add(oversized + " of " + mainFiles.size() + " main-source file(s) exceed 400 lines.");
        }

        String verdict = score >= 8 ? "Layering is real and enforced in practice."
                : score >= 5 ? "Layering mostly holds, with some leaks."
                : "Layering is thin or bypassed in places.";
        return new CriterionScore(Math.min(10, score), evidence, verdict);
    }
}
