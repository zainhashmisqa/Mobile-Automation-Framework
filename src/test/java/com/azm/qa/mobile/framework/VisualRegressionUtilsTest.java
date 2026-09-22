package com.azm.qa.mobile.framework;

import java.awt.image.BufferedImage;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.azm.qa.mobile.capability.visual.VisualRegressionUtils;

/**
 * Pure unit tests for {@link VisualRegressionUtils#pixelDiffRatio} against in-memory images --
 * no device, no driver, no screenshot capture. Proves the diff math itself is correct before
 * it's ever trusted against a real screenshot.
 */
public class VisualRegressionUtilsTest {

    private static final int WHITE = 0xFFFFFF;
    private static final int BLACK = 0x000000;

    @Test
    public void identicalImagesHaveZeroDiffRatio() {
        BufferedImage a = solidImage(10, 10, WHITE);
        BufferedImage b = solidImage(10, 10, WHITE);

        Assert.assertEquals(VisualRegressionUtils.pixelDiffRatio(a, b), 0.0);
    }

    @Test
    public void fullyDifferentImagesHaveDiffRatioOfOne() {
        BufferedImage a = solidImage(10, 10, WHITE);
        BufferedImage b = solidImage(10, 10, BLACK);

        Assert.assertEquals(VisualRegressionUtils.pixelDiffRatio(a, b), 1.0);
    }

    @Test
    public void partiallyDifferentImagesReportTheExactRatio() {
        // 10x10 = 100 pixels; make exactly 25 of them differ (top-left quadrant).
        BufferedImage a = solidImage(10, 10, WHITE);
        BufferedImage b = solidImage(10, 10, WHITE);
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                b.setRGB(x, y, BLACK);
            }
        }

        Assert.assertEquals(VisualRegressionUtils.pixelDiffRatio(a, b), 0.25, 0.0001);
    }

    @Test
    public void differentDimensionsCompareOnlyTheOverlappingRegion() {
        // A 10x10 white image vs a 5x5 black image: only the top-left 5x5 overlap is compared,
        // and every pixel in that overlap differs, so the ratio is still 1.0 -- not an error.
        BufferedImage a = solidImage(10, 10, WHITE);
        BufferedImage b = solidImage(5, 5, BLACK);

        Assert.assertEquals(VisualRegressionUtils.pixelDiffRatio(a, b), 1.0);
    }

    private static BufferedImage solidImage(int width, int height, int rgb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }
}
