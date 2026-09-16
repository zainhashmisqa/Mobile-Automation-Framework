package com.azm.qa.mobile.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.azm.qa.mobile.analysis.AiNarrator;
import com.azm.qa.mobile.analysis.RunAnalyzer;
import com.azm.qa.mobile.analysis.TestImpactSelector;
import com.azm.qa.mobile.core.driver.DriverFactory;
import com.azm.qa.mobile.healing.ElementFingerprint;
import com.azm.qa.mobile.healing.HealPolicy;
import com.azm.qa.mobile.locators.LocatorDriftChecker;
import com.azm.qa.mobile.locators.LocatorRepository;
import com.azm.qa.mobile.locators.ScreenCrawler;
import com.azm.qa.mobile.report.SelfHealingReportGenerator;
import com.azm.qa.mobile.selfscore.FrameworkScorer;
import com.azm.qa.mobile.selfscore.ScoreReport;
import com.azm.qa.mobile.spec.MarkdownSpecParser;
import com.azm.qa.mobile.spec.SpecModel;
import com.azm.qa.mobile.spec.TestNgCodeGenerator;

import io.appium.java_client.AppiumDriver;

/**
 * The single entry point for every non-device-execution pipeline command — the mobile twin of
 * the web framework's {@code scripts/run-pipeline.ts} / {@code npm run agent:*}. Every
 * subcommand here is either fully offline ({@code md}, {@code parse}, {@code impacted},
 * {@code analyze}) or, for {@code crawl}, needs a live session but no model call.
 *
 * <pre>
 * mvn exec:java -Dexec.args="md --file specs/onboarding.md"
 * mvn exec:java -Dexec.args="md --file specs/onboarding.md --dry-run"
 * mvn exec:java -Dexec.args="parse --file specs/onboarding.md"
 * mvn exec:java -Dexec.args="crawl --screen OnboardingScreen"
 * mvn exec:java -Dexec.args="impacted --base HEAD~1"
 * mvn exec:java -Dexec.args="analyze"
 * </pre>
 */
public final class PipelineRunner {

