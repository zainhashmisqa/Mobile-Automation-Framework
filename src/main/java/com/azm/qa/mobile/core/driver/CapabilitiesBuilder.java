package com.azm.qa.mobile.core.driver;

import java.io.File;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.core.config.ConfigManager;
import com.azm.qa.mobile.core.config.DeviceProfile;

import io.appium.java_client.android.options.UiAutomator2Options;

/**
 * Builds the Appium session capabilities. This is the <em>only</em> place capabilities are
 * constructed — screens and tests never touch them, so changing how a session is created is
 * a one-file change.
 *
 * <p>Reads the device-specific half from the active {@link DeviceProfile} and the app/session
 * half from {@link ConfigManager}, both of which are thread-scoped, so building capabilities
 * on two threads at once yields two genuinely different sessions.
 */
public final class CapabilitiesBuilder {

    private static final Logger log = LoggerFactory.getLogger(CapabilitiesBuilder.class);

    private CapabilitiesBuilder() {
    }

    public static UiAutomator2Options build() {
        DeviceProfile profile = DeviceProfile.active();
        UiAutomator2Options options = new UiAutomator2Options();

        options.setPlatformName(profile.getPlatformName());
        options.setAutomationName(profile.getAutomationName());

        if (profile.getDeviceName() != null) {
            options.setDeviceName(profile.getDeviceName());
        }
        // platformVersion is intentionally optional. Pinning it makes the profile fail against
        // any emulator but the exact one it was written for; leaving it unset lets Appium
        // auto-detect whatever is actually attached.
        if (profile.getPlatformVersion() != null) {
            options.setPlatformVersion(profile.getPlatformVersion());
        }
        if (profile.getUdid() != null) {
            options.setUdid(profile.getUdid());
        }

        // Ports come from DevicePool, not the profile: a hand-picked, hardcoded port per named
        // profile only stays unique for as long as everyone remembers to pick a new one for
        // every new entry. Deriving all three from the calling thread removes that failure mode
        // outright -- two sessions sharing a systemPort fail with "instrumentation process
        // cannot be initialized", which reads exactly like device flake and isn't.
        DevicePool.PortSlot ports = DevicePool.acquireSlotForCurrentThread();
        options.setSystemPort(ports.systemPort());
        options.setMjpegServerPort(ports.mjpegServerPort());
        options.setChromedriverPort(ports.chromedriverPort());

        options.setAppPackage(ConfigManager.require("app.package"));
        // app.activity is deliberately optional: UiAutomator2 resolves the package's own
        // default launcher activity when it's omitted, so a new app.package can be pointed at
        // before its real launcher activity is known/verified -- required only when a package
        // has more than one launcher activity and the default guess is wrong.
        String appActivity = ConfigManager.get("app.activity");
        if (appActivity != null) {
            options.setAppActivity(appActivity);
        }

        // app.path is optional: without it Appium attaches to the already-installed build,
        // which is what you want when iterating locally against an app you installed once.
        String appPath = ConfigManager.get("app.path");
        if (appPath != null) {
            File apk = new File(appPath);
            if (!apk.isFile()) {
                throw new IllegalStateException("app.path is set but no file exists there: " + apk.getAbsolutePath());
            }
            options.setApp(apk.getAbsolutePath());
        } else {
            log.info("app.path not set -- attaching to the installed build of {}",
                    ConfigManager.get("app.package"));
        }

        options.setNoReset(ConfigManager.getBoolean("appium.noReset", false));
        options.setFullReset(ConfigManager.getBoolean("appium.fullReset", false));
        options.setNewCommandTimeout(
                Duration.ofSeconds(ConfigManager.getInt("appium.newCommandTimeoutSec", 120)));
        options.setAutoGrantPermissions(
                ConfigManager.getBoolean("appium.autoGrantPermissions", true));

        // Flutter apps take noticeably longer to reach their first real frame than native ones,
        // and a too-tight launch budget surfaces as a session-creation failure rather than the
        // "app is still starting" that it actually is.
        options.setAppWaitActivity("*");
        options.setAppWaitDuration(
                Duration.ofSeconds(ConfigManager.getInt("wait.appReadyTimeoutSeconds", 60)));

        log.info("Capabilities built for profile '{}' ({})", profile.getName(), profile);
        return options;
    }
}
