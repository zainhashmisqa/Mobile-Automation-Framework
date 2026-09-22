package com.azm.qa.mobile.capability;

import org.openqa.selenium.TakesScreenshot;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.azm.qa.mobile.base.BaseTest;
import com.azm.qa.mobile.base.RetryAnalyzer;
import com.azm.qa.mobile.capability.visual.VisualRegressionUtils;
import com.azm.qa.mobile.core.config.ConfigManager;
import com.azm.qa.mobile.core.lifecycle.AppLifecycleUtils;
import com.azm.qa.mobile.core.waits.AppReadyWaits;
import com.azm.qa.mobile.report.AllureMeta;
import com.azm.qa.mobile.screens.HomeScreen;
import com.azm.qa.mobile.screens.OnboardingScreen;

/**
 * S-80: baseline screenshot-diff on the home screen. Single device/density only -- the spec's
 * "2-3 device densities" needs more profiles than the two phone profiles configured today (see
 * capability/COVERAGE.md). First run on a machine always captures the baseline and passes
 * trivially; only a run against an already-captured baseline is a real regression check.
 */
public class VisualRegressionTest extends BaseTest {

    private static final String BASELINE_KEY = "home-screen";
    private static final double MAX_DIFF_RATIO = 0.05;

    private OnboardingScreen onboarding;
    private HomeScreen home;

    @BeforeMethod(alwaysRun = true)
    public void wireHelpers() {
        onboarding = new OnboardingScreen(driver);
        home = new HomeScreen(driver);
    }

    @Test(groups = {"regression"},
            description = "S-80: home screen matches its captured visual baseline",
            retryAnalyzer = RetryAnalyzer.class)
    public void homeScreenMatchesVisualBaseline() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-80", "Visual Regression & Cross-Device",
                "Baseline screenshot-diff", "MEDIUM",
                "Compares the real home screen against a captured baseline PNG using a plain "
                        + "pixel-difference ratio (no external visual-diff dependency)."));

        AppLifecycleUtils lifecycle = new AppLifecycleUtils(driver, ConfigManager.require("app.package"));
        lifecycle.activate();
        AppReadyWaits.waitForAppReady(driver);
        onboarding.tapSkip();
        Assert.assertTrue(home.isDisplayed(), "home screen should be displayed before capturing a screenshot");

        TakesScreenshot shooter = (TakesScreenshot) driver;
        boolean capturedNewBaseline = VisualRegressionUtils.captureBaselineIfAbsent(shooter, BASELINE_KEY);
        if (capturedNewBaseline) {
            log.info("No visual baseline existed for '{}' -- captured one; this run cannot regression-check against it", BASELINE_KEY);
            return;
        }

        double diffRatio = VisualRegressionUtils.diffRatio(shooter, BASELINE_KEY);
        Assert.assertTrue(diffRatio <= MAX_DIFF_RATIO,
                String.format("home screen differs from its visual baseline by %.1f%%, above the %.0f%% threshold",
                        diffRatio * 100, MAX_DIFF_RATIO * 100));
    }
}
