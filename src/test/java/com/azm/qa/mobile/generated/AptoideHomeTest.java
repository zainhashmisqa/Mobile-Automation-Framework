package com.azm.qa.mobile.generated;

// GENERATED FILE -- do not edit by hand.
// Source: specs/aptoide-home.md
// Regenerate with: mvn exec:java -Dexec.args="md --file specs/aptoide-home.md"

import com.azm.qa.mobile.base.BaseTest;
import com.azm.qa.mobile.core.deeplink.DeepLinkUtils;
import com.azm.qa.mobile.core.gestures.GestureUtils;
import com.azm.qa.mobile.core.lifecycle.AppLifecycleUtils;
import com.azm.qa.mobile.core.permissions.PermissionUtils;
import com.azm.qa.mobile.core.context.WebViewContextUtils;
import com.azm.qa.mobile.report.AllureMeta;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AptoideHomeTest extends BaseTest {

    private com.azm.qa.mobile.screens.HomeScreen screen;
    private GestureUtils gestures;
    private AppLifecycleUtils appLifecycle;
    private PermissionUtils permissions;
    private DeepLinkUtils deepLink;
    private WebViewContextUtils webViewContext;

    @BeforeMethod(alwaysRun = true)
    public void wireHelpers() {
        screen = new com.azm.qa.mobile.screens.HomeScreen(driver);
        gestures = new GestureUtils(driver);
        appLifecycle = new AppLifecycleUtils(driver, com.azm.qa.mobile.core.config.ConfigManager.require("app.package"));
        permissions = new PermissionUtils((io.appium.java_client.android.AndroidDriver) driver, com.azm.qa.mobile.core.config.ConfigManager.require("app.package"));
        deepLink = new DeepLinkUtils((io.appium.java_client.android.AndroidDriver) driver, com.azm.qa.mobile.core.config.ConfigManager.require("app.package"));
        webViewContext = new WebViewContextUtils(driver);
    }

    @Test(groups = {"regression", "smoke"}, description = "Search entry point is reachable from home", retryAnalyzer = com.azm.qa.mobile.base.RetryAnalyzer.class)
    public void searchEntryPointIsReachableFromHome() {
        AllureMeta.annotate(AllureMeta.Meta.of("APT-HOME", "Aptoide Home", "Search entry point is reachable from home", "HIGH", "Aptoide's real home/catalog screen (cm.aptoide.pt), crawled live after skipping onboarding (see src/test/resources/locators/HomeScreen.json) -- real bottom navigation (Editorial/Search/Stores/Apps), category filter chips, and app tiles. No downloads, installs, or login involved -- just reaching the screen and using its real, safe navigation."));
        appLifecycle.activate();
        com.azm.qa.mobile.core.waits.AppReadyWaits.waitForAppReady(driver);
        screen.tap(io.appium.java_client.AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/skip_button\")"), "SKIP");
        screen.expectVisible(io.appium.java_client.AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/action_search\")"));
        screen.tap(io.appium.java_client.AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/action_search\")"), "Search");
    }

}
