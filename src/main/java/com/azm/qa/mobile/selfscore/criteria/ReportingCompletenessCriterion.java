package com.azm.qa.mobile.selfscore.criteria;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.azm.qa.mobile.selfscore.Criterion;
import com.azm.qa.mobile.selfscore.CriterionScore;
import com.azm.qa.mobile.selfscore.RepoScan;

/** Of the tests that exist, how many actually annotate themselves for the report (vs. running silently). */
public final class ReportingCompletenessCriterion implements Criterion {

    private static final Pattern TEST_ANNOTATION = Pattern.compile("@Test\\b");
    private static final Pattern REPORT_CALL =
            Pattern.compile("\\b(annotate|Allure\\.(step|label|epic|feature|story|description|addAttachment)|@Step|@Attachment)\\s*\\(?");

    @Override
    public String name() {
        return "Reporting completeness";
    }

    @Override
    public String mappedFrom() {
        return "Reporting Completeness";
    }

    @Override
    public CriterionScore evaluate(Path repoRoot) {
        List<String> evidence = new ArrayList<>();
        List<Path> testFiles = RepoScan.javaFiles(repoRoot, "src/test/java");

        int totalTests = 0;
        int filesWithReportCalls = 0;
        for (Path f : testFiles) {
            String stripped = RepoScan.readQuietly(f).map(RepoScan::stripComments).orElse("");
            int testsInFile = RepoScan.countMatches(stripped, TEST_ANNOTATION);
            totalTests += testsInFile;
            if (testsInFile > 0 && REPORT_CALL.matcher(stripped).find()) {
                filesWithReportCalls++;
            }
        }

        long testFilesWithTests = testFiles.stream()
                .filter(f -> RepoScan.readQuietly(f).map(RepoScan::stripComments)
                        .map(s -> TEST_ANNOTATION.matcher(s).find()).orElse(false))
                .count();

        if (testFilesWithTests == 0) {
            evidence.add("No @Test methods found under src/test/java.");
            return new CriterionScore(0, evidence, "No tests to evaluate reporting against.");
        }

        double rate = (double) filesWithReportCalls / testFilesWithTests;
        double score = rate * 10;
        evidence.add(totalTests + " @Test method(s) across " + testFilesWithTests + " test file(s).");
        evidence.add(filesWithReportCalls + " of " + testFilesWithTests + " test file(s) (" + Math.round(rate * 100)
                + "%) call an Allure/@Step-style reporting API.");

        String verdict = score >= 8 ? "Nearly every test file annotates itself for the report."
                : score >= 4 ? "Reporting calls are present but inconsistent across test files."
                : "Most tests run with no reporting annotation at all.";
        return new CriterionScore(Math.min(10, score), evidence, verdict);
    }
}
