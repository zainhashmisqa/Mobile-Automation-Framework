package com.azm.qa.mobile.core.context;

import java.time.Duration;
import java.util.Set;

import org.openqa.selenium.NoSuchWindowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.core.waits.WaitUtils;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;

/**
 * Context switching for the hybrid parts of the app — the manifest shows this Flutter app
 * embeds real Android WebViews (Firebase Auth's reCAPTCHA/GenericIdp activities are WebView-
 * backed under the hood). Native automation cannot reach into a WebView's DOM, so a test that
 * needs to interact with one has to switch context first.
 *
 * <p>Constructed from the {@link AppiumDriver}-typed field everything else in {@code core}
 * uses, and cast to {@link AndroidDriver} internally where context-switching actually lives —
 * the same pattern {@code AppLifecycleUtils}/{@code PermissionUtils} already use, so a future
 * iOS driver only needs its own equivalent class rather than a change here.
 */
public final class WebViewContextUtils {

    private static final Logger log = LoggerFactory.getLogger(WebViewContextUtils.class);
    private static final String NATIVE_CONTEXT = "NATIVE_APP";

    private final AndroidDriver driver;
    private final WaitUtils waits;

    public WebViewContextUtils(AppiumDriver driver) {
        this.driver = (AndroidDriver) driver;
        this.waits = new WaitUtils(driver);
    }

    /** Waits for at least one WEBVIEW_* context to attach, then switches to the first match. */
    public void switchToWebView() {
        waits.waitUntilTrue(
                d -> contexts().stream().anyMatch(c -> c.startsWith("WEBVIEW")),
                Duration.ofSeconds(15),
                "a WEBVIEW_* context to attach");
        String target = contexts().stream()
                .filter(c -> c.startsWith("WEBVIEW"))
                .findFirst()
                .orElseThrow(() -> new NoSuchWindowException("No WEBVIEW_* context attached"));
        driver.context(target);
        log.info("Switched to context: {}", target);
    }

    public void switchToNative() {
        driver.context(NATIVE_CONTEXT);
        log.info("Switched to context: {}", NATIVE_CONTEXT);
    }

    public boolean isWebViewAttached() {
        return contexts().stream().anyMatch(c -> c.startsWith("WEBVIEW"));
    }

    public String currentContext() {
        return driver.getContext();
    }

    @SuppressWarnings("unchecked")
    private Set<String> contexts() {
        return (Set<String>) (Set<?>) driver.getContextHandles();
    }
}
