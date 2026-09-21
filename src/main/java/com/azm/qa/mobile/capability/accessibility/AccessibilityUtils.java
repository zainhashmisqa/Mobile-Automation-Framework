package com.azm.qa.mobile.capability.accessibility;

import java.util.ArrayList;
import java.util.List;

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
        PageSourceNode root = PageSourceParser.parse(driver.getPageSource());
        List<PageSourceNode> offenders = new ArrayList<>();
        collectOffenders(root, offenders);
        return offenders;
    }

    private void collectOffenders(PageSourceNode node, List<PageSourceNode> offenders) {
        if (node.clickable && node.contentDesc.isBlank() && node.text.isBlank()) {
            offenders.add(node);
        }
        for (PageSourceNode child : node.children) {
            collectOffenders(child, offenders);
        }
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
