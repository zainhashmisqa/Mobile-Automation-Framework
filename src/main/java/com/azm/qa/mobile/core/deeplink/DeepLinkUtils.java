package com.azm.qa.mobile.core.deeplink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.core.util.AdbShellUtils;

import io.appium.java_client.android.AndroidDriver;

/**
 * Fires a deep link / App Link exactly the way tapping a real link would (an
 * {@code am start -a android.intent.action.VIEW} intent), then leaves verification to the
 * caller — a test should assert on where the app actually ended up, not assume the intent
 * routed anywhere.
 */
public final class DeepLinkUtils {

    private static final Logger log = LoggerFactory.getLogger(DeepLinkUtils.class);

    private final AdbShellUtils adb;
    private final String appPackage;

    public DeepLinkUtils(AndroidDriver driver, String appPackage) {
        this.adb = new AdbShellUtils(driver);
        this.appPackage = appPackage;
    }

    public void open(String uri) {
        log.info("Firing deep link: {}", uri);
        adb.openDeepLink(uri, appPackage);
    }

    /** True if the app package is the one currently in the foreground after the intent fired. */
    public boolean landedInApp() {
        return adb.currentFocusedActivity().contains(appPackage);
    }
}
