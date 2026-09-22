package com.azm.qa.mobile.capability.accessibility;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.capability.platform.PlatformActions;
import com.azm.qa.mobile.healing.PageSourceNode;
import com.azm.qa.mobile.healing.PageSourceParser;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;

/**
 * S-70/S-71: accessibility exposure and font-scale resilience. Toggling the real TalkBack
 * service and reading back what it would announce isn't observable through Appium/adb --
 * TalkBack speaks through the accessibility framework, not the UiAutomator2 tree -- so what's
 * actually checkable here is TalkBack's real data source: whether interactive elements expose
 * a non-blank content-desc/text in the page source at all. See {@code capability/COVERAGE.md}
 * for that scope note.
 *
 * <p>The scanning logic ({@link #findClickableNodesMissingAccessibleName}) is a pure function
 * over a {@link PageSourceNode} tree, deliberately kept separate from the driver-dependent read
 * in {@link #clickableNodesMissingAccessibleName()} -- the same split {@code PageSourceParser}
 * already uses so it's testable against a canned XML string with no emulator, not just exercised
 * live.
 */
public final class AccessibilityUtils {

    private static final Logger log = LoggerFactory.getLogger(AccessibilityUtils.class);

    private final AppiumDriver driver;
    private final PlatformActions platform;

    public AccessibilityUtils(AndroidDriver driver) {
        this.driver = driver;
        this.platform = new PlatformActions(driver);
    }

    /** Clickable nodes in the current page source with no content-desc AND no text -- unreachable to TalkBack. */
    public List<PageSourceNode> clickableNodesMissingAccessibleName() {
        return findClickableNodesMissingAccessibleName(PageSourceParser.parse(driver.getPageSource()));
    }

    /** Pure: no driver dependency, so it's directly unit-testable against a parsed tree. */
    public static List<PageSourceNode> findClickableNodesMissingAccessibleName(PageSourceNode root) {
        return root.flatten().stream()
                .filter(node -> node.clickable && node.contentDesc.isBlank() && node.text.isBlank())
                .collect(Collectors.toList());
    }

    /** {@code settings put system font_scale <scale>} -- 1.0 is default, e.g. 1.3 for "Large" text. */
    public void setFontScale(double scale) {
        platform.shell("settings", "put", "system", "font_scale", String.valueOf(scale));
        log.info("System font_scale set to {}", scale);
    }

    public double currentFontScale() {
        String raw = platform.shell("settings", "get", "system", "font_scale").trim();
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }
}
