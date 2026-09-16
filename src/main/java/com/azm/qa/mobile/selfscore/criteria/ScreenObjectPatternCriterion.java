package com.azm.qa.mobile.selfscore.criteria;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.azm.qa.mobile.selfscore.Criterion;
import com.azm.qa.mobile.selfscore.CriterionScore;
import com.azm.qa.mobile.selfscore.RepoScan;

/** Mobile POM check: a real Screen/Page Object base class exists, and tests resolve elements through it, not with raw locators of their own. */
public final class ScreenObjectPatternCriterion implements Criterion {

    private static final Pattern EXTENDS_SCREEN_OR_PAGE =
            Pattern.compile("extends\\s+(Base)?(Screen|Page)\\b");
    private static final Pattern RAW_LOCATOR_IN_TEST =
            Pattern.compile("\\b(By|AppiumBy)\\.(id|xpath|accessibilityId|className|cssSelector|androidUIAutomator)\\s*\\(");

    @Override
    public String name() {
        return "Screen Object pattern";
    }

    @Override
    public String mappedFrom() {
        return "POM";
    }

    @Override
    public CriterionScore evaluate(Path repoRoot) {
        List<String> evidence = new ArrayList<>();
        double score = 0;

        List<Path> mainFiles = RepoScan.javaFiles(repoRoot, "src/main/java");
        boolean hasBaseScreen = RepoScan.anyFileNameContains(repoRoot.resolve("src/main"), "basescreen")
                || RepoScan.anyFileNameContains(repoRoot.resolve("src/main"), "basepage");
        long screenObjectCount = mainFiles.stream()
                .filter(f -> RepoScan.readQuietly(f).map(RepoScan::stripComments)
                        .map(s -> EXTENDS_SCREEN_OR_PAGE.matcher(s).find()).orElse(false))
                .count();

        if (hasBaseScreen) {
            score += 3;
            evidence.add("A BaseScreen/BasePage class exists under src/main.");
        } else {
            evidence.add("No BaseScreen/BasePage class found under src/main.");
        }
        if (screenObjectCount > 0) {
            score += 3;
            evidence.add(screenObjectCount + " class(es) extend a Screen/Page base.");
        } else {
            evidence.add("No class found extending a Screen/Page base.");
        }

        List<Path> testFiles = RepoScan.javaFiles(repoRoot, "src/test/java");
        long testsWithRawLocators = testFiles.stream()
                .filter(f -> RepoScan.readQuietly(f).map(RepoScan::stripComments)
                        .map(s -> RAW_LOCATOR_IN_TEST.matcher(s).find()).orElse(false))
                .count();
        if (!testFiles.isEmpty()) {
            if (testsWithRawLocators == 0) {
                score += 4;
                evidence.add("0 of " + testFiles.size() + " test file(s) construct a raw locator (By.*/AppiumBy.*) directly.");
            } else {
                double rate = (double) testsWithRawLocators / testFiles.size();
                score += Math.max(0, 4 * (1 - rate));
                evidence.add(testsWithRawLocators + " of " + testFiles.size() + " test file(s) construct a raw locator directly -- the POM boundary leaks.");
            }
        }

        String verdict = score >= 8 ? "Locators live in screen objects; tests never touch one directly."
                : score >= 4 ? "Screen objects exist but the boundary isn't fully enforced."
                : "No real Page/Screen Object layer detected.";
        return new CriterionScore(Math.min(10, score), evidence, verdict);
    }
}
