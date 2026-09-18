package com.azm.qa.mobile.screens;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

import io.appium.java_client.AppiumBy;

/**
 * Aptoide's real search screen (cm.aptoide.pt), reached from Home's real Search entry point --
 * crawled live via a chained navigation ({@code crawl --screen SearchScreen --afterSteps
 * OnboardingScreen.tapSkip;HomeScreen.tapSearch}). The committed baseline
 * ({@code src/test/resources/locators/SearchScreen.json}) has 7 real elements: a real search
 * input field ({@code search_src_text}), Collapse, and the same bottom navigation seen on Home.
 */
public final class SearchScreen extends BaseScreen {

    private static final By SEARCH_FIELD = AppiumBy.androidUIAutomator(
            "new UiSelector().resourceId(\"cm.aptoide.pt:id/search_src_text\")");

    public SearchScreen(AppiumDriver driver) {
        super(driver);
    }

    @Override
    public boolean isDisplayed() {
        try {
            return isDisplayed(driver.findElement(SEARCH_FIELD));
        } catch (org.openqa.selenium.NoSuchElementException e) {
            return false;
        }
    }
}
