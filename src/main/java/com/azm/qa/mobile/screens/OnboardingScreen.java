package com.azm.qa.mobile.screens;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

import io.appium.java_client.AppiumBy;

/**
 * Aptoide's real first-run onboarding wizard (cm.aptoide.pt) -- a 3-page ViewPager carousel
 * with a SKIP button. Crawled live via {@code crawl --screen OnboardingScreen}; the resulting
 * baseline is committed at {@code src/test/resources/locators/OnboardingScreen.json}. Only the
 * SKIP button is genuinely clickable on this screen -- confirmed directly from the crawled page
 * source, where the title/description text and the page radio-group indicator are not.
 */
public final class OnboardingScreen extends BaseScreen {

    private static final By TITLE = AppiumBy.androidUIAutomator(
            "new UiSelector().resourceId(\"cm.aptoide.pt:id/title\")");

    public OnboardingScreen(AppiumDriver driver) {
        super(driver);
    }

    @Override
    public boolean isDisplayed() {
        try {
            return isDisplayed(driver.findElement(TITLE));
        } catch (org.openqa.selenium.NoSuchElementException e) {
            return false;
        }
    }
}
