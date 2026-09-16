package com.azm.qa.mobile.listeners;

import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.azm.qa.mobile.core.driver.DriverFactory;
import com.azm.qa.mobile.core.guard.InterruptionReporter;
import com.azm.qa.mobile.core.util.ScreenshotUtils;
import com.azm.qa.mobile.healing.HealReporter;

/**
 * Wires test lifecycle events to logging, failure screenshots, and the heal/interruption
 * counters that keep a self-healed or ad-interrupted run from reading as a plain, silent pass
 * in the console log. The Allure labels themselves are set live, at heal/interruption time, by
 * {@code HealReporter}/{@code InterruptionReporter} directly — not here. Confirmed directly:
 * TestNG doesn't guarantee listener order, Allure's own listener finalizes a test's result
 * before this one runs, and a label set from this class's onTestSuccess/onTestFailure attaches
 * to nothing.
 */
public class TestListener implements ITestListener {

    private static final Logger log = LoggerFactory.getLogger(TestListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        log.info(">>> START {}", testName(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        int healCount = markHealIfAny();
        markInterruptionIfAny();
        long durationMs = result.getEndMillis() - result.getStartMillis();

        // A healed pass is not the same claim as a clean pass: the locator it used wasn't the
        // one written in the code. Say so in the line itself, not only in a separate Allure
        // label someone has to go looking for.
        if (healCount > 0) {
            log.warn("<<< PASS (via self-heal x{})  {} ({} ms) -- see the Allure heal attachment(s)",
                    healCount, testName(result), durationMs);
        } else {
            log.info("<<< PASS  {} ({} ms)", testName(result), durationMs);
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        int healCount = markHealIfAny();
        markInterruptionIfAny();
        if (healCount > 0) {
            log.warn("Note: this failure happened AFTER {} self-heal(s) already occurred in this test "
                    + "-- check the heal attachment(s) before assuming the failure is unrelated to them", healCount);
        }
        log.error("<<< FAIL  {}: {}", testName(result),
                result.getThrowable() != null ? result.getThrowable().getMessage() : "");
        try {
            if (DriverFactory.hasDriver()) {
                var driver = DriverFactory.getDriver();
                ScreenshotUtils.capture((TakesScreenshot) driver, testName(result));
            }
        } catch (Exception e) {
            log.warn("Could not capture failure screenshot for {}: {}", testName(result), e.getMessage());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("<<< SKIP  {}", testName(result));
    }

    /**
     * Reads the per-thread heal counter this test accumulated (the Allure label was already
     * set live, at heal time) and returns it so the PASS/FAIL log line can say so directly.
     */
    private int markHealIfAny() {
        return HealReporter.healCountForCurrentTestAndReset();
    }

    private void markInterruptionIfAny() {
        int interruptionCount = InterruptionReporter.countForCurrentTestAndReset();
        if (interruptionCount > 0) {
            log.warn("Test was interrupted {} time(s) by an ad/dialog -- see the Allure attachment(s)", interruptionCount);
        }
    }

    private String testName(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
    }
}
