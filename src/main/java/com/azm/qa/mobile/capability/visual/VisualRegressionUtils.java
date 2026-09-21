package com.azm.qa.mobile.capability.visual;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.core.util.ScreenshotUtils;

/**
 * S-80: baseline screenshot-diff. A deliberately simple pixel-difference comparison -- no new
 * dependency, no perceptual-hash library -- good enough to catch a real layout break without
 * pretending this is a production visual-regression pipeline. Baselines live under
 * {@code target/visual-baselines/}, one PNG per key; whether to commit a baseline on purpose is
 * a follow-up decision for whoever reviews the first captured set, not something this class
 * does on its own.
 */
public final class VisualRegressionUtils {

    private static final Logger log = LoggerFactory.getLogger(VisualRegressionUtils.class);
    private static final Path BASELINE_DIR = Path.of("target", "visual-baselines");

    private VisualRegressionUtils() {
    }

    public static Path baselinePath(String key) {
        return BASELINE_DIR.resolve(key + ".png");
    }

    /** Saves the current screenshot as the baseline for {@code key} if none exists yet. Returns true if it just captured one. */
    public static boolean captureBaselineIfAbsent(TakesScreenshot driver, String key) {
        Path path = baselinePath(key);
        if (Files.exists(path)) {
            return false;
        }
        try {
            Files.createDirectories(BASELINE_DIR);
            Files.write(path, ScreenshotUtils.captureBytes(driver));
            log.info("Captured visual baseline for '{}'", key);
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write visual baseline for '" + key + "'", e);
        }
    }

    /** Fraction of pixels (0.0-1.0) that differ from the stored baseline for {@code key}. Baseline must already exist. */
    public static double diffRatio(TakesScreenshot driver, String key) {
        Path path = baselinePath(key);
        if (!Files.exists(path)) {
            throw new IllegalStateException(
                    "No visual baseline captured yet for '" + key + "' -- call captureBaselineIfAbsent first");
        }
        try {
            BufferedImage baseline = ImageIO.read(path.toFile());
            BufferedImage current = ImageIO.read(new ByteArrayInputStream(ScreenshotUtils.captureBytes(driver)));
            return pixelDiffRatio(baseline, current);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not compare screenshot against baseline for '" + key + "'", e);
        }
    }

    private static double pixelDiffRatio(BufferedImage a, BufferedImage b) {
        int width = Math.min(a.getWidth(), b.getWidth());
        int height = Math.min(a.getHeight(), b.getHeight());
        if (width == 0 || height == 0) {
            return 1.0;
        }
        long diffPixels = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    diffPixels++;
                }
            }
        }
        return diffPixels / (double) (width * height);
    }
}
