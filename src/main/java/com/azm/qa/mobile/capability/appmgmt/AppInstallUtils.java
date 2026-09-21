package com.azm.qa.mobile.capability.appmgmt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.capability.platform.PlatformActions;

import io.appium.java_client.android.AndroidDriver;

/**
 * S-04/S-53: install/upgrade and low-storage handling. Only the parts that are safe and
 * honest to automate against a shared CI emulator are implemented here -- see
 * {@code capability/COVERAGE.md} for why S-04 (needs a previous-build artifact this repo
 * doesn't have) and S-53 (deliberately filling emulator storage risks the runner, not just the
 * test) are logged skips rather than exercised end-to-end.
 */
public final class AppInstallUtils {

    private static final Logger log = LoggerFactory.getLogger(AppInstallUtils.class);

    private final AndroidDriver driver;
    private final PlatformActions platform;
    private final String appPackage;

    public AppInstallUtils(AndroidDriver driver, String appPackage) {
        this.driver = driver;
        this.platform = new PlatformActions(driver);
        this.appPackage = appPackage;
    }

    public void installFrom(String apkPath) {
        driver.installApp(apkPath);
        log.info("Installed {} from {}", appPackage, apkPath);
    }

    /** versionName from dumpsys, e.g. "9.22.5.3" -- used to confirm an upgrade actually changed the installed build. */
    public String installedVersionName() {
        String dump = platform.shell("dumpsys", "package", appPackage);
        for (String line : dump.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("versionName=")) {
                return trimmed.substring("versionName=".length());
            }
        }
        return "";
    }
}
