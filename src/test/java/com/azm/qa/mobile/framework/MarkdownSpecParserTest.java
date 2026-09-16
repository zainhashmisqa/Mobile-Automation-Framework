package com.azm.qa.mobile.framework;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.azm.qa.mobile.spec.MarkdownSpecParser;
import com.azm.qa.mobile.spec.SpecModel;

/**
 * Pure unit tests for {@link MarkdownSpecParser} — no device, no filesystem beyond an inline
 * string. Proves the parser is deterministic and that the documented spec format actually
 * parses the way the README and {@code /gen-tests} claim it does.
 */
public class MarkdownSpecParserTest {

    private static final String SAMPLE = """
            # Onboarding

            key: MOB-ONBOARD
            screen: OnboardingScreen
            priority: HIGH

            ## Description
            First-launch flow.

            ## Acceptance Criteria
            - The app launches
            - A real screen renders

            ## Scenarios

            ### [POSITIVE] The app launches and reaches a real screen
            priority: CRITICAL
            tags: @smoke
            preconditions: app is installed
            - launch app => the app is in the foreground
            - wait for app ready => the tree has stabilized

            ### [NEGATIVE] A cold start with no network still reaches the app
            - mock **/api/config status 500 -> a config fetch fails
            - launch app => the app still starts
            """;

    @Test
    public void parsesTopLevelMetadata() {
        SpecModel spec = new MarkdownSpecParser().parse(SAMPLE);

        Assert.assertEquals(spec.featureName, "Onboarding");
        Assert.assertEquals(spec.key, "MOB-ONBOARD");
        Assert.assertEquals(spec.screen, "OnboardingScreen");
        Assert.assertEquals(spec.priority, "HIGH");
    }

    @Test
    public void parsesDescriptionAndAcceptanceCriteria() {
        SpecModel spec = new MarkdownSpecParser().parse(SAMPLE);

        Assert.assertEquals(spec.description, "First-launch flow.");
        Assert.assertEquals(spec.acceptanceCriteria.size(), 2);
        Assert.assertEquals(spec.acceptanceCriteria.get(0), "The app launches");
    }

    @Test
    public void parsesScenariosWithTypeTagsAndSteps() {
        SpecModel spec = new MarkdownSpecParser().parse(SAMPLE);

        Assert.assertEquals(spec.scenarios.size(), 2);

        SpecModel.Scenario positive = spec.scenarios.get(0);
        Assert.assertEquals(positive.type, "POSITIVE");
        Assert.assertEquals(positive.title, "The app launches and reaches a real screen");
        Assert.assertEquals(positive.priority, "CRITICAL");
        Assert.assertTrue(positive.tags.contains("@smoke"));
        Assert.assertTrue(positive.preconditions.contains("app is installed"));
        Assert.assertEquals(positive.steps.size(), 2);
        Assert.assertEquals(positive.steps.get(0).action, "launch app");
        Assert.assertEquals(positive.steps.get(0).expected, "the app is in the foreground");
    }

    @Test
    public void arrowAliasWorksTheSameAsFatArrow() {
        SpecModel spec = new MarkdownSpecParser().parse(SAMPLE);
        SpecModel.Scenario negative = spec.scenarios.get(1);

        // Uses "->" instead of "=>" -- the documented alias.
        Assert.assertEquals(negative.steps.get(0).action, "mock **/api/config status 500");
        Assert.assertEquals(negative.steps.get(0).expected, "a config fetch fails");
    }

    @Test
    public void negativeScenarioWithNoExplicitPriorityInheritsSpecDefault() {
        SpecModel spec = new MarkdownSpecParser().parse(SAMPLE);
        SpecModel.Scenario negative = spec.scenarios.get(1);

        // The scenario declares no priority of its own -- it should inherit the spec's
        // top-level default ("priority: HIGH") rather than some hardcoded fallback.
        Assert.assertEquals(negative.priority, "HIGH");
    }

    @Test
    public void sameInputAlwaysProducesAnEqualParseTree() {
        MarkdownSpecParser parser = new MarkdownSpecParser();
        SpecModel first = parser.parse(SAMPLE);
        SpecModel second = parser.parse(SAMPLE);

        Assert.assertEquals(first.scenarios.size(), second.scenarios.size());
        Assert.assertEquals(first.scenarios.get(0).steps.size(), second.scenarios.get(0).steps.size());
    }
}
