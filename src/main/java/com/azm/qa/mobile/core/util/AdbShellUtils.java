package com.azm.qa.mobile.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.appium.java_client.android.AndroidDriver;

/**
 * Thin wrapper around the handful of things exposed more reliably via {@code adb shell} than
 * through Appium's own API: real permission-state verification and firing a deep-link intent.
 * Everything else in this framework goes through the W3C driver — this class is deliberately
 * small.
 *
 * <p>Runs through the driver's {@code executeScript("mobile: shell", ...)} rather than
 * shelling out to a local {@code adb} binary, so it works the same way against a remote
 * Appium server (a device farm) as it does against a local emulator.
 */
public final class AdbShellUtils {

    private static final Logger log = LoggerFactory.getLogger(AdbShellUtils.class);

    private final AndroidDriver driver;

    public AdbShellUtils(AndroidDriver driver) {
        this.driver = driver;
    }

    @SuppressWarnings("unchecked")
    public String shell(String command, String... args) {
        Map<String, Object> payload = Map.of(
                "command", command,
                "args", List.of(args));
        Object result = driver.executeScript("mobile: shell", payload);
        return result == null ? "" : result.toString();
    }

    /** dumpsys-backed truth, independent of whatever PermissionUtils just asked Appium to do. */
    public boolean isPermissionGranted(String packageName, String permission) {
        String dump = shell("dumpsys", "package", packageName);
        for (String line : dump.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(permission + ":")) {
                return trimmed.contains("granted=true");
            }
        }
        log.warn("Permission '{}' not found in dumpsys output for {} -- treating as not granted", permission, packageName);
        return false;
    }

    /** Fires an Android App Link / deep link the same way tapping a real link would. */
    public void openDeepLink(String uri, String packageName) {
        shell("am", "start", "-a", "android.intent.action.VIEW", "-d", uri, packageName);
    }

    /**
     * Foreground activity, e.g. "com.google.android.gms.ads.AdActivity" — used by
     * InterferenceGuard. Deliberately {@code dumpsys window}, not {@code dumpsys window
     * windows}: confirmed directly against a real API 34 device that the "windows" subcommand
     * no longer prints {@code mCurrentFocus}/{@code mFocusedApp} at all on this API level, which
     * made this method silently return "" instead of throwing — every caller (InterferenceGuard,
     * DeepLinkUtils.landedInApp()) would read a real deep link that had genuinely landed as
     * "nothing focused".
     */
    public String currentFocusedActivity() {
        String dump = shell("dumpsys", "window");
        for (String line : dump.split("\\R")) {
            if (line.contains("mCurrentFocus") || line.contains("mFocusedApp")) {
                return extractComponent(line);
            }
        }
        return "";
    }

    private static String extractComponent(String line) {
        int slash = line.lastIndexOf('/');
        if (slash < 0) {
            return "";
        }
        int start = line.lastIndexOf(' ', slash);
        int end = line.indexOf('}', slash);
        if (end < 0) {
            end = line.length();
        }
        List<Character> stop = new ArrayList<>(List.of('}', ' '));
        String candidate = line.substring(Math.max(start, 0) + 1, end).trim();
        for (char c : stop) {
            int idx = candidate.indexOf(c);
            if (idx >= 0) {
                candidate = candidate.substring(0, idx);
            }
        }
        return candidate;
    }

    public List<String> installedPackages() {
        String dump = shell("pm", "list", "packages");
        return dump.lines()
                .map(l -> l.replace("package:", "").trim())
                .filter(l -> !l.isBlank())
                .collect(Collectors.toList());
    }
}
