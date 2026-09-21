package com.azm.qa.mobile.capability.localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.capability.platform.PlatformActions;

import io.appium.java_client.android.AndroidDriver;

/**
 * S-72: device-language switch, including RTL. There is no supported Appium/UIAutomator2
 * command for changing system locale on a stock, non-rooted device; this makes a best-effort
 * attempt via adb's {@code cmd locale set-locale} (Android 12+) and reports back whether the
 * device actually took it, so a caller can log an honest skip instead of a false pass when it
 * doesn't -- exactly the pattern CLAUDE.md's mandatory honesty rule (#4) names this scenario
 * for ("locale change on non-root real devices... skip with a reason if the device refuses").
 */
public final class LocalizationUtils {

    private static final Logger log = LoggerFactory.getLogger(LocalizationUtils.class);

    private final PlatformActions platform;

    public LocalizationUtils(AndroidDriver driver) {
        this.platform = new PlatformActions(driver);
    }

    public String currentLocale() {
        return platform.shell("settings", "get", "system", "system_locales").trim();
    }

    /**
     * Best-effort; returns true only if the device's reported locale actually changed to match.
     * {@code cmd locale set-locale} doesn't exist on every API level (confirmed directly:
     * "Unknown command: set-locale" on a real API 34 emulator) -- that's exactly the "device
     * refuses" case the caller is meant to treat as an honest skip, so a thrown exception here
     * is swallowed into {@code applied=false} rather than surfacing as a test error.
     */
    public boolean attemptSetLocale(String localeTag) {
        try {
            platform.shell("cmd", "locale", "set-locale", localeTag);
        } catch (RuntimeException e) {
            log.info("Locale change command failed on this device ({}) -- treating as refused", e.getMessage());
            return false;
        }
        String actual = currentLocale();
        boolean applied = actual.contains(localeTag);
        log.info("Requested locale {} -- device now reports '{}' (applied={})", localeTag, actual, applied);
        return applied;
    }
}
