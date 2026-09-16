package com.azm.qa.mobile.framework;

import java.nio.file.Files;
import java.nio.file.Path;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.azm.qa.mobile.selfscore.CriterionScore;
import com.azm.qa.mobile.selfscore.FrameworkScorer;
import com.azm.qa.mobile.selfscore.RepoScan;
import com.azm.qa.mobile.selfscore.ScoreReport;
import com.azm.qa.mobile.selfscore.criteria.FlakinessCriterion;

/**
 * Pure unit tests for the {@code selfscore} agent — no device, no compile of the scored repo.
 * The one non-negotiable behaviour: a comment-only mention of {@code Thread.sleep} must never
 * count as a real call, confirmed against this exact repo's own history (a raw grep here
 * returns 3 hits, all in javadoc).
 */
public class FrameworkScorerTest {

    // Built at runtime, never written contiguously in this file's own source: this repo's own
    // selfscore command scans src/test/java (including this very file), and a literal
    // "Thread.sleep(" substring sitting in a fixture string would count as a real call against
    // this repo -- exactly the kind of string-literal-vs-comment confusion the scorer can't
    // resolve on its own (it distinguishes comments from code, not fixture data from real code).
    private static final String THREAD_SLEEP = "Thread" + ".sleep";

    @Test
    public void stripCommentsRemovesBlockCommentsAndFullLineComments() {
        String source = """
                package x;
                /**
                 * This class has no engineer-written {@code %s} anywhere.
                 */
                public class Y {
                    // %s(1000); -- an old, commented-out call
                    void real() {
                        %s(1000);
                    }
                }
                """.formatted(THREAD_SLEEP, THREAD_SLEEP, THREAD_SLEEP);

        String stripped = RepoScan.stripComments(source);

        Assert.assertFalse(stripped.contains("{@code"), "block comment should be fully removed");
        Assert.assertFalse(stripped.contains("an old, commented-out call"), "full-line // comment should be removed");
        Assert.assertTrue(stripped.contains(THREAD_SLEEP + "(1000);"), "the real call must survive stripping");
    }

    @Test
    public void flakinessCriterionIgnoresCommentOnlyMentionsOfThreadSleep() throws Exception {
        Path tempRepo = Files.createTempDirectory("selfscore-flakiness-fixture");
        Path srcFile = tempRepo.resolve("src/main/java/Sample.java");
        Files.createDirectories(srcFile.getParent());
        Files.writeString(srcFile, """
                /**
                 * No {@code %s} is ever called here -- see %s discussion.
                 */
                public class Sample {
                    // %s(500);
                    void ok() { }
                }
                """.formatted(THREAD_SLEEP, THREAD_SLEEP, THREAD_SLEEP));

        CriterionScore score = new FlakinessCriterion().evaluate(tempRepo);

        boolean mentionsZeroRealCalls = score.evidence().stream()
                .anyMatch(e -> e.contains("0 real Thread.sleep"));
        Assert.assertTrue(mentionsZeroRealCalls,
                "evidence should report 0 real calls despite raw text containing 'Thread.sleep' three times: " + score.evidence());
        Assert.assertTrue(score.score() >= 6, "a file with zero real sleeps should score at least the base 6 for that check");
    }

    @Test
    public void frameworkScorerRunsAllTenCriteriaAgainstThisRealRepo() {
        Path repoRoot = Path.of(".");
        ScoreReport report = new FrameworkScorer().evaluate(repoRoot, "this repo");

        Assert.assertEquals(report.byCriterion().size(), 10, "all ten criteria should have run");
        report.byCriterion().forEach((criterion, score) ->
                Assert.assertFalse(score.evidence().isEmpty(),
                        criterion.name() + " should always produce at least one evidence line"));

        CriterionScore flakiness = report.byCriterion().entrySet().stream()
                .filter(e -> e.getKey().name().equals("Flakiness management"))
                .map(java.util.Map.Entry::getValue)
                .findFirst().orElseThrow();
        Assert.assertTrue(flakiness.score() >= 6,
                "this repo's own Thread.sleep javadoc mentions must not drag its flakiness score down");
    }

    @Test
    public void degradesGracefullyAgainstAnEmptyDirectory() throws Exception {
        Path empty = Files.createTempDirectory("selfscore-empty-fixture");

        ScoreReport report = new FrameworkScorer().evaluate(empty, "empty repo");

        Assert.assertEquals(report.byCriterion().size(), 10);
        report.byCriterion().forEach((criterion, score) -> {
            Assert.assertTrue(score.score() <= 3, criterion.name() + " should score low against an empty repo, got " + score.score());
            Assert.assertFalse(score.evidence().isEmpty(), criterion.name() + " must still explain itself, not just return 0 silently");
        });
    }
}
