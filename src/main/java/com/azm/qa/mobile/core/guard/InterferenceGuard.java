package com.azm.qa.mobile.core.guard;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.core.config.ConfigManager;
import com.azm.qa.mobile.core.util.AdbShellUtils;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;

/**
 * Dismisses activities that steal the foreground out from under a test, without ever hiding
 * that it happened.
 *
 * <p><b>Why this exists at all:</b> com.titanstudio.pro links Google Mobile Ads
 * ({@code AdActivity}, {@code NotificationHandlerActivity}) and Play Core
 * ({@code PlayCoreDialogWrapperActivity}). Either can appear mid-flow with no relationship
 * to the step the test just performed, and without a guard that reads as unexplainable
 * flake — a test failing on a locator that was never wrong.
 *
 * <p><b>The rule that keeps this honest:</b> every dismissal goes through
 * {@link InterruptionReporter}, never swallowed silently. An interstitial that fires on every
 * single run is a product-behaviour finding worth raising, not noise to be engineered away
 * invisibly. The reporter is thread-scoped rather than held here, because a screen object
 * constructs a fresh guard on every instantiation — see {@code BaseScreen} — so an
 * instance-level counter would lose every interruption the moment the next screen object
 * is created.
 */
public final class InterferenceGuard {

    private static final Logger log = LoggerFactory.getLogger(InterferenceGuard.class);

    private final AndroidDriver driver;
    private final AdbShellUtils adb;
    private final List<String> interruptingActivities;
    private final boolean enabled;

    public InterferenceGuard(AndroidDriver driver) {
        this.driver = driver;
        this.adb = new AdbShellUtils(driver);
        this.enabled = ConfigManager.getBoolean("guard.enabled", true);
        this.interruptingActivities = Arrays.stream(
                        ConfigManager.get("guard.interruptingActivities", "").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Checks the foreground once and dismisses a known interrupter if present. Cheap enough
     * to call from {@code BaseScreen}'s wait loop before every resolution attempt, and from a
     * {@code @BeforeMethod} hook right after launch.
     *
     * @return true if an interruption was found and handled
     */
    public boolean checkAndDismiss() {
        if (!enabled || interruptingActivities.isEmpty()) {
            return false;
        }
        String focused = adb.currentFocusedActivity();
        for (String interrupter : interruptingActivities) {
            if (focused.contains(interrupter)) {
                dismiss(interrupter, focused);
                return true;
            }
        }
        return false;
    }

    private void dismiss(String matchedActivity, String fullComponent) {
        log.warn("InterferenceGuard: {} took the foreground -- dismissing", fullComponent);

        // Ads and Play Core dialogs both close on BACK in the overwhelming majority of cases;
        // this avoids maintaining a separate close-button locator per interrupter (which would
        // itself need self-healing) for a component we don't own and can't rely on staying
        // visually stable.
        driver.pressKey(new KeyEvent(AndroidKey.BACK));

        InterruptionReporter.report(matchedActivity, fullComponent);
    }

    /** True once the interrupter is confirmed gone, or false on timeout — caller decides how to react. */
    public boolean waitUntilClear(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!checkAndDismiss()) {
                return true;
            }
        }
        return false;
    }
}
