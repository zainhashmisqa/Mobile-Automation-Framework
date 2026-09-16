package com.azm.qa.mobile.healing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A composite snapshot of one element — content-desc, text, resource-id, class, structural
 * position and screen bounds — captured the first time a screen resolves it successfully.
 * Stored alongside the declared locator in {@code locator-baselines/*.json} so
 * {@link StrategyCascade} has something concrete to score candidates against when the
 * original locator later breaks.
 *
 * <p>No single attribute is trusted alone: content-desc might be reused by five list rows,
 * text is often localized or user-generated, resource-id is stable but frequently absent on a
 * Flutter build. Scoring several signals together is what lets the cascade tell "the same
 * button after a copy change" apart from "a different button on a redesigned screen".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ElementFingerprint(
        String contentDesc,
        String text,
        String resourceId,
        String className,
        int indexInParent,
        int centerXPercent,
        int centerYPercent) {

    @JsonCreator
    public ElementFingerprint(
            @JsonProperty("contentDesc") String contentDesc,
            @JsonProperty("text") String text,
            @JsonProperty("resourceId") String resourceId,
            @JsonProperty("className") String className,
            @JsonProperty("indexInParent") int indexInParent,
            @JsonProperty("centerXPercent") int centerXPercent,
            @JsonProperty("centerYPercent") int centerYPercent) {
        this.contentDesc = nullToEmpty(contentDesc);
        this.text = nullToEmpty(text);
        this.resourceId = nullToEmpty(resourceId);
        this.className = nullToEmpty(className);
        this.indexInParent = indexInParent;
        this.centerXPercent = centerXPercent;
        this.centerYPercent = centerYPercent;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static ElementFingerprint capture(PageSourceNode node, int screenWidth, int screenHeight) {
        int cxPct = screenWidth <= 0 ? 0 : (node.centerX() * 100) / screenWidth;
        int cyPct = screenHeight <= 0 ? 0 : (node.centerY() * 100) / screenHeight;
        return new ElementFingerprint(
                node.contentDesc, node.text, node.resourceId, node.className,
                node.indexInParent, cxPct, cyPct);
    }

    /**
     * Captures a baseline straight from a live, already-resolved element — used to bootstrap
     * {@code locators/*.json} the first time a screen resolves something with no stored
     * fingerprint yet. {@code indexInParent} is unknown from a live element alone (that needs
     * the page-source tree) and defaults to 0; the other eight cascade strategies don't depend
     * on it, so a bootstrap fingerprint is still a useful baseline, just a slightly weaker one
     * than a strategy-cascade-captured node.
     */
    public static ElementFingerprint captureFromElement(org.openqa.selenium.WebElement element,
                                                          org.openqa.selenium.Dimension screenSize) {
        String contentDesc = attr(element, "content-desc");
        String text = attr(element, "text");
        String resourceId = attr(element, "resource-id");
        String className = attr(element, "className");
        org.openqa.selenium.Point loc = element.getLocation();
        org.openqa.selenium.Dimension size = element.getSize();
        int cx = loc.x + size.width / 2;
        int cy = loc.y + size.height / 2;
        int cxPct = screenSize.width <= 0 ? 0 : (cx * 100) / screenSize.width;
        int cyPct = screenSize.height <= 0 ? 0 : (cy * 100) / screenSize.height;
        return new ElementFingerprint(contentDesc, text, resourceId, className, 0, cxPct, cyPct);
    }

    private static String attr(org.openqa.selenium.WebElement element, String name) {
        try {
            String value = element.getAttribute(name);
            return value == null ? "" : value;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Weighted similarity in [0, 1], renormalized over only the signals this fingerprint
     * actually carries. A Flutter build very commonly gives an element a content-desc and
     * nothing else — no text, no resource-id — and fixed absolute weights would silently
     * punish that as "missing evidence" even though there was never anything to miss. Instead,
     * a field only enters the score (numerator and denominator both) when this fingerprint has
     * a non-empty value for it; class/index/position are structural facts every resolved
     * element has, so they always count.
     *
     * <p>Weights favour content-desc and text — the two signals that survive a resource-id
     * being stripped by the Flutter build — while still crediting structural and positional
     * agreement so a completely different element with coincidental matching text doesn't win.
     */
    public double similarityTo(PageSourceNode candidate, int screenWidth, int screenHeight) {
        double numerator = 0.0;
        double denominator = 0.0;

        if (!contentDesc.isEmpty()) {
            numerator += 0.32 * fieldScore(contentDesc, candidate.contentDesc);
            denominator += 0.32;
        }
        if (!text.isEmpty()) {
            numerator += 0.24 * fieldScore(text, candidate.text);
            denominator += 0.24;
        }
        if (!resourceId.isEmpty()) {
            numerator += 0.16 * fieldScore(resourceId, candidate.resourceId);
            denominator += 0.16;
        }

        // Structural facts: always in play, whether or not this fingerprint's className was
        // ever populated (an empty className correctly scores 0 against any real class, rather
        // than being excluded and inflating the rest of the score).
        numerator += 0.10 * (!className.isEmpty() && className.equals(candidate.className) ? 1.0 : 0.0);
        denominator += 0.10;

        numerator += 0.06 * (indexInParent == candidate.indexInParent ? 1.0 : 0.0);
        denominator += 0.06;

        int candCxPct = screenWidth <= 0 ? 0 : (candidate.centerX() * 100) / screenWidth;
        int candCyPct = screenHeight <= 0 ? 0 : (candidate.centerY() * 100) / screenHeight;
        double positionDistance = Math.hypot(centerXPercent - candCxPct, centerYPercent - candCyPct);
        // Within ~14% of screen diagonal counts as "roughly the same place"; beyond that, no credit.
        double positionScore = Math.max(0.0, 1.0 - positionDistance / 14.0);
        numerator += 0.12 * positionScore;
        denominator += 0.12;

        return Math.min(1.0, numerator / denominator);
    }

    /** Exact match = 1.0, one-way substring = 0.6, either side blank = 0 (no signal to score). */
    private static double fieldScore(String expected, String actual) {
        if (expected.isEmpty() || actual.isEmpty()) {
            return 0.0;
        }
        if (expected.equals(actual)) {
            return 1.0;
        }
        String e = expected.toLowerCase(java.util.Locale.ROOT);
        String a = actual.toLowerCase(java.util.Locale.ROOT);
        if (a.contains(e) || e.contains(a)) {
            return 0.6;
        }
        return 0.0;
    }
}
