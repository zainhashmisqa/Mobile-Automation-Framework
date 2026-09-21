package com.azm.qa.mobile.capability;

import java.time.Duration;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.azm.qa.mobile.base.BaseTest;
import com.azm.qa.mobile.base.RetryAnalyzer;
import com.azm.qa.mobile.core.config.ConfigManager;
import com.azm.qa.mobile.core.lifecycle.AppLifecycleUtils;
import com.azm.qa.mobile.core.waits.AppReadyWaits;
import com.azm.qa.mobile.report.AllureMeta;
import com.azm.qa.mobile.screens.SearchScreen;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * S-51: background during an in-progress action, foreground after 30s, verify continuity.
 * There is no download-trigger screen object yet (see capability/COVERAGE.md), so this proves
 * the same continuity property against the real, already-crawled search screen instead: typed
 * state must survive a real 30s background/foreground cycle.
 */
public class BackgroundStateTest extends BaseTest {

    private static final By SEARCH_FIELD = AppiumBy.androidUIAutomator(
            "new UiSelector().resourceId(\"cm.aptoide.pt:id/search_src_text\")");

    private AppLifecycleUtils lifecycle;
    private SearchScreen screen;

    @BeforeMethod(alwaysRun = true)
    public void wireHelpers() {
        lifecycle = new AppLifecycleUtils(driver, ConfigManager.require("app.package"));
        screen = new SearchScreen(driver);
    }

    @Test(groups = {"regression", "smoke"},
            description = "S-51: state survives backgrounding for 30s and foregrounding again",
            retryAnalyzer = RetryAnalyzer.class)
    public void stateSurvives30sBackground() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-51", "Lifecycle, Interrupts & Orientation",
                "Background during action, foreground after 30s", "HIGH",
                "Types a real search query, backgrounds Aptoide for 30s, then confirms the query "
                        + "text survived once Appium brings it back to the foreground."));

        lifecycle.activate();
        AppReadyWaits.waitForAppReady(driver);
        screen.tap(AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/skip_button\")"), "Skip onboarding");
        screen.tap(AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/action_search\")"), "Search entry point");
        screen.type(SEARCH_FIELD, "id:cm.aptoide.pt:id/search_src_text", "aptoide");

        lifecycle.backgroundFor(Duration.ofSeconds(30));

        // screen.isDisplayed() does an immediate, non-waiting findElement -- right after
        // Appium's automatic re-foreground there's a brief render gap before the accessibility
        // tree repopulates (confirmed directly: a failure screenshot at this exact assertion
        // showed the search screen fully rendered with the typed text intact, a few hundred ms
        // after isDisplayed() had already returned false). expectVisible waits, so it doesn't
        // race the same way.
        Assert.assertTrue(screen.expectVisible(SEARCH_FIELD).getText().contains("aptoide"),
                "typed search text should survive the 30s background/foreground cycle");
        Assert.assertTrue(screen.isDisplayed(), "search screen should still be displayed after returning from background");
    }
}
