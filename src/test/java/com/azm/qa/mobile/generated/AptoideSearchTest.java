package com.azm.qa.mobile.generated;

// GENERATED FILE -- do not edit by hand.
// Source: specs/aptoide-search.md
// Regenerate with: mvn exec:java -Dexec.args="md --file specs/aptoide-search.md"

import com.azm.qa.mobile.base.BaseTest;
import com.azm.qa.mobile.core.deeplink.DeepLinkUtils;
import com.azm.qa.mobile.core.gestures.GestureUtils;
import com.azm.qa.mobile.core.lifecycle.AppLifecycleUtils;
import com.azm.qa.mobile.core.permissions.PermissionUtils;
import com.azm.qa.mobile.core.context.WebViewContextUtils;
import com.azm.qa.mobile.report.AllureMeta;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AptoideSearchTest extends BaseTest {

    private com.azm.qa.mobile.screens.SearchScreen screen;
    private GestureUtils gestures;
    private AppLifecycleUtils appLifecycle;
    private PermissionUtils permissions;
    private DeepLinkUtils deepLink;
    private WebViewContextUtils webViewContext;

    @BeforeMethod(alwaysRun = true)
    public void wireHelpers() {
        screen = new com.azm.qa.mobile.screens.SearchScreen(driver);
        gestures = new GestureUtils(driver);
        appLifecycle = new AppLifecycleUtils(driver, com.azm.qa.mobile.core.config.ConfigManager.require("app.package"));
        permissions = new PermissionUtils((io.appium.java_client.android.AndroidDriver) driver, com.azm.qa.mobile.core.config.ConfigManager.require("app.package"));
        deepLink = new DeepLinkUtils((io.appium.java_client.android.AndroidDriver) driver, com.azm.qa.mobile.core.config.ConfigManager.require("app.package"));
        webViewContext = new WebViewContextUtils(driver);
    }

    @Test(groups = {"regression", "smoke"}, description = "Search field accepts and reflects real input", retryAnalyzer = com.azm.qa.mobile.base.RetryAnalyzer.class)
    public void searchFieldAcceptsAndReflectsRealInput() {
        AllureMeta.annotate(AllureMeta.Meta.of("APT-SEARCH", "Aptoide Search", "Search field accepts and reflects real input", "HIGH", "Aptoide's real search screen (cm.aptoide.pt), reached via Home's real Search entry point -- crawled live via a chained navigation (skip onboarding, then tap Search). Typing into the real search field is a safe, read-only, non-destructive action -- no network call, download, or purchase is exercised, only that the real input field accepts and reflects real text."));
        appLifecycle.activate();
        com.azm.qa.mobile.core.waits.AppReadyWaits.waitForAppReady(driver);
        screen.tap(io.appium.java_client.AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/skip_button\")"), "SKIP");
        screen.tap(io.appium.java_client.AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/action_search\")"), "Search");
        screen.type(io.appium.java_client.AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/search_src_text\")"), "id:cm.aptoide.pt:id/search_src_text", "aptoide");
        org.testng.Assert.assertTrue(driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/search_src_text\")")).getText().contains("aptoide"), "id:cm.aptoide.pt:id/search_src_text should contain: aptoide");
    }

}
