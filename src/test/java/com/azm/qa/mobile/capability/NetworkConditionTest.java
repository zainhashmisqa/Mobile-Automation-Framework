package com.azm.qa.mobile.capability;

import java.time.Duration;

import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.azm.qa.mobile.base.BaseTest;
import com.azm.qa.mobile.base.RetryAnalyzer;
import com.azm.qa.mobile.capability.network.NetworkConditionUtils;
import com.azm.qa.mobile.core.config.ConfigManager;
import com.azm.qa.mobile.core.lifecycle.AppLifecycleUtils;
import com.azm.qa.mobile.core.waits.AppReadyWaits;
import com.azm.qa.mobile.report.AllureMeta;

import io.appium.java_client.android.AndroidDriver;

/**
 * S-40/S-41/S-42: real network-state changes via Appium's {@code setConnection}. S-43
 * (throttled network) is a logged skip -- see {@link NetworkConditionUtils}' javadoc for why.
 */
public class NetworkConditionTest extends BaseTest {

    private NetworkConditionUtils network;
    private AppLifecycleUtils lifecycle;

    @BeforeMethod(alwaysRun = true)
    public void wireHelpers() {
        network = new NetworkConditionUtils((AndroidDriver) driver);
        lifecycle = new AppLifecycleUtils(driver, ConfigManager.require("app.package"));
    }

    @AfterMethod(alwaysRun = true)
    public void restoreConnectivity() {
        try {
            network.setWifiAndData(true, true);
        } catch (Exception e) {
            log.warn("Could not restore connectivity after test: {}", e.getMessage());
        }
    }

    @Test(groups = {"regression", "smoke"},
            description = "S-40: toggling airplane mode does not crash the app",
            retryAnalyzer = RetryAnalyzer.class)
    public void airplaneModeToggleDoesNotCrashApp() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-40", "Network Conditions & Offline",
                "Airplane mode toggle", "HIGH",
                "Toggles airplane mode on then off while Aptoide is foregrounded, asserting the app "
                        + "stays alive throughout. Not exercised mid-download specifically -- no "
                        + "download-trigger screen object exists yet."));

        lifecycle.activate();
        AppReadyWaits.waitForAppReady(driver);

        network.setAirplaneMode(true);
        assertAppAlive();

        network.setAirplaneMode(false);
        assertAppAlive();
    }

    @Test(groups = {"regression", "smoke"},
            description = "S-41: launching fully offline does not crash the app",
            retryAnalyzer = RetryAnalyzer.class)
    public void launchingOfflineDoesNotCrashApp() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-41", "Network Conditions & Offline",
                "Fully offline launch", "HIGH",
                "Disables Wi-Fi and data, terminates and relaunches Aptoide, and confirms it still "
                        + "reaches the foreground -- offline-cached-content rendering itself is not "
                        + "asserted, since that needs a known offline-state locator this repo doesn't "
                        + "have yet."));

        network.setWifiAndData(false, false);
        lifecycle.terminate();
        lifecycle.activate();
        assertAppAlive();
    }

    @Test(groups = {"regression"},
            description = "S-42: switching Wi-Fi to cellular mid-session does not crash the app",
            retryAnalyzer = RetryAnalyzer.class)
    public void wifiToCellularSwitchDoesNotCrashApp() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-42", "Network Conditions & Offline",
                "Wi-Fi to cellular switch", "MEDIUM",
                "Switches from Wi-Fi to data-only while Aptoide is foregrounded and confirms it "
                        + "survives the transition."));

        lifecycle.activate();
        AppReadyWaits.waitForAppReady(driver);

        network.setWifiAndData(true, false);
        assertAppAlive();

        network.setWifiAndData(false, true);
        assertAppAlive();
    }

    @Test(groups = {"regression"}, description = "S-43: throttled network -- not exercisable on this setup")
    public void throttledNetworkIsNotExercisable() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-43", "Network Conditions & Offline",
                "Slow/throttled network", "MEDIUM",
                "Logged skip, not a fake pass -- see the mandatory honesty rule (CLAUDE.md #4): "
                        + "network throttling is named there as a condition to skip with a reason."));
        throw new SkipException("No supported throttling primitive on a stock UIAutomator2 emulator -- "
                + "would need a proxy (e.g. Toxiproxy) or a real device farm's network-shaping feature, "
                + "neither of which is wired into this framework yet.");
    }

    private void assertAppAlive() {
        waits.waitUntilTrue(d -> lifecycle.state().contains("RUNNING_IN_FOREGROUND"),
                Duration.ofSeconds(ConfigManager.getInt("wait.appReadyTimeoutSeconds", 60)),
                "app state to reach RUNNING_IN_FOREGROUND after network change");
    }
}
