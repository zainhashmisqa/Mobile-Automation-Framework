package com.azm.qa.mobile.framework;

import java.nio.file.Files;
import java.nio.file.Path;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.azm.qa.mobile.report.SelfHealingReportGenerator;

/**
 * Pure, device-free unit tests for {@link SelfHealingReportGenerator} -- proves it renders the
 * real heal ledger honestly (deterministic, never claiming AI) and degrades gracefully when
 * there is nothing to report yet.
 */
public class SelfHealingReportGeneratorTest {

    @Test
    public void rendersRealLedgerEntriesWithHonestDeterministicLabeling() throws Exception {
        Path ledger = Files.createTempFile("heal-events", ".jsonl");
        Files.writeString(ledger, """
                {"timestamp":1735689600000,"testClass":"SelfHealingProofTest","testMethod":"cascadeRecoversTheSameElementAfterPrimaryLocatorBreaks","description":"SKIP","originalStrategy":"declared-primary","healedStrategy":"cascade:text-exact","confidence":1.0,"screen":"unknown"}
                """);

        String html = new SelfHealingReportGenerator().generate(ledger);

        Assert.assertTrue(html.contains("Deterministic, not AI."),
                "report must explicitly disclaim AI to avoid the exact overclaim the assessment deck flags");
        Assert.assertTrue(html.contains("SelfHealingProofTest"));
        Assert.assertTrue(html.contains("SKIP"));
        Assert.assertTrue(html.contains("declared-primary"));
        Assert.assertTrue(html.contains("cascade:text-exact"));
        Assert.assertTrue(html.contains("1.00"));
        Assert.assertFalse(html.toLowerCase(java.util.Locale.ROOT).contains("ai-healed"),
                "must never claim an AI/model call performed the heal");
    }

    @Test
    public void degradesGracefullyWithNoLedgerFile() {
        Path missing = Path.of("target", "definitely-does-not-exist-heal-events.jsonl");

        String html = new SelfHealingReportGenerator().generate(missing);

        Assert.assertTrue(html.contains("No heal events recorded yet"));
        Assert.assertTrue(html.contains("0"), "stats should show zero counts, not throw");
    }

    @Test
    public void writesRealFileToDisk() throws Exception {
        Path ledger = Files.createTempFile("heal-events", ".jsonl");
        Files.writeString(ledger, """
                {"timestamp":1735689600000,"testClass":"X","testMethod":"y","description":"Skip","originalStrategy":"declared-primary","healedStrategy":"cascade:resource-id","confidence":0.87,"screen":"OnboardingScreen"}
                """);
        Path outputDir = Files.createTempDirectory("self-healing-report");
        Path outputFile = outputDir.resolve("index.html");

        new SelfHealingReportGenerator().generateToFile(ledger, outputFile);

        Assert.assertTrue(Files.isRegularFile(outputFile));
        String written = Files.readString(outputFile);
        Assert.assertTrue(written.contains("cascade:resource-id"));
    }
}
