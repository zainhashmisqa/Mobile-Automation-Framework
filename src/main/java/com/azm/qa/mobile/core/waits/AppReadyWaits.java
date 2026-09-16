package com.azm.qa.mobile.core.waits;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.FluentWait;

import com.azm.qa.mobile.core.config.ConfigManager;

import io.appium.java_client.AppiumDriver;

/**
 * {@code wait for app ready} in generated tests. Flutter apps fire no document-load event and
 * expose no universal "spinner gone" locator, so this proxies readiness with page-source
 * stability: poll the tree until two consecutive reads come back identical, which is true once
 * rendering has settled and false while an animation, a shimmer, or a loading state is still
 * changing it.
 *
 * <p>Built on Selenium's {@link FluentWait} rather than a hand-rolled polling loop — this
 * framework has no engineer-written {@code Thread.sleep} anywhere, including here; the poll
 * interval below is the same explicit, configured interval every other wait in this framework
 * uses ({@code wait.pollIntervalMillis}).
 */
public final class AppReadyWaits {

    private AppReadyWaits() {
    }

    public static void waitForAppReady(AppiumDriver driver) {
        int timeoutSeconds = ConfigManager.getInt("wait.appReadyTimeoutSeconds", 60);
        int pollMillis = ConfigManager.getInt("wait.pollIntervalMillis", 250);

        AtomicReference<String> previous = new AtomicReference<>();
        FluentWait<AppiumDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(pollMillis))
                .ignoring(RuntimeException.class);

        try {
            wait.until(d -> {
                String current = safePageSource(d);
                boolean stable = current != null && current.equals(previous.get());
                previous.set(current);
                return stable;
            });
        } catch (TimeoutException e) {
            // Times out without rethrowing: "the tree was still changing after the full budget"
            // is itself useful information for whichever step runs next, and turning a readiness
            // wait into a hard failure would hide that behind this step instead of the real
            // assertion that actually cares.
        }
    }

    private static String safePageSource(AppiumDriver driver) {
        try {
            return driver.getPageSource();
        } catch (Exception e) {
            return null;
        }
    }
}
