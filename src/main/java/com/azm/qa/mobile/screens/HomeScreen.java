package com.azm.qa.mobile.screens;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

import io.appium.java_client.AppiumBy;

/**
 * Aptoide's real home/catalog screen (cm.aptoide.pt), reached after skipping onboarding --
 * crawled live via {@code crawl --screen HomeScreen --afterScreen OnboardingScreen
 * --afterAction tapSkip}. The committed baseline
 * ({@code src/test/resources/locators/HomeScreen.json}) has 11 real elements: category filter
 * chips (APPS/GAMES), app tiles, and a real bottom navigation bar (Editorial/Search/Stores/
 * Apps) -- this class anchors on the Search entry point since it's the most stable,
 * content-desc'd real element crawled.
 */
public final class HomeScreen extends BaseScreen {

    private static final By SEARCH_ACTION = AppiumBy.androidUIAutomator(
            "new UiSelector().resourceId(\"cm.aptoide.pt:id/action_search\")");

    public HomeScreen(AppiumDriver driver) {
        super(driver);
    }

    @Override
    public boolean isDisplayed() {
        try {
            return isDisplayed(driver.findElement(SEARCH_ACTION));
        } catch (org.openqa.selenium.NoSuchElementException e) {
            return false;
        }
    }
}
