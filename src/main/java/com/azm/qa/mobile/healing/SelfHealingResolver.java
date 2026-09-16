package com.azm.qa.mobile.healing;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.core.config.ConfigManager;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;

/**
 * Runtime locator-recovery engine — the Java port of the web framework's
 * {@code SelfHealingLocator}, with the same two-mode hybrid and the same non-negotiable rules.
 * No model is ever called: recovery is entirely fingerprint matching and structural search
 * over the current page source.
 *
 * <p><b>Mode 1 — declared candidate chain (preferred).</b> A screen supplies a ranked list of
 * {@link LocatorCandidate}s; the first that resolves to exactly one usable element wins.
 * Anything past index 0 is a heal and is reported as such — a screen quietly living on its
 * third fallback shows up in the ledger rather than hiding.
 *
 * <p><b>Mode 2 — heuristic cascade (fallback).</b> When every declared candidate fails, or
 * when a screen has declared only a single locator and it breaks, {@link StrategyCascade}
 * searches the live page source for the best-scoring match against the stored
 * {@link ElementFingerprint}. If nothing clears {@code selfhealing.minConfidence}, resolution
 * fails loudly rather than guessing.
 *
 * <p><b>Assertions never go through this class.</b> Healing answers "this locator is broken";
 * an assertion waiting for something to appear can't tell that apart from "not rendered yet",
 * and healing onto a looser locator could make an assertion pass against the wrong element.
 * {@code BaseScreen}'s {@code expectVisible}/{@code expectHidden} use the raw {@link By} with
 * an ordinary wait — see its javadoc.
 */
public final class SelfHealingResolver {

    private static final Logger log = LoggerFactory.getLogger(SelfHealingResolver.class);

    private final AppiumDriver driver;
    private final HealStore store;
    private final boolean enabled;
    private final Duration candidateTimeout;
    private final StrategyCascade cascade;

    public SelfHealingResolver(AppiumDriver driver) {
        this.driver = driver;
        this.store = new HealStore(Path.of(
                ConfigManager.get("selfhealing.ledgerPath", "self-healing-data/heal-events.jsonl")));
        this.enabled = ConfigManager.getBoolean("selfhealing.enabled", true);
        this.candidateTimeout = Duration.ofMillis(
                ConfigManager.getInt("selfhealing.candidateTimeoutMillis", 2000));
        this.cascade = new StrategyCascade(
                Double.parseDouble(ConfigManager.get("selfhealing.minConfidence", "0.62")));
    }

    /**
     * Resolves a declared candidate chain. First candidate that attaches AND is usable wins;
     * if all of them fail and a fingerprint baseline is supplied, falls through to the cascade
     * before giving up.
     */
    public WebElement resolve(List<LocatorCandidate> candidates, String description, ElementFingerprint baseline) {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "No locator candidates supplied for \"" + description + "\"");
        }
        String primaryStrategy = candidates.get(0).strategy();

        for (int i = 0; i < candidates.size(); i++) {
            LocatorCandidate candidate = candidates.get(i);
            Optional<WebElement> found = firstUsable(candidate.by());
            if (found.isEmpty()) {
                continue;
            }
            if (i > 0) {
                recordHeal(description, primaryStrategy, candidate.strategy(), 1.0);
            }
            return found.get();
        }

        if (enabled && baseline != null) {
            Optional<WebElement> healed = tryCascade(description, baseline);
            if (healed.isPresent()) {
                return healed.get();
            }
        }

        throw new NoSuchElementException(
                "[self-heal] All " + candidates.size() + " candidate(s) failed for \"" + description + "\":\n"
                        + candidates.stream()
                                .map(c -> "  - " + c.strategy() + ": " + c.by())
                                .reduce("", (a, b) -> a + b + "\n"));
    }

    /** Single-locator resolution: cascade fallback driven by the description + a stored fingerprint. */
    public WebElement resolve(By primary, String description, ElementFingerprint baseline) {
        Optional<WebElement> found = firstUsable(primary);
        if (found.isPresent()) {
            return found.get();
        }
        if (!enabled || baseline == null) {
            throw new NoSuchElementException("Element not found and self-healing is disabled/unavailable: "
                    + description + " (" + primary + ")");
        }
        return tryCascade(description, baseline)
                .orElseThrow(() -> new NoSuchElementException(
                        "[self-heal] Primary locator and the heuristic cascade both failed for \""
                                + description + "\": " + primary));
    }

    private Optional<WebElement> tryCascade(String description, ElementFingerprint baseline) {
        String pageSource = driver.getPageSource();
        var size = driver.manage().window().getSize();
        Optional<StrategyCascade.CascadeMatch> match =
                cascade.resolve(pageSource, description, baseline, size.width, size.height);

        if (match.isEmpty()) {
            log.warn("Self-heal cascade found no candidate above threshold for \"{}\"", description);
            return Optional.empty();
        }

        StrategyCascade.CascadeMatch m = match.get();
        By healedLocator = toLocator(m.node());
        Optional<WebElement> element = firstUsable(healedLocator);
        if (element.isEmpty()) {
            // The tree said this node exists, but the live driver couldn't attach to a matching
            // live element (it moved/changed between the page-source snapshot and now). Fail
            // rather than retry indefinitely -- an unstable target is itself worth surfacing.
            return Optional.empty();
        }

        recordHeal(description, "declared-primary", "cascade:" + m.strategy(), m.score());
        return element;
    }

    private Optional<WebElement> firstUsable(By by) {
        try {
            List<WebElement> matches = driver.findElements(by);
            return matches.stream()
                    .filter(this::isUsable)
                    .findFirst();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private boolean isUsable(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /** Prefers content-desc (survives a Flutter rebuild best), then resource-id, then a structural xpath. */
    private By toLocator(PageSourceNode node) {
        if (!node.contentDesc.isEmpty()) {
            return AppiumBy.accessibilityId(node.contentDesc);
        }
        if (!node.resourceId.isEmpty()) {
            return AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"" + node.resourceId.replace("\"", "\\\"") + "\")");
        }
        if (!node.text.isEmpty()) {
            return AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + node.text.replace("\"", "\\\"") + "\")");
        }
        return By.xpath("//" + node.className + "[" + (node.indexInParent + 1) + "]");
    }

    private void recordHeal(String description, String originalStrategy, String healedStrategy, double confidence) {
        log.warn("SELF-HEAL: \"{}\" recovered via {} (was {}, confidence {})",
                description, healedStrategy, originalStrategy, String.format(java.util.Locale.ROOT, "%.2f", confidence));

        HealReporter.report(description, originalStrategy, healedStrategy, confidence);

        store.append(new HealEvent(
                System.currentTimeMillis(),
                CurrentTest.className(),
                CurrentTest.methodName(),
                description,
                originalStrategy,
                healedStrategy,
                confidence,
                CurrentTest.screenName()));
    }
}
