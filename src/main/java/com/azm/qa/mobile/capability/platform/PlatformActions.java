package com.azm.qa.mobile.capability.platform;

import com.azm.qa.mobile.core.util.AdbShellUtils;

import io.appium.java_client.android.AndroidDriver;

/**
 * Single audited seam for every OS-level effect a capability class needs. Nothing under
 * {@code capability/} calls adb directly or builds its own {@link AdbShellUtils} -- it goes
 * through here. Kept deliberately small: it grows one method at a time, as a real capability
 * class needs a real OS-level read or effect, never speculatively ahead of a consumer.
 */
public final class PlatformActions {

    private final AdbShellUtils adb;

    public PlatformActions(AndroidDriver driver) {
        this.adb = new AdbShellUtils(driver);
    }

    /** Raw escape hatch for a capability class that needs a shell command not yet wrapped below. */
    public String shell(String command, String... args) {
        return adb.shell(command, args);
    }

    /**
     * Ground-truth device rotation via {@code settings get system user_rotation}: 0/1/2/3, each
     * step 90 degrees clockwise from natural (portrait) orientation. Read independently of
     * whatever the driver's own {@code rotate()} call just requested -- the same
     * ask-then-verify-via-adb pattern {@code PermissionUtils}/{@code AdbShellUtils} already use
     * for permission grants, applied to rotation.
     */
    public int screenRotationState() {
        String raw = adb.shell("settings", "get", "system", "user_rotation").trim();
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Unexpected user_rotation value from device: '" + raw + "'", e);
        }
    }
}
