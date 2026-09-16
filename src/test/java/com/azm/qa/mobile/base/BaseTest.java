package com.azm.qa.mobile.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.azm.qa.mobile.core.config.ConfigManager;
import com.azm.qa.mobile.core.driver.DriverFactory;
import com.azm.qa.mobile.core.waits.WaitUtils;
import com.azm.qa.mobile.data.TestDataStore;
import com.azm.qa.mobile.healing.CurrentTest;
import com.azm.qa.mobile.report.AllureEnvironment;

import io.appium.java_client.AppiumDriver;

/**
 * Every test class extends this. Fresh driver + fresh app state per test method (Test
 * Independence, T4) — {@code @BeforeMethod}/{@code @AfterMethod} rather than class-level setup,
 * so one test's state can never leak into the next, including across retries.
 *
 * <p>The {@code device} TestNG parameter, when supplied by a suite XML, scopes
 * {@code device.profile} to this thread only (see {@code ConfigManager}) — that per-thread
 * scoping is what makes {@code parallel="methods"} with more than one device profile safe:
 * two threads reading a single process-wide profile would race and both end up on whichever
 * one was set last.
 *
 * <p>Teardown runs {@link TestDataStore#cleanupAllForCurrentThread()} <em>before</em> the
 * driver quits, so a registered cleanup handler can still drive the UI (or call an API) to
 * remove whatever this test created via {@link TestDataStore#save}.
 */
public abstract class BaseTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseTest.class);

    protected AppiumDriver driver;
    protected WaitUtils waits;

    @BeforeMethod(alwaysRun = true)
    @Parameters("device")
    public void setUp(@Optional String device, java.lang.reflect.Method method) {
        if (device != null && !device.isBlank()) {
            ConfigManager.setThreadOverride("device.profile", device);
        }
        CurrentTest.set(getClass().getSimpleName(), method.getName());

        driver = DriverFactory.createDriver();
        waits = new WaitUtils(driver);

        // Captures the real negotiated session details into the Allure report once per JVM.
        AllureEnvironment.writeOnce(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        try {
            TestDataStore.CleanupSummary cleanup = TestDataStore.cleanupAllForCurrentThread();
            if (cleanup.attempted() > 0) {
                log.info("Cleanup: {}/{} succeeded, {} skipped (no handler registered)",
                        cleanup.succeeded(), cleanup.attempted(), cleanup.skippedNoHandler());
            }
        } catch (Exception e) {
            // A cleanup failure is a real problem worth logging loudly, but it must never mask
            // whatever the test itself already asserted, or take the driver-quit step below
            // down with it.
            log.warn("Test data cleanup threw ({}) -- driver teardown still proceeds", e.getMessage());
        } finally {
            TestDataStore.clearForCurrentThread();
        }

        DriverFactory.quitDriver();
        ConfigManager.clearThreadOverrides();
        CurrentTest.clear();
    }
}
