package com.azm.qa.mobile.spec;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles one spec step into Java source lines. This is the single place that decides which
 * phrasings compile to a real call versus an explicit {@code // TODO(codegen)} — matching it
 * is what {@code /gen-tests} reads before authoring a spec, so AI-authored specs land on real
 * code from the first draft instead of a wall of TODOs.
 *
 * <p>Every recognized step is deterministic string-templating: no branch here depends on
 * anything but the step's own text, so the same Markdown always compiles to the same Java.
 */
public final class StepVocabulary {

    private static final Pattern BACKGROUND = Pattern.compile("^background app (\\d+)s$");
    private static final Pattern TAP = Pattern.compile("^tap (.+)$");
    private static final Pattern TYPE = Pattern.compile("^type (.+?) into (.+)$");
    private static final Pattern SWIPE = Pattern.compile("^swipe (up|down|left|right)$");
    private static final Pattern SCROLL_TO = Pattern.compile("^scroll to (.+)$");
    private static final Pattern GRANT_PERMISSION = Pattern.compile("^grant permission (.+)$");
    private static final Pattern REVOKE_PERMISSION = Pattern.compile("^revoke permission (.+)$");
    private static final Pattern DEEP_LINK = Pattern.compile("^open deep link (.+)$");
    private static final Pattern ROTATE = Pattern.compile("^rotate (portrait|landscape)$");
    private static final Pattern EXPECT_VISIBLE = Pattern.compile("^expect (.+) visible$");
    private static final Pattern EXPECT_HIDDEN = Pattern.compile("^expect (.+) hidden$");
    private static final Pattern EXPECT_CONTAINS = Pattern.compile("^expect (.+?) contains (.+)$");
    private static final Pattern RAW_ESCAPE = Pattern.compile("^`(.+;)\\s*`$");

    public CompiledStep compile(SpecModel.Step step) {
        String action = step.action.trim();

        if (action.equals("launch app")) {
            return real("appLifecycle.activate();");
        }
        if (action.equals("relaunch app")) {
            return real("appLifecycle.restart();");
        }
        Matcher background = BACKGROUND.matcher(action);
        if (background.matches()) {
            return real("appLifecycle.backgroundFor(java.time.Duration.ofSeconds(" + background.group(1) + "));");
        }

        Matcher tap = TAP.matcher(action);
        if (tap.matches()) {
            return compileTargeted(tap.group(1), "tap");
        }

        Matcher type = TYPE.matcher(action);
        if (type.matches()) {
            String value = stripQuotesOrBackticks(type.group(1));
            TargetSpec target = TargetSpec.parse(type.group(2));
            if (!target.hasLocator()) {
                return todo(step, "type step has no backtick locator for \"" + target.description + "\"");
            }
            return real("screen.type(" + target.javaByExpression + ", " + javaString(target.description)
                    + ", " + javaString(value) + ");");
        }

        Matcher swipe = SWIPE.matcher(action);
        if (swipe.matches()) {
            return real("gestures.swipe(com.azm.qa.mobile.core.gestures.GestureUtils.Direction."
                    + swipe.group(1).toUpperCase(java.util.Locale.ROOT) + ");");
        }

        Matcher scrollTo = SCROLL_TO.matcher(action);
        if (scrollTo.matches()) {
            TargetSpec target = TargetSpec.parse(scrollTo.group(1));
            if (!target.hasLocator()) {
                return todo(step, "scroll-to step has no backtick locator for \"" + target.description + "\"");
            }
            return real("gestures.scrollToElement(() -> driver.findElement(" + target.javaByExpression
                    + "), com.azm.qa.mobile.core.gestures.GestureUtils.Direction.DOWN, 6);");
        }

        Matcher grant = GRANT_PERMISSION.matcher(action);
        if (grant.matches()) {
            return real("permissions.grant(" + javaString(grant.group(1).trim()) + ");");
        }
        Matcher revoke = REVOKE_PERMISSION.matcher(action);
        if (revoke.matches()) {
            return real("permissions.revoke(" + javaString(revoke.group(1).trim()) + ");");
        }

        Matcher deepLink = DEEP_LINK.matcher(action);
        if (deepLink.matches()) {
            return real("deepLink.open(" + javaString(deepLink.group(1).trim()) + ");");
        }

        Matcher rotate = ROTATE.matcher(action);
        if (rotate.matches()) {
            String orientation = rotate.group(1).equalsIgnoreCase("portrait") ? "PORTRAIT" : "LANDSCAPE";
            return real("driver.rotate(org.openqa.selenium.ScreenOrientation." + orientation + ");");
        }

        Matcher expectVisible = EXPECT_VISIBLE.matcher(action);
        if (expectVisible.matches()) {
            TargetSpec target = TargetSpec.parse(expectVisible.group(1));
            if (!target.hasLocator()) {
                return todo(step, "expect-visible has no backtick locator for \"" + target.description + "\"");
            }
            return real("screen.expectVisible(" + target.javaByExpression + ");");
        }
        Matcher expectHidden = EXPECT_HIDDEN.matcher(action);
        if (expectHidden.matches()) {
            TargetSpec target = TargetSpec.parse(expectHidden.group(1));
            if (!target.hasLocator()) {
                return todo(step, "expect-hidden has no backtick locator for \"" + target.description + "\"");
            }
            return real("org.testng.Assert.assertTrue(screen.expectHidden(" + target.javaByExpression + "), "
                    + javaString("expected hidden: " + target.description) + ");");
        }
        Matcher expectContains = EXPECT_CONTAINS.matcher(action);
        if (expectContains.matches()) {
            TargetSpec target = TargetSpec.parse(expectContains.group(1));
            String expectedText = stripQuotesOrBackticks(expectContains.group(2));
            if (!target.hasLocator()) {
                return todo(step, "expect-contains has no backtick locator for \"" + target.description + "\"");
            }
            return real("org.testng.Assert.assertTrue(driver.findElement(" + target.javaByExpression
                    + ").getText().contains(" + javaString(expectedText) + "), "
                    + javaString(target.description + " should contain: " + expectedText) + ");");
        }

        if (action.equals("switch to webview")) {
            return real("webViewContext.switchToWebView();");
        }
        if (action.equals("switch to native")) {
            return real("webViewContext.switchToNative();");
        }
        if (action.equals("wait for app ready")) {
            return real("com.azm.qa.mobile.core.waits.AppReadyWaits.waitForAppReady(driver);");
        }

        Matcher rawEscape = RAW_ESCAPE.matcher(action);
        if (rawEscape.matches()) {
            return real(rawEscape.group(1));
        }

        return todo(step, "no recognized phrasing");
    }

    private CompiledStep compileTargeted(String remainder, String verb) {
        TargetSpec target = TargetSpec.parse(remainder);
        if (!target.hasLocator()) {
            return new CompiledStep(
                    List.of("// TODO(codegen): " + verb + " " + remainder
                            + " -- no backtick locator declared; crawl it with `agent:crawl` "
                            + "or add one by hand, then regenerate."),
                    true);
        }
        return real("screen." + verb + "(" + target.javaByExpression + ", " + javaString(target.description) + ");");
    }

    private CompiledStep real(String line) {
        return new CompiledStep(List.of(line), false);
    }

    private CompiledStep todo(SpecModel.Step step, String reason) {
        return new CompiledStep(
                List.of("// TODO(codegen): \"" + step.action + "\" -- " + reason), true);
    }

    private static String stripQuotesOrBackticks(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("`") && trimmed.endsWith("`")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String javaString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public record CompiledStep(List<String> javaLines, boolean isTodo) {
    }
}
