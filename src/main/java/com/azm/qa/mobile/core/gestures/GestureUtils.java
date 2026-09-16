package com.azm.qa.mobile.core.gestures;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import io.appium.java_client.AppiumDriver;

/**
 * W3C Actions-based gestures. Every gesture here is built from raw pointer input rather than
 * a driver-specific "swipe" helper, so the same code works whether {@code AppiumDriver} is
 * backed by UiAutomator2 today or an iOS driver later.
 */
public final class GestureUtils {

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private final AppiumDriver driver;

    public GestureUtils(AppiumDriver driver) {
        this.driver = driver;
    }

    public void swipe(Direction direction) {
        Dimension size = driver.manage().window().getSize();
        int centerX = size.width / 2;
        int centerY = size.height / 2;
        int margin = (int) (size.height * 0.2);

        Point start;
        Point end;
        switch (direction) {
            case UP -> {
                start = new Point(centerX, size.height - margin);
                end = new Point(centerX, margin);
            }
            case DOWN -> {
                start = new Point(centerX, margin);
                end = new Point(centerX, size.height - margin);
            }
            case LEFT -> {
                start = new Point(size.width - margin, centerY);
                end = new Point(margin, centerY);
            }
            case RIGHT -> {
                start = new Point(margin, centerY);
                end = new Point(size.width - margin, centerY);
            }
            default -> throw new IllegalArgumentException("Unhandled direction: " + direction);
        }
        performSwipe(start, end, Duration.ofMillis(400));
    }

    /**
     * Scrolls in the given direction, checking after each attempt whether the target element
     * became present. Bounded by {@code maxSwipes} rather than looping forever against a list
     * that never contains the element.
     */
    public boolean scrollToElement(java.util.function.Supplier<WebElement> elementSupplier,
                                    Direction direction, int maxSwipes) {
        for (int attempt = 0; attempt < maxSwipes; attempt++) {
            try {
                WebElement element = elementSupplier.get();
                if (element != null && element.isDisplayed()) {
                    return true;
                }
            } catch (Exception ignored) {
                // Not present yet -- keep scrolling.
            }
            swipe(direction);
        }
        try {
            WebElement element = elementSupplier.get();
            return element != null && element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void longPress(WebElement element, Duration duration) {
        Point center = elementCenter(element);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence sequence = new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), center.x, center.y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, duration))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(sequence));
    }

    /** Two-finger pinch. Positive delta zooms in, negative zooms out. */
    public void pinch(int centerXPercent, int centerYPercent, int deltaPx) {
        Dimension size = driver.manage().window().getSize();
        int cx = size.width * centerXPercent / 100;
        int cy = size.height * centerYPercent / 100;

        PointerInput f1 = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
        PointerInput f2 = new PointerInput(PointerInput.Kind.TOUCH, "finger2");

        Sequence seq1 = new Sequence(f1, 1)
                .addAction(f1.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), cx - deltaPx, cy))
                .addAction(f1.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(f1.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), cx, cy))
                .addAction(f1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        Sequence seq2 = new Sequence(f2, 1)
                .addAction(f2.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), cx + deltaPx, cy))
                .addAction(f2.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(f2.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), cx, cy))
                .addAction(f2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(List.of(seq1, seq2));
    }

    private void performSwipe(Point start, Point end, Duration duration) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence sequence = new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), start.x, start.y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(duration, PointerInput.Origin.viewport(), end.x, end.y))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(sequence));
    }

    private Point elementCenter(WebElement element) {
        Point location = element.getLocation();
        Dimension size = element.getSize();
        return new Point(location.x + size.width / 2, location.y + size.height / 2);
    }
}
