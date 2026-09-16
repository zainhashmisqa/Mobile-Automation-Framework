package com.azm.qa.mobile.screens;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.core.gestures.GestureUtils;
import com.azm.qa.mobile.core.guard.InterferenceGuard;
import com.azm.qa.mobile.core.waits.WaitUtils;
import com.azm.qa.mobile.healing.CurrentTest;
import com.azm.qa.mobile.healing.ElementFingerprint;
import com.azm.qa.mobile.healing.LocatorCandidate;
import com.azm.qa.mobile.healing.SelfHealingResolver;
import com.azm.qa.mobile.locators.LocatorRepository;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

/**
 * Every screen object extends this and gets self-healing, gestures, waits and the ad/dialog
 * interference guard for free — the same shape as the web framework's {@code BasePage}.
 *
 * <p><b>Actions heal, assertions don't.</b> {@link #tap}/{@link #type} resolve through
 * {@link SelfHealingResolver}. {@link #expectVisible}/{@link #expectHidden} use the raw
 * locator with an ordinary explicit wait — an assertion that healed onto a looser locator
 * could pass against the wrong element, and it can't distinguish "broken locator" from
 * "hasn't rendered yet" the way an action's immediate miss can.
 */
public abstract class BaseScreen {

    private static final Logger log = LoggerFactory.getLogger(BaseScreen.class);

    protected final AppiumDriver driver;
    protected final WaitUtils waits;
    protected final GestureUtils gestures;
    protected final InterferenceGuard guard;

    private final SelfHealingResolver healing;
    private final LocatorRepository locators;

    protected BaseScreen(AppiumDriver driver) {
        this.driver = driver;
        this.waits = new WaitUtils(driver);
        this.gestures = new GestureUtils(driver);
        this.guard = new InterferenceGuard((AndroidDriver) driver);
        this.healing = new SelfHealingResolver(driver);
        this.locators = new LocatorRepository(screenKey());
        CurrentTest.setScreen(screenKey());
        PageFactory.initElements(new AppiumFieldDecorator(driver, Duration.ofSeconds(10)), this);
    }

    /** File name (without extension) this screen's baselines live in — override if it should differ from the class name. */
    protected String screenKey() {
        return getClass().getSimpleName();
    }

    // ---------------------------------------------------------------- actions (self-healing)

    /** Preferred form: an author-declared, ranked fallback chain. */
    protected void tap(List<LocatorCandidate> candidates, String description) {
        guard.checkAndDismiss();
        WebElement element = healing.resolve(candidates, description, baselineFor(description));
        element.click();
        bootstrapIfAbsent(description, element);
    }

    /** Single-locator form: heals via the heuristic cascade if it breaks. */
    protected void tap(By locator, String description) {
        guard.checkAndDismiss();
        WebElement element = healing.resolve(locator, description, baselineFor(description));
        element.click();
        bootstrapIfAbsent(description, element);
    }

    protected void type(List<LocatorCandidate> candidates, String description, String value) {
        guard.checkAndDismiss();
        WebElement element = healing.resolve(candidates, description, baselineFor(description));
        element.clear();
        element.sendKeys(value);
        bootstrapIfAbsent(description, element);
    }

    protected void type(By locator, String description, String value) {
        guard.checkAndDismiss();
        WebElement element = healing.resolve(locator, description, baselineFor(description));
        element.clear();
        element.sendKeys(value);
        bootstrapIfAbsent(description, element);
    }

    protected boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (org.openqa.selenium.NoSuchElementException | org.openqa.selenium.StaleElementReferenceException e) {
            return false;
        }
    }

    // ------------------------------------------------------------- assertions (never healed)

    protected WebElement expectVisible(By locator) {
        return waits.waitForVisible(locator);
    }

    protected WebElement expectVisible(By locator, Duration timeout) {
        return waits.waitForVisible(locator, timeout);
    }

    protected boolean expectHidden(By locator) {
        return waits.waitForInvisible(locator);
    }

    protected boolean expectHidden(By locator, Duration timeout) {
        return waits.waitForInvisible(locator, timeout);
    }

    // --------------------------------------------------------------------------- internals

    private ElementFingerprint baselineFor(String description) {
        return locators.get(description).orElse(null);
    }

    /** Seeds locators/&lt;screen&gt;.json the first time this description resolves with no stored baseline. */
    private void bootstrapIfAbsent(String description, WebElement element) {
        if (locators.has(description)) {
            return;
        }
        try {
            ElementFingerprint fingerprint = ElementFingerprint.captureFromElement(
                    element, driver.manage().window().getSize());
            locators.put(description, fingerprint);
            log.info("Bootstrapped locator baseline for \"{}\" on screen {}", description, screenKey());
        } catch (Exception e) {
            // Bootstrapping is a convenience, not a correctness requirement -- never fail a test over it.
            log.debug("Could not bootstrap baseline for \"{}\": {}", description, e.getMessage());
        }
    }

    public abstract boolean isDisplayed();
}
