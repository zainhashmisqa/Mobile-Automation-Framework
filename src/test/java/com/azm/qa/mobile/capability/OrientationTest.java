package com.azm.qa.mobile.capability;

import org.openqa.selenium.By;
import org.openqa.selenium.ScreenOrientation;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.azm.qa.mobile.base.BaseTest;
import com.azm.qa.mobile.base.RetryAnalyzer;
import com.azm.qa.mobile.capability.orientation.OrientationUtils;
import com.azm.qa.mobile.core.config.ConfigManager;
import com.azm.qa.mobile.core.lifecycle.AppLifecycleUtils;
import com.azm.qa.mobile.core.waits.AppReadyWaits;
import com.azm.qa.mobile.report.AllureMeta;
import com.azm.qa.mobile.screens.SearchScreen;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

/**
 * S-50: rotate a key screen (search) and verify layout and state survive. Verifies via {@link
 * OrientationUtils}' adb-backed ground truth rather than only the driver's own report of the
 * orientation it was just asked to set -- see {@code capability/COVERAGE.md} for what S-50
 * still doesn't cover (the app-detail half of the scenario; no app-detail screen object exists
 * yet).
 */
public class OrientationTest extends BaseTest {

    private static final By SEARCH_FIELD = AppiumBy.androidUIAutomator(
            "new UiSelector().resourceId(\"cm.aptoide.pt:id/search_src_text\")");

    private OrientationUtils orientation;
    private SearchScreen screen;

    @BeforeMethod(alwaysRun = true)
    public void wireHelpers() {
        orientation = new OrientationUtils((AndroidDriver) driver);
        screen = new SearchScreen(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void restorePortrait() {
        // Best-effort -- never let a restore failure mask whatever the test already asserted.
        try {
            orientation.rotateTo(ScreenOrientation.PORTRAIT);
        } catch (Exception e) {
            log.warn("Could not restore portrait orientation after test: {}", e.getMessage());
        }
    }

    @Test(groups = {"regression", "smoke"},
            description = "S-50: rotating the search screen preserves layout and typed state",
            retryAnalyzer = RetryAnalyzer.class)
    public void rotatingSearchScreenPreservesLayoutAndState() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-50", "Lifecycle, Interrupts & Orientation",
                "Rotate device on key screens", "HIGH",
                "Rotates Aptoide's real search screen to landscape and back, confirming via "
                        + "PlatformActions' adb-backed rotation state -- not just the driver's own "
                        + "getOrientation() -- that the OS actually rotated, and that the search field "
                        + "stays visible with its typed text intact across the rotation."));

        String appPackage = ConfigManager.require("app.package");
        AppLifecycleUtils lifecycle = new AppLifecycleUtils(driver, appPackage);
        lifecycle.activate();
        AppReadyWaits.waitForAppReady(driver);

        screen.tap(AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/skip_button\")"), "Skip onboarding");
        screen.tap(AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/action_search\")"), "Search entry point");
        screen.type(SEARCH_FIELD, "id:cm.aptoide.pt:id/search_src_text", "aptoide");

        Assert.assertEquals(orientation.currentOrientation(), ScreenOrientation.PORTRAIT,
                "search screen should start portrait");

        orientation.rotateTo(ScreenOrientation.LANDSCAPE);

        Assert.assertTrue(orientation.isRotationConfirmed(ScreenOrientation.LANDSCAPE),
                "device should report landscape from both the driver and adb's rotation state");
        Assert.assertTrue(screen.isDisplayed(), "search screen should still be displayed after rotating to landscape");
        Assert.assertTrue(screen.expectVisible(SEARCH_FIELD).getText().contains("aptoide"),
                "typed search text should survive the rotation");

        orientation.rotateTo(ScreenOrientation.PORTRAIT);

        Assert.assertTrue(orientation.isRotationConfirmed(ScreenOrientation.PORTRAIT),
                "device should report portrait again after rotating back");
        Assert.assertTrue(screen.isDisplayed(), "search screen should still be displayed after rotating back to portrait");
    }
}
