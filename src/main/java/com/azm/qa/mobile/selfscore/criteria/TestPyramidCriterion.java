package com.azm.qa.mobile.selfscore.criteria;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.azm.qa.mobile.selfscore.Criterion;
import com.azm.qa.mobile.selfscore.CriterionScore;
import com.azm.qa.mobile.selfscore.RepoScan;

/**
 * Mobile mapping of the deck's slide-7 finding ("only 4/18 go beyond GUI/E2E -- the rest are
 * pure ice-cream-cone suites"). A test file is classed as "device/E2E" if it references a
 * driver directly or extends a base that clearly wires one up; everything else (parsers,
 * scoring logic, data stores, pure unit tests) counts as the device-free base of the pyramid.
 */
public final class TestPyramidCriterion implements Criterion {

    private static final Pattern DEVICE_SIGNAL = Pattern.compile(
            "\\b(AppiumDriver|AndroidDriver|IOSDriver|DriverFactory|extends\\s+BaseTest)\\b");
    private static final Pattern TEST_ANNOTATION = Pattern.compile("@Test\\b");

    @Override
    public String name() {
        return "Test pyramid shape";
    }

    @Override
    public String mappedFrom() {
        return "Test Count/Type";
    }

    @Override
    public CriterionScore evaluate(Path repoRoot) {
        List<String> evidence = new ArrayList<>();
        List<Path> testFiles = RepoScan.javaFiles(repoRoot, "src/test/java");

        int deviceTests = 0;
        int deviceFreeTests = 0;
        for (Path f : testFiles) {
            String stripped = RepoScan.readQuietly(f).map(RepoScan::stripComments).orElse("");
            int testsInFile = RepoScan.countMatches(stripped, TEST_ANNOTATION);
            if (testsInFile == 0) {
                continue;
            }
            if (DEVICE_SIGNAL.matcher(stripped).find()) {
                deviceTests += testsInFile;
            } else {
                deviceFreeTests += testsInFile;
            }
        }

        int total = deviceTests + deviceFreeTests;
        if (total == 0) {
            evidence.add("No @Test methods found.");
            return new CriterionScore(0, evidence, "No tests to classify.");
        }

        double deviceFreeRate = (double) deviceFreeTests / total;
        evidence.add(deviceFreeTests + " device-free test(s), " + deviceTests + " device/E2E test(s) -- "
                + Math.round(deviceFreeRate * 100) + "% of the suite needs no device at all.");

        double score;
        if (deviceFreeRate >= 0.5) {
            score = 8 + Math.min(2, deviceFreeRate * 2);
            evidence.add("A real base layer exists beneath the E2E tests -- not a pure ice-cream-cone suite.");
        } else if (deviceFreeRate > 0) {
            score = 4 + deviceFreeRate * 8;
            evidence.add("Some device-free coverage exists, but E2E tests still dominate the suite.");
        } else {
            score = deviceTests > 0 ? 2 : 0;
            evidence.add("Every test needs a live device/driver -- a classic ice-cream-cone shape (matches the assessment's most common finding across the web repos: 14 of 18).");
        }

        String verdict = score >= 8 ? "A real pyramid: a solid device-free base under the E2E layer."
                : score >= 4 ? "Partial base layer, still E2E-heavy."
                : "Ice-cream-cone: everything depends on a live device.";
        return new CriterionScore(Math.min(10, score), evidence, verdict);
    }
}
