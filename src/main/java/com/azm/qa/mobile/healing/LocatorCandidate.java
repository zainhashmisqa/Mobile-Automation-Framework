package com.azm.qa.mobile.healing;

import org.openqa.selenium.By;

import io.appium.java_client.AppiumBy;

/**
 * One author-declared, ranked fallback in a locator chain — see
 * {@link SelfHealingResolver#resolve(java.util.List, String, ElementFingerprint)}.
 *
 * <p>A chain is deterministic and reviewable: healing can only ever land on a locator a human
 * wrote down in advance, in code, in the diff. That is the preferred mode. The
 * {@link StrategyCascade} fallback exists for elements nobody has declared a chain for yet.
 */
public record LocatorCandidate(String strategy, By by) {

    public static LocatorCandidate accessibilityId(String value) {
        return new LocatorCandidate("accessibility-id", AppiumBy.accessibilityId(value));
    }

    public static LocatorCandidate resourceId(String value) {
        return new LocatorCandidate("resource-id",
                AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"" + escape(value) + "\")"));
    }

    public static LocatorCandidate textExact(String value) {
        return new LocatorCandidate("text-exact",
                AppiumBy.androidUIAutomator("new UiSelector().text(\"" + escape(value) + "\")"));
    }

    public static LocatorCandidate textContains(String value) {
        return new LocatorCandidate("text-contains",
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + escape(value) + "\")"));
    }

    public static LocatorCandidate uiAutomator(String uiSelectorExpression) {
        return new LocatorCandidate("uiautomator", AppiumBy.androidUIAutomator(uiSelectorExpression));
    }

    public static LocatorCandidate xpath(String expression) {
        return new LocatorCandidate("xpath", By.xpath(expression));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
