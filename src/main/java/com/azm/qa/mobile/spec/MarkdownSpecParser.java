package com.azm.qa.mobile.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a {@code specs/*.md} feature file into a {@link SpecModel}. Fully deterministic, no
 * AI, no network call, sub-millisecond — the exact same guarantee the web framework's
 * {@code MarkdownAgent} makes: the same spec always produces the same tree, and therefore the
 * same generated code. This is what makes {@code codegen-drift-check} in CI meaningful — if
 * regenerating from an unchanged spec ever produced a different file, drift detection would be
 * lying.
 *
 * <p>Format (see {@code specs/onboarding.md} for a worked example):
 * <pre>
 * # Feature name
 *
 * key: SBA-LOGIN
 * screen: LoginScreen
 * priority: CRITICAL
 *
 * ## Description
 * Free text.
 *
 * ## Acceptance Criteria
 * - one per bullet
 *
 * ## Scenarios
 *
 * ### [POSITIVE] a descriptive title
 * priority: CRITICAL
 * tags: @smoke
 * preconditions: comma, separated
 * - action =&gt; expected result
 * </pre>
 */
public final class MarkdownSpecParser {

    private static final Pattern METADATA_LINE = Pattern.compile("^(\\w+):\\s*(.+)$");
    private static final Pattern SCENARIO_HEADING = Pattern.compile("^###\\s*\\[(POSITIVE|NEGATIVE|EDGE)]\\s*(.+)$");
    // "=>" is canonical; "->" is the documented alias.
    private static final Pattern STEP_LINE = Pattern.compile("^-\\s*(.+?)\\s*(?:=>|->)\\s*(.+)$");

    public SpecModel parse(String markdown) {
        List<String> lines = List.of(markdown.replace("\r\n", "\n").split("\n"));
        SpecModel spec = new SpecModel();

        Section section = Section.NONE;
        SpecModel.Scenario currentScenario = null;

        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();
            String trimmed = line.trim();

            if (trimmed.startsWith("# ") && spec.featureName == null) {
                spec.featureName = trimmed.substring(2).trim();
                continue;
            }
            if (trimmed.equals("## Description")) {
                section = Section.DESCRIPTION;
                continue;
            }
            if (trimmed.equals("## Acceptance Criteria")) {
                section = Section.ACCEPTANCE;
                continue;
            }
            if (trimmed.equals("## Scenarios")) {
                section = Section.SCENARIOS;
                continue;
            }

            Matcher scenarioHeading = SCENARIO_HEADING.matcher(trimmed);
            if (scenarioHeading.matches()) {
                currentScenario = new SpecModel.Scenario();
                currentScenario.type = scenarioHeading.group(1);
                currentScenario.title = scenarioHeading.group(2).trim();
                currentScenario.priority = spec.priority;
                spec.scenarios.add(currentScenario);
                continue;
            }

            if (trimmed.isEmpty()) {
                continue;
            }

            switch (section) {
                case NONE -> parseTopLevelMetadata(spec, trimmed);
                case DESCRIPTION -> spec.description = spec.description.isEmpty()
                        ? trimmed
                        : spec.description + " " + trimmed;
                case ACCEPTANCE -> {
                    if (trimmed.startsWith("- ")) {
                        spec.acceptanceCriteria.add(trimmed.substring(2).trim());
                    }
                }
                case SCENARIOS -> parseScenarioLine(currentScenario, trimmed);
            }
        }

        return spec;
    }

    private void parseTopLevelMetadata(SpecModel spec, String line) {
        Matcher m = METADATA_LINE.matcher(line);
        if (!m.matches()) {
            return;
        }
        String key = m.group(1);
        String value = m.group(2).trim();
        switch (key) {
            case "key" -> spec.key = value;
            case "screen" -> spec.screen = value;
            case "priority" -> spec.priority = value;
            default -> { /* unknown top-level metadata is ignored, not fatal */ }
        }
    }

    private void parseScenarioLine(SpecModel.Scenario scenario, String line) {
        if (scenario == null) {
            return; // metadata appearing before any scenario heading -- ignore rather than NPE
        }

        Matcher metadata = METADATA_LINE.matcher(line);
        if (metadata.matches() && !line.startsWith("-")) {
            String key = metadata.group(1);
            String value = metadata.group(2).trim();
            switch (key) {
                case "priority" -> scenario.priority = value;
                case "tags" -> scenario.tags.addAll(splitCsv(value));
                case "preconditions" -> scenario.preconditions.addAll(splitCsv(value));
                default -> { /* unknown scenario metadata is ignored, not fatal */ }
            }
            return;
        }

        Matcher step = STEP_LINE.matcher(line);
        if (step.matches()) {
            scenario.steps.add(new SpecModel.Step(step.group(1).trim(), step.group(2).trim()));
        }
    }

    private static List<String> splitCsv(String value) {
        List<String> parts = new ArrayList<>();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        return parts;
    }

    private enum Section { NONE, DESCRIPTION, ACCEPTANCE, SCENARIOS }
}
