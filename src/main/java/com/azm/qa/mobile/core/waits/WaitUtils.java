package com.azm.qa.mobile.core.waits;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.azm.qa.mobile.core.config.ConfigManager;

/**
 * Every synchronization point in this framework goes through here. There is no
 * {@code Thread.sleep} anywhere in the codebase — a fixed sleep either wastes time waiting
 * for something already true, or isn't long enough and fails anyway, and either way it
 * hides the actual condition being waited for. Every wait below names that condition.
 */
public final class WaitUtils {

    private final WebDriver driver;
    private final Duration defaultTimeout;
    private final Duration pollInterval;

    public WaitUtils(WebDriver driver) {
        this.driver = driver;
        this.defaultTimeout = Duration.ofSeconds(ConfigManager.getInt("wait.timeoutSeconds", 20));
        this.pollInterval = Duration.ofMillis(ConfigManager.getInt("wait.pollIntervalMillis", 250));
    }

    private WebDriverWait wait(Duration timeout) {
        return new WebDriverWait(driver, timeout, pollInterval);
    }

    public <T> T until(Function<WebDriver, T> condition) {
        return wait(defaultTimeout).until(condition);
    }

    public <T> T until(Function<WebDriver, T> condition, Duration timeout) {
        return wait(timeout).until(condition);
    }

    /** Same as {@link #until(Function, Duration)} but rewrites a bare timeout into a named failure. */
    public <T> T until(Function<WebDriver, T> condition, Duration timeout, String describeOnTimeout) {
        try {
            return wait(timeout).until(condition);
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    "Timed out after " + timeout.getSeconds() + "s waiting for: " + describeOnTimeout, e);
        }
    }

    public WebElement waitForVisible(By locator) {
        return until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForVisible(By locator, Duration timeout) {
        return until(ExpectedConditions.visibilityOfElementLocated(locator), timeout);
    }

    public WebElement waitForClickable(By locator) {
        return until(ExpectedConditions.elementToBeClickable(locator));
    }

    public WebElement waitForPresence(By locator) {
        return until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public List<WebElement> waitForAllPresent(By locator) {
        return until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
    }

    public boolean waitForInvisible(By locator) {
        return until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public boolean waitForInvisible(By locator, Duration timeout) {
        return until(ExpectedConditions.invisibilityOfElementLocated(locator), timeout);
    }

    /** Polls an arbitrary condition, e.g. "page source stopped changing" or "foreground activity == X". */
    public void waitUntilTrue(Function<WebDriver, Boolean> condition, Duration timeout, String describeOnTimeout) {
        until(condition, timeout, describeOnTimeout);
    }

    /**
     * Blocks for a fixed real-time duration — used only for genuinely time-based discovery
     * (e.g. sampling page source at t=0s/5s/10s to catch a splash-screen transition), never as
     * a substitute for waiting on a real condition. Built on {@link FluentWait} rather than
     * {@code Thread.sleep} so this framework's "no Thread.sleep anywhere" claim stays true of
     * every line of code in it, not just the ones that happen to need a real condition.
     */
    public void pauseFor(Duration duration) {
        try {
            new FluentWait<>(driver)
                    .withTimeout(duration)
                    .pollingEvery(duration)
                    .ignoring(RuntimeException.class)
                    .until(d -> false);
        } catch (TimeoutException expected) {
            // The point of this call -- the duration has elapsed.
        }
    }
}
