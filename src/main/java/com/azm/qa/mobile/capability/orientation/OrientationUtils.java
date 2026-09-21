package com.azm.qa.mobile.capability.orientation;

import java.time.Duration;

import org.openqa.selenium.ScreenOrientation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.capability.platform.PlatformActions;
import com.azm.qa.mobile.core.config.ConfigManager;
import com.azm.qa.mobile.core.waits.WaitUtils;

import io.appium.java_client.android.AndroidDriver;

/**
 * S-50/S-54 (rotate on key screens; tablet/phone portrait+landscape). Drives real device
 * rotation through Appium's {@code rotate} command and confirms it against {@link
 * PlatformActions}' adb-backed ground truth rather than trusting the driver's own {@code
 * getOrientation()} -- the same "don't ask the thing that made the change whether it worked"
 * reasoning {@code PermissionUtils} already applies to permission grants.
 */
public final class OrientationUtils {

    private static final Logger log = LoggerFactory.getLogger(OrientationUtils.class);

    private final AndroidDriver driver;
    private final PlatformActions platform;
    private final WaitUtils waits;

    public OrientationUtils(AndroidDriver driver) {
        this.driver = driver;
        this.platform = new PlatformActions(driver);
        this.waits = new WaitUtils(driver);
    }

    /** Rotates and waits until adb's own rotation-state read agrees, instead of returning immediately. */
    public void rotateTo(ScreenOrientation orientation) {
        driver.rotate(orientation);
        int expectedParity = orientation == ScreenOrientation.LANDSCAPE ? 1 : 0;
        waits.waitUntilTrue(
                d -> platform.screenRotationState() % 2 == expectedParity,
                Duration.ofSeconds(ConfigManager.getInt("wait.timeoutSeconds", 20)),
                "device rotation state to reach " + orientation);
        log.info("Rotated device to {}", orientation);
    }

    public ScreenOrientation currentOrientation() {
        return driver.getOrientation();
    }

    /**
     * True only when the driver-reported orientation AND the adb-verified rotation state agree
     * with {@code expected} -- rotation states 0/2 are portrait, 1/3 are landscape.
     */
    public boolean isRotationConfirmed(ScreenOrientation expected) {
        int expectedParity = expected == ScreenOrientation.LANDSCAPE ? 1 : 0;
        boolean adbAgrees = platform.screenRotationState() % 2 == expectedParity;
        return adbAgrees && driver.getOrientation() == expected;
    }
}
