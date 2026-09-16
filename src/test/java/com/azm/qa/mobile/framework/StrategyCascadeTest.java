package com.azm.qa.mobile.framework;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.azm.qa.mobile.healing.ElementFingerprint;
import com.azm.qa.mobile.healing.StrategyCascade;

/**
 * Pure unit tests for {@link StrategyCascade} against a canned page-source fixture — no
 * device, no driver, no model call. This is the class that decides what a broken locator
 * heals onto, so proving its behaviour deterministically here (rather than only against a
 * live emulator) is what lets it be trusted at CI speed.
 */
public class StrategyCascadeTest {

    private static final int SCREEN_WIDTH = 1080;
    private static final int SCREEN_HEIGHT = 2280;

    @Test
    public void recoversAKnownElementFromItsFingerprintAloneAboveTheConfidenceThreshold() {
        String pageSource = readFixture();
        // A fingerprint for the real "Skip" button, as if captured on an earlier, successful run.
        ElementFingerprint skipBaseline = new ElementFingerprint("Skip", "", "", "android.view.View", 1, 84, 6);

        StrategyCascade cascade = new StrategyCascade(0.62);
        Optional<StrategyCascade.CascadeMatch> match = cascade.resolve(
                pageSource, "skip button", skipBaseline, SCREEN_WIDTH, SCREEN_HEIGHT);

        Assert.assertTrue(match.isPresent(), "Cascade should recover the Skip button from its fingerprint");
        Assert.assertEquals(match.get().node().contentDesc, "Skip");
        Assert.assertTrue(match.get().score() >= 0.62);
    }

    @Test
    public void refusesAMatchBelowConfidenceRatherThanGuessing() {
        String pageSource = readFixture();
        // A fingerprint that resembles nothing in the fixture at all.
        ElementFingerprint nonsense = new ElementFingerprint(
                "totally-unrelated-label-xyz", "unrelated-text-xyz", "com.other.app:id/nothing",
                "android.widget.SomeOtherWidget", 9, 50, 50);

        StrategyCascade cascade = new StrategyCascade(0.62);
        Optional<StrategyCascade.CascadeMatch> match = cascade.resolve(
                pageSource, "something that doesn't exist", nonsense, SCREEN_WIDTH, SCREEN_HEIGHT);

        Assert.assertTrue(match.isEmpty(),
                "Cascade should refuse to guess when nothing clears the confidence threshold");
    }

    @Test
    public void descriptionTokenHeuristicFindsAnElementWithNoFingerprintSignalAtAll() {
        String pageSource = readFixture();
        // An empty fingerprint (nothing captured yet) but a description that matches real content.
        ElementFingerprint empty = new ElementFingerprint("", "", "", "", 0, 0, 0);

        StrategyCascade cascade = new StrategyCascade(0.0); // token heuristic alone won't score high; prove it still finds something
        Optional<StrategyCascade.CascadeMatch> match = cascade.resolve(
                pageSource, "continue", empty, SCREEN_WIDTH, SCREEN_HEIGHT);

        Assert.assertTrue(match.isPresent(), "Description-token heuristic should still surface the Continue button");
    }

    private String readFixture() {
        try {
            return Files.readString(Path.of("src", "test", "resources", "fixtures", "sample-page-source.xml"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