    private static final Path SPECS_DIR = Path.of("specs");
    private static final Path GENERATED_DIR =
            Path.of("src", "test", "java", "com", "azm", "qa", "mobile", "generated");

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsageAndExit();
        }
        String command = args[0];
        Map<String, String> flags = parseFlags(args, 1);

        switch (command) {
            case "md" -> runMd(flags);
            case "parse" -> runParse(flags);
            case "crawl" -> runCrawl(flags);
            case "impacted" -> runImpacted(flags);
            case "analyze" -> runAnalyze(flags);
            case "selfscore" -> runSelfScore(flags);
            case "heal-report" -> runHealReport(flags);
            default -> {
                System.err.println("Unknown command: " + command);
                printUsageAndExit();
            }
        }
    }

    private static void runMd(Map<String, String> flags) {
        boolean dryRun = flags.containsKey("dry-run");
        List<Path> specFiles = resolveSpecFiles(flags.get("file"));

        MarkdownSpecParser parser = new MarkdownSpecParser();
        TestNgCodeGenerator generator = new TestNgCodeGenerator();

        for (Path specFile : specFiles) {
            String markdown = readFile(specFile);
            SpecModel spec = parser.parse(markdown);
            TestNgCodeGenerator.GeneratedFile generated = generator.generate(spec);

            if (dryRun) {
                System.out.println("--- " + generated.fileName() + " (dry run, not written) ---");
                System.out.println(generated.source());
            } else {
                Path outputFile = GENERATED_DIR.resolve(generated.fileName());
                writeFile(outputFile, generated.source());
                System.out.println("Generated " + outputFile + " (" + generated.todoCount() + " TODO step(s))");
            }
        }
    }

    private static void runParse(Map<String, String> flags) {
        MarkdownSpecParser parser = new MarkdownSpecParser();
        for (Path specFile : resolveSpecFiles(flags.get("file"))) {
            SpecModel spec = parser.parse(readFile(specFile));
            System.out.println("=== " + specFile + " ===");
            System.out.println("feature: " + spec.featureName + "  key: " + spec.key + "  screen: " + spec.screen);
            for (SpecModel.Scenario scenario : spec.scenarios) {
                System.out.println("  [" + scenario.type + "] " + scenario.title
                        + " (" + scenario.steps.size() + " step(s), priority=" + scenario.priority + ")");
            }
        }
    }

    private static void runCrawl(Map<String, String> flags) {
        String screenKey = flags.getOrDefault("screen", "unnamed-screen");
        AppiumDriver driver = DriverFactory.createDriver();
        try {
            ScreenCrawler crawler = new ScreenCrawler(driver);
            Map<String, ElementFingerprint> found = crawler.crawl();

            LocatorRepository repository = new LocatorRepository(screenKey);
            found.forEach(repository::put);

            LocatorDriftChecker driftChecker = new LocatorDriftChecker();
            LocatorDriftChecker.DriftReport drift = driftChecker.check(screenKey, found);
            System.out.println("Crawled " + screenKey + ": " + found.size() + " element(s). Drift: " + drift.summary());
        } finally {
            DriverFactory.quitDriver();
        }
    }

    private static void runImpacted(Map<String, String> flags) {
        String baseRef = flags.getOrDefault("base", "HEAD~1");
        TestImpactSelector selector = new TestImpactSelector();
        TestImpactSelector.Result result = selector.select(baseRef);
        selector.writeSuiteXml(result, Path.of("testng-impacted.xml"));
        System.out.println(result.reason());
    }

    private static void runAnalyze(Map<String, String> flags) {
        Path resultsDir = Path.of(flags.getOrDefault("results", "allure-results"));
        Path ledgerPath = Path.of(flags.getOrDefault("ledger", "self-healing-data/heal-events.jsonl"));
        boolean warnOnly = flags.containsKey("warn-only");

        RunAnalyzer analyzer = new RunAnalyzer();
        RunAnalyzer.Report report = analyzer.analyze(resultsDir, ledgerPath);
        System.out.println(report.summary());

        Optional<String> narration = new AiNarrator().narrate(report);
        narration.ifPresent(text -> {
            System.out.println("--- AI narration (ANTHROPIC_API_KEY was set) ---");
            System.out.println(text);
        });

        // The heal-rate gate: a run where a large fraction of tests needed healing is a real
        // UI-change signal, not incidental drift, and should not just scroll past as green.
        HealPolicy.Verdict verdict = HealPolicy.fromConfig().evaluate(report);
        System.out.println("--- Heal policy: " + verdict.level() + " ---");
        System.out.println(verdict.message());

        if (verdict.shouldFailBuild() && !warnOnly) {
            System.err.println("Failing (pass --warn-only to downgrade this to a warning instead).");
            System.exit(1);
        }
    }

    /**
     * Scores a framework repo against the mobile-mapped 10-criteria rubric adapted from the
     * web-framework assessment deck. Two shapes:
     * <ul>
     *   <li>{@code selfscore} or {@code selfscore --repo <path>} — scores one repo (cwd by
     *       default, or {@code --repo} instead).</li>
     *   <li>{@code selfscore --repo <path> --compare} — scores cwd AND {@code --repo} together
     *       and prints both scorecards side by side, the same way the assessment deck
     *       benchmarked 18 repos against each other.</li>
     * </ul>
     */
    private static void runSelfScore(Map<String, String> flags) {
        FrameworkScorer scorer = new FrameworkScorer();
        String repoFlag = flags.get("repo");

        if (flags.containsKey("compare")) {
            if (repoFlag == null) {
                System.err.println("--compare needs --repo <path> too (compares the current directory against it).");
                return;
            }
            ScoreReport ours = scoreAndWrite(scorer, Path.of("."), "scorecard.md");
            ScoreReport theirs = scoreAndWrite(scorer, Path.of(repoFlag), "scorecard-compare.md");
            printComparison(ours, theirs);
            return;
        }

        scoreAndWrite(scorer, Path.of(repoFlag != null ? repoFlag : "."), "scorecard.md");
    }

    private static ScoreReport scoreAndWrite(FrameworkScorer scorer, Path repoRoot, String reportFileName) {
        String label = repoRoot.toAbsolutePath().normalize().toString();
        ScoreReport report = scorer.evaluate(repoRoot, label);
        System.out.println(report.toConsoleTable());
        Path outputFile = Path.of("target", "selfscore", reportFileName);
        writeFile(outputFile, report.toMarkdown());
        System.out.println("Wrote " + outputFile);
        return report;
    }

    private static void printComparison(ScoreReport ours, ScoreReport theirs) {
        System.out.println("=== Comparison ===");
        for (var criterion : FrameworkScorer.CRITERIA) {
            double a = ours.byCriterion().get(criterion).score();
            double b = theirs.byCriterion().get(criterion).score();
            System.out.printf(java.util.Locale.ROOT, "%-38s %4.1f  vs  %4.1f  (%s)%n",
                    criterion.name(), a, b, a >= b ? "ahead" : "behind");
        }
        System.out.printf(java.util.Locale.ROOT, "%-38s %4.1f  vs  %4.1f%n", "OVERALL", ours.overall(), theirs.overall());
    }

    /**
     * Renders {@code self-healing-data/heal-events.jsonl} into a standalone, honestly-labeled
     * static page -- deterministic fingerprint+cascade recoveries, explicitly not AI, so the
     * report never reads as claiming a model call happened at runtime.
     */
    private static void runHealReport(Map<String, String> flags) {
        Path ledgerPath = Path.of(flags.getOrDefault("ledger", "self-healing-data/heal-events.jsonl"));
        Path outputFile = Path.of(flags.getOrDefault("out", "target/self-healing-report/index.html"));

        new SelfHealingReportGenerator().generateToFile(ledgerPath, outputFile);
        System.out.println("Wrote " + outputFile);
    }

    private static List<Path> resolveSpecFiles(String explicitFile) {
        if (explicitFile != null) {
            Path file = Path.of(explicitFile);
            if (!Files.isRegularFile(file)) {
                throw new IllegalArgumentException("No such spec file: " + file);
            }
            return List.of(file);
        }
        if (!Files.isDirectory(SPECS_DIR)) {
            return List.of();
        }
        try (var stream = Files.list(SPECS_DIR)) {
            return stream.filter(p -> p.toString().endsWith(".md")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
    }

    private static void writeFile(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + path, e);
        }
    }

    private static Map<String, String> parseFlags(String[] args, int startIndex) {
        Map<String, String> flags = new HashMap<>();
        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                continue;
            }
            String key = arg.substring(2);
            boolean hasValue = i + 1 < args.length && !args[i + 1].startsWith("--");
            flags.put(key, hasValue ? args[++i] : "true");
        }
        return flags;
    }

    private static void printUsageAndExit() {
        System.err.println("""
                Usage: mvn exec:java -Dexec.args="<command> [--flags]"
                Commands:
                  md       --file <spec.md> [--dry-run]     Generate TestNG classes from spec(s)
                  parse    --file <spec.md>                 Parse and print a spec's structure
                  crawl    --screen <name>                  Crawl the current live screen into locators/<name>.json
                  impacted --base <gitRef>                  Write testng-impacted.xml from a git diff
                  analyze  [--results <dir>] [--ledger <f>] [--warn-only] Deterministic run analysis
                           (+ optional AI narration) + a heal-rate gate that exits 1 above
                           healpolicy.maxHealRate unless --warn-only downgrades it to a warning
                  selfscore [--repo <path>] [--compare]     Score a repo against the mobile-mapped
                           10-criteria rubric (adapted from the web-framework assessment deck).
                           --repo alone scores that repo instead of the current directory;
                           --repo + --compare scores both and prints them side by side.
                  heal-report [--ledger <f>] [--out <f>]    Render the heal ledger into a standalone,
                           honestly-labeled static page (deterministic, not AI) at
                           target/self-healing-report/index.html by default.
                """);
        System.exit(1);
    }
}
