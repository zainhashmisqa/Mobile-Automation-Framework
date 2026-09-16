package com.azm.qa.mobile.core.permissions;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.core.util.AdbShellUtils;

import io.appium.java_client.android.AndroidDriver;

/**
 * Runtime permission control. Grant/revoke go through Appium's own {@code mobile: changePermissions}
 * (backed by {@code pm grant}/{@code pm revoke}), but verification goes through
 * {@link AdbShellUtils#isPermissionGranted}: asking Appium whether Appium's own grant worked
 * would only prove Appium can talk to itself, not that the OS actually changed state.
 */
public final class PermissionUtils {

    private static final Logger log = LoggerFactory.getLogger(PermissionUtils.class);

    private final AndroidDriver driver;
    private final AdbShellUtils adb;
    private final String targetPackage;

    public PermissionUtils(AndroidDriver driver, String targetPackage) {
        this.driver = driver;
        this.adb = new AdbShellUtils(driver);
        this.targetPackage = targetPackage;
    }

    public void grant(String permission) {
        driver.executeScript("mobile: changePermissions", Map.of(
                "permissions", permission,
                "appPackage", targetPackage,
                "action", "grant"));
        log.info("Granted {} to {}", permission, targetPackage);
    }

    public void revoke(String permission) {
        driver.executeScript("mobile: changePermissions", Map.of(
                "permissions", permission,
                "appPackage", targetPackage,
                "action", "revoke"));
        log.info("Revoked {} from {}", permission, targetPackage);
    }

    /** Ground truth via dumpsys — independent of whatever grant()/revoke() just requested. */
    public boolean isGranted(String permission) {
        return adb.isPermissionGranted(targetPackage, permission);
    }
}
