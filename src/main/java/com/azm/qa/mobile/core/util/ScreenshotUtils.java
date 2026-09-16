package com.azm.qa.mobile.core.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ScreenshotUtils {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotUtils.class);
    private static final Path DIR = Path.of("target", "screenshots");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private ScreenshotUtils() {
    }

    /** Returns the saved file path, or null if capture failed — callers decide whether that's fatal. */
    public static Path capture(TakesScreenshot driver, String testName) {
        try {
            Files.createDirectories(DIR);
            byte[] bytes = driver.getScreenshotAs(OutputType.BYTES);
            String fileName = sanitize(testName) + "-" + java.time.LocalDateTime.now().format(STAMP) + ".png";
            Path file = DIR.resolve(fileName);
            Files.write(file, bytes);
            return file;
        } catch (IOException e) {
            log.warn("Could not save screenshot for {}: {}", testName, e.getMessage());
            return null;
        }
    }

    public static byte[] captureBytes(TakesScreenshot driver) {
        return driver.getScreenshotAs(OutputType.BYTES);
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
