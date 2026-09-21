package com.azm.qa.mobile.capability;

import java.time.Duration;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.azm.qa.mobile.base.BaseTest;
import com.azm.qa.mobile.base.RetryAnalyzer;
import com.azm.qa.mobile.capability.performance.PerformanceUtils;
import com.azm.qa.mobile.core.config.ConfigManager;
import com.azm.qa.mobile.core.lifecycle.AppLifecycleUtils;
import com.azm.qa.mobile.core.waits.AppReadyWaits;
import com.azm.qa.mobile.report.AllureMeta;
import com.azm.qa.mobile.screens.HomeScreen;

import io.appium.java_client.AppiumBy;

/**
 * S-02/S-03: lifecycle and cold-start-timing scenarios that only need Home's already-known
 * locators -- no new screen crawl required.
 */
public class LaunchAndLifecycleTest extends BaseTest {

    private AppLifecycleUtils lifecycle;
    private HomeScreen home;

    @BeforeMethod(alwaysRun = true)
    public void wireHelpers() {
        lifecycle = new AppLifecycleUtils(driver, ConfigManager.require("app.package"));
        home = new HomeScreen(driver);
    }

    @Test(groups = {"regression"},
            description = "S-02: backgrounding and relaunching restores the home screen state",
            retryAnalyzer = RetryAnalyzer.class)
    public void warmStartRestoresHomeScreen() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-02", "Launch, Install & Smoke",
                "Warm and hot start", "MEDIUM",
                "Backgrounds Aptoide for 5s then lets Appium bring it back to the foreground, "
                        + "asserting the real home screen's Search entry point is still displayed -- "
                        + "state restoration, not just process survival."));

        lifecycle.activate();
        AppReadyWaits.waitForAppReady(driver);
        home.tap(AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/skip_button\")"), "Skip onboarding");
        Assert.assertTrue(home.isDisplayed(), "home screen should be displayed before backgrounding");

        lifecycle.backgroundFor(Duration.ofSeconds(5));

        Assert.assertTrue(home.isDisplayed(), "home screen should still be displayed after returning from background");
    }

    @Test(groups = {"regression"},
            description = "S-03: cold-start time is measured against real app-ready readiness",
            retryAnalyzer = RetryAnalyzer.class)
    public void coldStartTimeIsMeasured() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-03", "Launch, Install & Smoke",
                "Cold-start time", "MEDIUM",
                "Terminates and relaunches Aptoide, measuring wall-clock time to AppReadyWaits' real "
                        + "readiness signal. Single-device measurement only -- the spec's full device/OS "
                        + "matrix needs more than the two phone profiles configured today (see "
                        + "capability/COVERAGE.md)."));

        PerformanceUtils performance = new PerformanceUtils(driver, lifecycle);
        Duration elapsed = performance.measureColdStart();

        int ceilingSeconds = ConfigManager.getInt("wait.appReadyTimeoutSeconds", 60);
        Assert.assertTrue(elapsed.toMillis() > 0, "cold start duration should be a real positive measurement");
        Assert.assertTrue(elapsed.getSeconds() < ceilingSeconds,
                "cold start took " + elapsed.getSeconds() + "s, at/above the " + ceilingSeconds + "s app-ready ceiling");
    }
}
