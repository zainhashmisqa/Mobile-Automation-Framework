package com.azm.qa.mobile.spec;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A step's target, resolved from its written-out form — never invented. Two shapes:
 *
 * <pre>
 * `accessibility:Skip` (skip button)   -- backtick locator + optional healing-hint description
 * `Skip`                               -- unprefixed backtick: treated as an accessibility id,
 *                                         since Semantics/content-desc is this app's primary
 *                                         locator strategy
 * skip button                          -- no backtick at all: no locator was ever declared, so
 *                                         codegen cannot invent one -- see {@link #hasLocator()}
 * </pre>
 *
 * <p>{@code javaByExpression} is a ready-to-splice Java source expression, e.g.
 * {@code AppiumBy.accessibilityId("Skip")} — {@link TestNgCodeGenerator} embeds it directly
 * rather than resolving it itself, so the generator stays a pure string-template layer with no
 * locator-construction logic of its own.
 */
public final class TargetSpec {

    private static final Pattern BACKTICK = Pattern.compile("`([^`]+)`\\s*(?:\\(([^)]+)\\))?");

    public final String javaByExpression; // null => no locator was declared, this is a TODO
    public final String description;

    private TargetSpec(String javaByExpression, String description) {
        this.javaByExpression = javaByExpression;
        this.description = description;
    }

    public boolean hasLocator() {
        return javaByExpression != null;
    }

    public static TargetSpec parse(String raw) {
        Matcher m = BACKTICK.matcher(raw.trim());
        if (!m.find()) {
            // Prose only -- never invent a locator. This step becomes a TODO; the author (or
            // ScreenCrawler) has to supply a real one.
            return new TargetSpec(null, raw.trim());
        }

        String content = m.group(1);
        String hint = m.group(2);
        String description = hint != null ? hint.trim() : content;

        int colon = content.indexOf(':');
        String prefix = colon > 0 ? content.substring(0, colon) : "";
        String value = colon > 0 ? content.substring(colon + 1) : content;

        String byExpression = switch (prefix) {
            case "accessibility" -> "io.appium.java_client.AppiumBy.accessibilityId(" + quote(value) + ")";
            case "id" -> "io.appium.java_client.AppiumBy.androidUIAutomator("
                    + quote("new UiSelector().resourceId(\"" + escapeForUiSelector(value) + "\")") + ")";
            case "text" -> "io.appium.java_client.AppiumBy.androidUIAutomator("
                    + quote("new UiSelector().text(\"" + escapeForUiSelector(value) + "\")") + ")";
            case "xpath" -> "org.openqa.selenium.By.xpath(" + quote(value) + ")";
            case "uiautomator" -> "io.appium.java_client.AppiumBy.androidUIAutomator(" + quote(value) + ")";
            // No recognized prefix: this app's primary strategy is accessibility id (Flutter
            // Semantics -> content-desc), so bare backtick content is treated as one rather than
            // being rejected outright -- it is still exactly what was written, not a guess.
            default -> "io.appium.java_client.AppiumBy.accessibilityId(" + quote(content) + ")";
        };

        return new TargetSpec(byExpression, description);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String escapeForUiSelector(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
