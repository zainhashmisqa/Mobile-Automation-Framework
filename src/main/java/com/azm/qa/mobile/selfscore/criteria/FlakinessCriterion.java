package com.azm.qa.mobile.selfscore.criteria;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.azm.qa.mobile.selfscore.Criterion;
import com.azm.qa.mobile.selfscore.CriterionScore;
import com.azm.qa.mobile.selfscore.RepoScan;

/**
 * The one criterion this project already learned a hard lesson from: a naive {@code grep -r
 * "Thread.sleep"} against this very repo returns 3 hits, all of them javadoc explaining the
 * framework has none. Scoring off that raw count would incorrectly penalize the exact
 * discipline it's trying to reward, so this counts real calls only, via
 * {@link RepoScan#stripComments}.
 */
public final class FlakinessCriterion implements Criterion {

    private static final Pattern REAL_SLEEP_CALL = Pattern.compile("Thread\\.sleep\\s*\\(");
    // Built at runtime, not written contiguously below: this class's own evidence strings
    // describe what it looks for, and a plain "Thread.sleep(" substring in that description
    // would itself match REAL_SLEEP_CALL when this criterion scans its own source -- confirmed
    // directly (this repo's own selfscore run flagged this file before this constant existed).
    private static final String SLEEP_LABEL = "Thread" + ".sleep";

    @Override
    public String name() {
        return "Flakiness management";
    }

    @Override
    public String mappedFrom() {
        return "Flakiness";
    }

    @Override
    public CriterionScore evaluate(Path repoRoot) {
        List<String> evidence = new ArrayList<>();
        double score = 0;

        List<Path> allFiles = RepoScan.javaFiles(repoRoot, "src/main/java", "src/test/java");
        int rawHits = 0;
        int realHits = 0;
        for (Path f : allFiles) {
            String raw = RepoScan.readQuietly(f).orElse("");
            rawHits += RepoScan.countMatches(raw, REAL_SLEEP_CALL);
            realHits += RepoScan.countMatches(RepoScan.stripComments(raw), REAL_SLEEP_CALL);
        }

        if (allFiles.isEmpty()) {
            // No source to check is not evidence of cleanliness -- score this sub-check 0
            // rather than rewarding "nothing was there to find" as if it were "nothing was
            // found despite looking".
            evidence.add("No Java source files found under src/main/java or src/test/java -- nothing to check.");
        } else if (realHits == 0) {
            score += 6;
            if (rawHits > 0) {
                evidence.add("0 real " + SLEEP_LABEL + "(...) call(s) in code (a raw text search finds " + rawHits
                        + ", all inside comments/javadoc discussing the policy, not actual calls).");
            } else {
                evidence.add("0 " + SLEEP_LABEL + "(...) calls found anywhere, raw or otherwise.");
            }
        } else {
            evidence.add(realHits + " real " + SLEEP_LABEL + "(...) call(s) found in actual code -- a common source of both slow and flaky tests.");
        }

        boolean hasRetry = RepoScan.anyFileNameContains(repoRoot.resolve("src/test"), "retryanalyzer")
                || RepoScan.anyFileNameContains(repoRoot.resolve("src/test"), "retry");
        if (hasRetry) {
            score += 2;
            evidence.add("A RetryAnalyzer/retry mechanism is present.");
        } else {
            evidence.add("No retry-analyzer-style class found.");
        }

        boolean hasQuarantine = RepoScan.anyFileNameContains(repoRoot, "quarantine");
        if (hasQuarantine) {
            score += 2;
            evidence.add("A quarantine suite/tag exists for known-unstable tests, isolated from the main run.");
        } else {
            evidence.add("No quarantine suite/tag found for known-unstable tests.");
        }

        String verdict = score >= 8 ? "Flakiness is actively managed: no raw sleeps, retry + quarantine both present."
                : score >= 4 ? "Some flakiness controls exist but the toolkit is incomplete."
                : "Little to no flakiness management -- expect brittle, sleep-driven tests.";
        return new CriterionScore(Math.min(10, score), evidence, verdict);
    }
}
