package com.azm.qa.mobile.core.driver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.core.config.ConfigManager;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;

/**
 * Creates and owns one Appium session per thread.
 *
 * <p>The driver is typed as {@link AppiumDriver}, not {@code AndroidDriver}, deliberately.
 * Concrete-typing the driver is cheap on day one and expensive to undo: it spreads
 * {@code AndroidDriver} through the factory, the base screen, every screen object and every
 * test, so adding iOS later means touching every file instead of adding one subclass here.
 * The handful of places that genuinely need Android-only behaviour cast locally.
 *
 * <p>A {@link ThreadLocal} is what makes {@code parallel="methods"} safe: each TestNG thread
 * gets its own session, and no thread can see another's driver.
 */
public final class DriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);

    private static final ThreadLocal<AppiumDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
    }

    public static AppiumDriver createDriver() {
        if (DRIVER.get() != null) {
            log.warn("A driver already exists on thread {}; quitting it before creating a new one",
                    Thread.currentThread().getName());
            quitDriver();
        }

        String serverUrl = ConfigManager.get("appium.server.url", "http://127.0.0.1:4723");
        AppiumDriver driver = new AndroidDriver(toUrl(serverUrl), CapabilitiesBuilder.build());

        // Explicit waits only. Mixing an implicit wait with an explicit one makes every
        // negative check (is this element gone?) pay the implicit timeout, and makes the
        // effective timeout of any wait unpredictable.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);

        DRIVER.set(driver);
        log.info("Session created on thread {} (sessionId={})",
                Thread.currentThread().getName(), driver.getSessionId());
        return driver;
    }

    public static AppiumDriver getDriver() {
        AppiumDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "No driver on thread " + Thread.currentThread().getName()
                            + " -- BaseTest creates one in @BeforeMethod. If you are seeing this from a "
                            + "utility called outside a test method, pass the driver in instead.");
        }
        return driver;
    }

    /** True when this thread currently holds a session. Lets listeners avoid throwing during teardown. */
    public static boolean hasDriver() {
        return DRIVER.get() != null;
    }

    public static void quitDriver() {
        AppiumDriver driver = DRIVER.get();
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
            log.info("Session quit on thread {}", Thread.currentThread().getName());
        } catch (Exception e) {
            // Never let a teardown failure mask the real test failure that preceded it.
            log.warn("Error quitting session on thread {}: {}",
                    Thread.currentThread().getName(), e.getMessage());
        } finally {
            DRIVER.remove();
        }
    }

    private static URL toUrl(String serverUrl) {
        try {
            return new URI(serverUrl).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalStateException(
                    "Invalid appium.server.url: '" + serverUrl
                            + "'. Appium 3 uses the bare URL with no /wd/hub suffix.", e);
        }
    }
}
