package com.azm.qa.mobile.generated;

// GENERATED FILE -- do not edit by hand.
// Source: specs/aptoide-onboarding.md
// Regenerate with: mvn exec:java -Dexec.args="md --file specs/aptoide-onboarding.md"

import com.azm.qa.mobile.base.BaseTest;
import com.azm.qa.mobile.core.deeplink.DeepLinkUtils;
import com.azm.qa.mobile.core.gestures.GestureUtils;
import com.azm.qa.mobile.core.lifecycle.AppLifecycleUtils;
import com.azm.qa.mobile.core.permissions.PermissionUtils;
import com.azm.qa.mobile.core.context.WebViewContextUtils;
import com.azm.qa.mobile.report.AllureMeta;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AptoideOnboardingTest extends BaseTest {

    private com.azm.qa.mobile.screens.OnboardingScreen screen;
    private GestureUtils gestures;
    private AppLifecycleUtils appLifecycle;
    private PermissionUtils permissions;
    private DeepLinkUtils deepLink;
    private WebViewContextUtils webViewContext;

    @BeforeMethod(alwaysRun = true)
    public void wireHelpers() {
        screen = new com.azm.qa.mobile.screens.OnboardingScreen(driver);
        gestures = new GestureUtils(driver);
        appLifecycle = new AppLifecycleUtils(driver, com.azm.qa.mobile.core.config.ConfigManager.require("app.package"));
        permissions = new PermissionUtils((io.appium.java_client.android.AndroidDriver) driver, com.azm.qa.mobile.core.config.ConfigManager.require("app.package"));
        deepLink = new DeepLinkUtils((io.appium.java_client.android.AndroidDriver) driver, com.azm.qa.mobile.core.config.ConfigManager.require("app.package"));
        webViewContext = new WebViewContextUtils(driver);
    }

    @Test(groups = {"regression", "smoke"}, description = "Skip button dismisses the real onboarding wizard")
    public void skipButtonDismissesTheRealOnboardingWizard() {
        AllureMeta.annotate(AllureMeta.Meta.of("APT-ONBOARD", "Aptoide Onboarding", "Skip button dismisses the real onboarding wizard", "CRITICAL", "The real, first-run onboarding wizard for cm.aptoide.pt (v9.22.5.3), crawled live from the actual app (see src/test/resources/locators/OnboardingScreen.json) -- no server-side login, no seeded data, nothing beyond what a fresh install genuinely shows."));
        appLifecycle.activate();
        com.azm.qa.mobile.core.waits.AppReadyWaits.waitForAppReady(driver);
        screen.expectVisible(io.appium.java_client.AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/title\")"));
        screen.tap(io.appium.java_client.AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/skip_button\")"), "SKIP");
        org.testng.Assert.assertTrue(screen.expectHidden(io.appium.java_client.AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/title\")")), "expected hidden: id:cm.aptoide.pt:id/title");
    }

}
