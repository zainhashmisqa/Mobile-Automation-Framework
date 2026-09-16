package com.azm.qa.mobile.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.core.config.ConfigManager;

import io.appium.java_client.AppiumDriver;

/**
 * Writes {@code environment.properties} and copies {@code categories.json} into the Allure
 * results directory, so the generated report carries the real run context (which device, which
 * app build, which Appium session) rather than an anonymous list of test names.
 *
 * <p>Values are read off the <em>live session's own capabilities</em>, not from config — config
 * says what was requested, capabilities say what Appium actually negotiated, and when those
 * disagree the report should show the truth. Written once per JVM (see {@link #WRITTEN}); a
 * parallel run's threads all describe the same host/app, so re-writing per test would just be
 * repeated I/O for an identical file.
 */
public final class AllureEnvironment {

    private static final Logger log = LoggerFactory.getLogger(AllureEnvironment.class);
    private static final AtomicBoolean WRITTEN = new AtomicBoolean(false);

    private AllureEnvironment() {
    }

    /** Safe to call on every test; only the first call per JVM does any work. */
    public static void writeOnce(AppiumDriver driver) {
        if (!WRITTEN.compareAndSet(false, true)) {
            return;
        }
        try {
            Path resultsDir = Path.of(ConfigManager.get(
                    "allure.results.directory", "allure-results"));
            Files.createDirectories(resultsDir);

            writeEnvironmentProperties(resultsDir, driver);
            copyCategories(resultsDir);
        } catch (Exception e) {
            // Reporting metadata is never worth failing a test over.
            log.warn("Could not write Allure environment metadata: {}", e.getMessage());
        }
    }

    private static void writeEnvironmentProperties(Path resultsDir, AppiumDriver driver) throws IOException {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("App.package", String.valueOf(ConfigManager.get("app.package", "unknown")));
        env.put("App.activity", String.valueOf(ConfigManager.get("app.activity", "(auto-detected)")));
        env.put("App.path", String.valueOf(ConfigManager.get("app.path", "(attached to installed build)")));
        env.put("Device.profile", String.valueOf(ConfigManager.get("device.profile", "default")));
        env.put("Appium.serverUrl", String.valueOf(ConfigManager.get("appium.server.url", "unknown")));
        env.put("SelfHealing.enabled", String.valueOf(ConfigManager.getBoolean("selfhealing.enabled", true)));
        env.put("SelfHealing.minConfidence", String.valueOf(ConfigManager.get("selfhealing.minConfidence", "0.62")));
        env.put("HealPolicy.maxHealRate", String.valueOf(ConfigManager.get("healpolicy.maxHealRate", "0.15")));

        // What the session actually negotiated, which can differ from what was requested.
        capability(driver, "platformName").ifPresent(v -> env.put("Device.platformName", v));
        capability(driver, "platformVersion").ifPresent(v -> env.put("Device.platformVersion", v));
        capability(driver, "deviceName").ifPresent(v -> env.put("Device.name", v));
        capability(driver, "deviceUDID").ifPresent(v -> env.put("Device.udid", v));
        capability(driver, "deviceApiLevel").ifPresent(v -> env.put("Device.apiLevel", v));
        capability(driver, "automationName").ifPresent(v -> env.put("Appium.automationName", v));

        StringBuilder sb = new StringBuilder();
        env.forEach((k, v) -> sb.append(k).append('=').append(v).append(System.lineSeparator()));
        Files.writeString(resultsDir.resolve("environment.properties"), sb.toString(), StandardCharsets.UTF_8);
        log.info("Wrote Allure environment.properties ({} entries)", env.size());
    }

    private static java.util.Optional<String> capability(AppiumDriver driver, String name) {
        try {
            Object value = driver.getCapabilities().getCapability(name);
            return value == null ? java.util.Optional.empty() : java.util.Optional.of(String.valueOf(value));
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    /** Allure only picks categories up from the results directory, not the classpath. */
    private static void copyCategories(Path resultsDir) {
        try (InputStream in = AllureEnvironment.class.getClassLoader()
                .getResourceAsStream("allure/categories.json")) {
            if (in == null) {
                return;
            }
            Files.copy(in, resultsDir.resolve("categories.json"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not copy categories.json into " + resultsDir, e);
        }
    }
}
