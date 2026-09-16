package com.azm.qa.mobile.core.lifecycle;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;

/**
 * Install / launch / background / foreground / reset — the app-state half of Mobile
 * Automation. Kept as thin wrappers over the driver's own lifecycle API rather than shell
 * commands, so behaviour stays consistent between a local emulator and a remote device farm.
 */
public final class AppLifecycleUtils {

    private static final Logger log = LoggerFactory.getLogger(AppLifecycleUtils.class);

    private final AndroidDriver driver;
    private final String appPackage;

    public AppLifecycleUtils(AppiumDriver driver, String appPackage) {
        this.driver = (AndroidDriver) driver;
        this.appPackage = appPackage;
    }

    public void backgroundFor(Duration duration) {
        log.info("Backgrounding {} for {}s", appPackage, duration.getSeconds());
        driver.runAppInBackground(duration);
    }

    public void activate() {
        driver.activateApp(appPackage);
    }

    public void terminate() {
        driver.terminateApp(appPackage);
    }

    public boolean isInstalled() {
        return driver.isAppInstalled(appPackage);
    }

    public String state() {
        // 0 NOT_INSTALLED, 1 NOT_RUNNING, 2 RUNNING_IN_BACKGROUND_SUSPENDED,
        // 3 RUNNING_IN_BACKGROUND, 4 RUNNING_IN_FOREGROUND
        return driver.queryAppState(appPackage).toString();
    }

    /** Terminates and relaunches, without a fresh install (see appium.fullReset for that). */
    public void restart() {
        terminate();
        activate();
    }
}
