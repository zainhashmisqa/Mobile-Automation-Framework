package com.azm.qa.mobile.locators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.healing.ElementFingerprint;
import com.azm.qa.mobile.healing.PageSourceNode;
import com.azm.qa.mobile.healing.PageSourceParser;

import io.appium.java_client.AppiumDriver;

/**
 * The mobile twin of the web framework's {@code LocatorAgent}: a pure page-source crawler
 * (no AI, no vision model) that visits the currently-open screen, finds every interactive
 * element, and ranks a description + fingerprint for each — the same signals
 * {@code StrategyCascade} scores against at healing time.
 *
 * <p>Backs the {@code /gen-screen} slash command and {@code mvn exec:java -Dexec.args="crawl
 * --screen &lt;name&gt;"}: point it at a live screen once, review what it found, and commit the
 * result to {@code locators/&lt;screen&gt;.json} as that screen's baseline — the human-reviewed
 * knowledge base a {@code screen-map.json}-style workflow depends on, and never invented.
 */
public final class ScreenCrawler {

    private static final Logger log = LoggerFactory.getLogger(ScreenCrawler.class);

    private final AppiumDriver driver;

    public ScreenCrawler(AppiumDriver driver) {
        this.driver = driver;
    }

    /** Description -&gt; fingerprint for every interactive, uniquely-identifiable node on the current screen. */
    public Map<String, ElementFingerprint> crawl() {
        String pageSource = driver.getPageSource();
        Dimension screenSize = driver.manage().window().getSize();
        PageSourceNode root = PageSourceParser.parse(pageSource);

        Map<String, ElementFingerprint> found = new LinkedHashMap<>();
        List<PageSourceNode> interactive = new ArrayList<>();
        for (PageSourceNode node : root.flatten()) {
            if (node.clickable && (!node.contentDesc.isEmpty() || !node.text.isEmpty() || !node.resourceId.isEmpty())) {
                interactive.add(node);
            }
        }

        for (PageSourceNode node : interactive) {
            String description = describe(node);
            String key = uniqueKey(found, description);
            found.put(key, ElementFingerprint.capture(node, screenSize.width, screenSize.height));
        }

        log.info("Crawled {} interactive element(s) with a stable label", found.size());
        return found;
    }

    /** Prefers content-desc (accessibility id -- this app's primary strategy), then text, then resource-id. */
    private String describe(PageSourceNode node) {
        if (!node.contentDesc.isEmpty()) {
            return node.contentDesc;
        }
        if (!node.text.isEmpty()) {
            return node.text;
        }
        return node.className + "#" + node.resourceId;
    }

    private String uniqueKey(Map<String, ElementFingerprint> existing, String description) {
        if (!existing.containsKey(description)) {
            return description;
        }
        int suffix = 2;
        while (existing.containsKey(description + " (" + suffix + ")")) {
            suffix++;
        }
        return description + " (" + suffix + ")";
    }
}
