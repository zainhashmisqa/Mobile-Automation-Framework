package com.azm.qa.mobile.screens;

import io.appium.java_client.AppiumDriver;

/**
 * Used by generated tests whose spec has no {@code screen:} metadata — the mobile twin of a
 * generated web test that has no {@code page:} Page Object and drives the app through raw
 * steps instead. Still goes through {@link BaseScreen}, so those steps still get self-healing,
 * the interference guard, and locator bootstrapping; it just isn't a named, purpose-built
 * screen object.
 *
 * <p>{@link #isDisplayed()} has no specific screen to check, so it reports whether the driver
 * has any content at all — enough to distinguish "app crashed / blank" from "something is
 * rendered", which is the only thing generic code could honestly assert here.
 */
public final class GenericScreen extends BaseScreen {

    public GenericScreen(AppiumDriver driver) {
        super(driver);
    }

    @Override
    protected String screenKey() {
        return "generic";
    }

    @Override
    public boolean isDisplayed() {
        try {
            String source = driver.getPageSource();
            return source != null && !source.isBlank();
        } catch (Exception e) {
            return false;
        }
    }
}
