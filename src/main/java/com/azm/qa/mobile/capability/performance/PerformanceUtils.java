package com.azm.qa.mobile.capability.performance;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.core.lifecycle.AppLifecycleUtils;
import com.azm.qa.mobile.core.waits.AppReadyWaits;

import io.appium.java_client.AppiumDriver;

/**
 * S-01/S-03: cold-start timing. Terminates the app (if running) so the next launch is a real
 * cold start, then measures wall-clock time from {@code activate()} to
 * {@link AppReadyWaits#waitForAppReady} returning -- the same readiness signal every other
 * screen in this framework waits on, so the measured window matches what a user actually
 * experiences rather than an arbitrary driver-internal event.
 */
public final class PerformanceUtils {

    private static final Logger log = LoggerFactory.getLogger(PerformanceUtils.class);

    private final AppiumDriver driver;
    private final AppLifecycleUtils lifecycle;

    public PerformanceUtils(AppiumDriver driver, AppLifecycleUtils lifecycle) {
        this.driver = driver;
        this.lifecycle = lifecycle;
    }

    public Duration measureColdStart() {
        lifecycle.terminate();
        Instant start = Instant.now();
        lifecycle.activate();
        AppReadyWaits.waitForAppReady(driver);
        Duration elapsed = Duration.between(start, Instant.now());
        log.info("Cold start took {} ms", elapsed.toMillis());
        return elapsed;
    }
}
