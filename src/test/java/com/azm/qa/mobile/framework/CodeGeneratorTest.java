package com.azm.qa.mobile.framework;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.azm.qa.mobile.spec.MarkdownSpecParser;
import com.azm.qa.mobile.spec.SpecModel;
import com.azm.qa.mobile.spec.TestNgCodeGenerator;

/**
 * Pure unit tests for {@link TestNgCodeGenerator} — proves the {@code md} pipeline command's
 * two hard guarantees without needing a device: (1) recognized step phrasings compile to a
 * real call, never a guess, and (2) the same spec always generates byte-for-byte identical
 * source, which is what makes {@code codegen-drift-check} in CI meaningful.
 */
public class CodeGeneratorTest {

    private static final String WITH_LOCATOR = """
            # Skip Onboarding

            key: MOB-SKIP
            screen: OnboardingScreen
            priority: CRITICAL

            ## Description
            The user can skip onboarding.

            ## Acceptance Criteria
            - Tapping Skip reaches the home screen

            ## Scenarios

            ### [POSITIVE] Tapping Skip reaches home
            tags: @smoke
            - launch app => the app is in the foreground
            - wait for app ready => the tree has stabilized
            - tap `accessibility:Skip` => the skip button responds
            - expect `accessibility:Home` visible => the home screen renders
            """;

    private static final String WITHOUT_LOCATOR = """
            # Unmapped Screen

            key: MOB-UNMAPPED

            ## Description
            No locators have been crawled for this screen yet.

            ## Acceptance Criteria
            - Placeholder

            ## Scenarios

            ### [POSITIVE] Some interaction nobody has mapped yet
            - tap the mystery button => something happens
            """;

    @Test
    public void recognizedStepsCompileToRealCallsNotComments() {
        SpecModel spec = new MarkdownSpecParser().parse(WITH_LOCATOR);
        TestNgCodeGenerator.GeneratedFile generated = new TestNgCodeGenerator().generate(spec);

        Assert.assertEquals(generated.todoCount(), 0, "every step here uses a recognized phrasing with a real locator");
        Assert.assertTrue(generated.source().contains("appLifecycle.activate();"));
        Assert.assertTrue(generated.source().contains("AppReadyWaits.waitForAppReady(driver);"));
        Assert.assertTrue(generated.source().contains("AppiumBy.accessibilityId(\"Skip\")"));
        Assert.assertTrue(generated.source().contains("screen.expectVisible(io.appium.java_client.AppiumBy.accessibilityId(\"Home\"))"));
    }

    @Test
    public void unrecognizedOrLocatorlessStepsBecomeExplicitTodosNotGuesses() {
        SpecModel spec = new MarkdownSpecParser().parse(WITHOUT_LOCATOR);
        TestNgCodeGenerator.GeneratedFile generated = new TestNgCodeGenerator().generate(spec);

        Assert.assertEquals(generated.todoCount(), 1);
        Assert.assertTrue(generated.source().contains("// TODO(codegen)"));
        Assert.assertFalse(generated.source().contains("AppiumBy.accessibilityId(\"the mystery button\")"),
                "the generator must never invent a locator out of prose");
    }

    @Test
    public void generatedClassNameDerivesFromFeatureName() {
        SpecModel spec = new MarkdownSpecParser().parse(WITH_LOCATOR);
        TestNgCodeGenerator.GeneratedFile generated = new TestNgCodeGenerator().generate(spec);

        Assert.assertEquals(generated.fileName(), "SkipOnboardingTest.java");
        Assert.assertTrue(generated.source().contains("public class SkipOnboardingTest extends BaseTest"));
    }

    @Test
    public void smokeTaggedCriticalScenarioGetsBothGroups() {
        SpecModel spec = new MarkdownSpecParser().parse(WITH_LOCATOR);
        TestNgCodeGenerator.GeneratedFile generated = new TestNgCodeGenerator().generate(spec);

        Assert.assertTrue(generated.source().contains("groups = {\"regression\", \"smoke\"}"));
    }

    @Test
    public void sameSpecGeneratesByteForByteIdenticalSourceEveryTime() {
        SpecModel first = new MarkdownSpecParser().parse(WITH_LOCATOR);
        SpecModel second = new MarkdownSpecParser().parse(WITH_LOCATOR);

        String firstSource = new TestNgCodeGenerator().generate(first).source();
        String secondSource = new TestNgCodeGenerator().generate(second).source();

        Assert.assertEquals(firstSource, secondSource,
                "codegen-drift-check in CI depends on this: same spec in, same Java out, always");
    }
}
