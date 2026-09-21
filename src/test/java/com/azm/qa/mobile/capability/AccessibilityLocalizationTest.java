package com.azm.qa.mobile.capability;

import java.util.List;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.azm.qa.mobile.base.BaseTest;
import com.azm.qa.mobile.base.RetryAnalyzer;
import com.azm.qa.mobile.capability.accessibility.AccessibilityUtils;
import com.azm.qa.mobile.capability.localization.LocalizationUtils;
import com.azm.qa.mobile.core.config.ConfigManager;
import com.azm.qa.mobile.core.lifecycle.AppLifecycleUtils;
import com.azm.qa.mobile.core.waits.AppReadyWaits;
import com.azm.qa.mobile.healing.PageSourceNode;
import com.azm.qa.mobile.report.AllureMeta;
import com.azm.qa.mobile.screens.HomeScreen;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

/**
 * S-70/S-71/S-72: accessibility exposure, font-scale resilience, and locale switching.
 */
public class AccessibilityLocalizationTest extends BaseTest {

    private AccessibilityUtils accessibility;
    private LocalizationUtils localization;
    private AppLifecycleUtils lifecycle;
    private HomeScreen home;

    @BeforeMethod(alwaysRun = true)
    public void wireHelpers() {
        accessibility = new AccessibilityUtils((AndroidDriver) driver);
        localization = new LocalizationUtils((AndroidDriver) driver);
        lifecycle = new AppLifecycleUtils(driver, ConfigManager.require("app.package"));
        home = new HomeScreen(driver);

        lifecycle.activate();
        AppReadyWaits.waitForAppReady(driver);
        home.tap(AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"cm.aptoide.pt:id/skip_button\")"), "Skip onboarding");
    }

    @AfterMethod(alwaysRun = true)
    public void resetFontScale() {
        try {
            accessibility.setFontScale(1.0);
        } catch (Exception e) {
            log.warn("Could not reset font_scale after test: {}", e.getMessage());
        }
    }

    @Test(groups = {"regression"},
            description = "S-70: interactive home-screen controls expose a content-desc or text",
            retryAnalyzer = RetryAnalyzer.class)
    public void homeScreenControlsExposeAccessibleNames() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-70", "Accessibility & Localization",
                "TalkBack reachability", "MEDIUM",
                "Reads the real home screen's page source and asserts every clickable node exposes "
                        + "a non-blank content-desc or text -- TalkBack's actual data source. Whether "
                        + "the TalkBack service itself is switched on isn't observable via Appium/adb, "
                        + "so this checks what TalkBack would announce, not the service toggle."));

        List<PageSourceNode> offenders = accessibility.clickableNodesMissingAccessibleName();
        Assert.assertTrue(offenders.isEmpty(),
                offenders.size() + " clickable element(s) on the home screen expose neither "
                        + "content-desc nor text, so TalkBack would announce them as unlabeled");
    }

    @Test(groups = {"regression"},
            description = "S-71: increased font scale does not hide the home screen's search entry point",
            retryAnalyzer = RetryAnalyzer.class)
    public void largeFontScaleDoesNotBreakHomeScreen() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-71", "Accessibility & Localization",
                "Font scale resilience", "MEDIUM",
                "Sets system font_scale to 1.3 ('Large' text) and confirms the home screen's real "
                        + "Search entry point is still displayed -- a coarse but real clipped/overlapping "
                        + "check, not a pixel-level layout assertion."));

        accessibility.setFontScale(1.3);
        lifecycle.restart();
        AppReadyWaits.waitForAppReady(driver);

        Assert.assertTrue(home.isDisplayed(),
                "home screen's search entry point should still be displayed at 1.3x font scale");
    }

    @Test(groups = {"regression"},
            description = "S-72: device locale switch, honestly skipped if the emulator refuses",
            retryAnalyzer = RetryAnalyzer.class)
    public void deviceLocaleSwitchAppliesOrIsSkipped() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-72", "Accessibility & Localization",
                "Locale / RTL switch", "MEDIUM",
                "Best-effort locale switch to 'ar' (RTL) via adb's `cmd locale set-locale`. Per the "
                        + "mandatory honesty rule, this logs a skip rather than a false pass if the "
                        + "device doesn't actually apply it."));

        String original = localization.currentLocale();
        boolean applied = localization.attemptSetLocale("ar");
        if (!applied) {
            throw new SkipException("Device did not apply locale 'ar' via adb (reported: '"
                    + localization.currentLocale() + "') -- no root/non-standard-API-level fallback "
                    + "is wired into LocalizationUtils. See CLAUDE.md's honesty rule #4.");
        }

        Assert.assertTrue(home.isDisplayed(), "home screen should still render after the locale switch");
        localization.attemptSetLocale(original.isBlank() ? "en-US" : original);
    }
}
