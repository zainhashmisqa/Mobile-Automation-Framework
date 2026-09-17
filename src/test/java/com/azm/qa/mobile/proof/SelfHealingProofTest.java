package com.azm.qa.mobile.proof;

import java.util.Map;
import java.util.UUID;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.azm.qa.mobile.base.BaseTest;
import com.azm.qa.mobile.base.RetryAnalyzer;
import com.azm.qa.mobile.healing.ElementFingerprint;
import com.azm.qa.mobile.healing.SelfHealingResolver;
import com.azm.qa.mobile.locators.ScreenCrawler;
import com.azm.qa.mobile.report.AllureMeta;

import io.appium.java_client.AppiumBy;

/**
 * A standing proof that self-healing actually works at runtime, not a flow that happens to
 * pass — the mobile twin of the web framework's {@code self-healing.spec.ts}. Deliberately
 * breaks a locator and asserts recovery, rather than asserting an end-to-end flow that would
 * pass the same way whether or not the healer ever engaged.
 *
 * <p>Needs no pre-known screen or locator for this app: it crawls whatever is currently on
 * screen for one real, live element and its fingerprint, constructs a primary locator
 * guaranteed to match nothing (a random accessibility id), then asks
 * {@link SelfHealingResolver} to resolve it anyway. The only way that can succeed is if the
 * cascade genuinely found the same element again via its fingerprint — which is exactly what
 * "self-healing works" means.
 */
public class SelfHealingProofTest extends BaseTest {

    @Test(groups = "regression",
            description = "The heuristic cascade recovers the same element after its primary locator is deliberately broken",
            retryAnalyzer = RetryAnalyzer.class)
    public void cascadeRecoversTheSameElementAfterPrimaryLocatorBreaks() {
        AllureMeta.annotate(AllureMeta.Meta.of("PROOF-001", "Self-healing", "Cascade recovery", "CRITICAL",
                "Breaks a real, live element's locator on purpose and asserts the cascade "
                        + "resolves it back to the same element via its stored fingerprint."));

        ScreenCrawler crawler = new ScreenCrawler(driver);
        Map<String, ElementFingerprint> found = crawler.crawl();
        Assert.assertFalse(found.isEmpty(),
                "Need at least one interactive, labelled element on the current screen to prove healing against. "
                        + "If this fails, the app hasn't reached a screen with any accessible content yet "
                        + "-- see AppLaunchTest's page-source dump for what's actually on screen.");

        Map.Entry<String, ElementFingerprint> target = found.entrySet().iterator().next();
        String description = target.getKey();
        ElementFingerprint fingerprint = target.getValue();

        // Guaranteed to match nothing real: this is the "locator broke" half of the proof.
        By deliberatelyBrokenLocator = AppiumBy.accessibilityId(
                "deliberately-broken-locator-" + UUID.randomUUID());

        SelfHealingResolver resolver = new SelfHealingResolver(driver);
        WebElement recovered = resolver.resolve(deliberatelyBrokenLocator, description, fingerprint);

        Assert.assertNotNull(recovered, "Cascade should have recovered an element for \"" + description + "\"");
        Assert.assertTrue(recovered.isDisplayed(), "Recovered element should actually be visible on screen");

        // Prove it recovered the SAME element, not merely some element: at least one of the
        // fingerprint's non-empty identifying signals must match the live recovered element.
        boolean sameElement =
                (!fingerprint.contentDesc().isEmpty() && fingerprint.contentDesc().equals(attr(recovered, "content-desc")))
                        || (!fingerprint.text().isEmpty() && fingerprint.text().equals(attr(recovered, "text")))
                        || (!fingerprint.resourceId().isEmpty() && fingerprint.resourceId().equals(attr(recovered, "resource-id")));

        Assert.assertTrue(sameElement,
                "Recovered element's attributes should match the original fingerprint for \"" + description + "\"");
    }

    private String attr(WebElement element, String name) {
        try {
            String value = element.getAttribute(name);
            return value == null ? "" : value;
        } catch (Exception e) {
            return "";
        }
    }
}
