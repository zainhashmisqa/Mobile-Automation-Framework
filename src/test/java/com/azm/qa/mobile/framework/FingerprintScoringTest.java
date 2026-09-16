package com.azm.qa.mobile.framework;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.azm.qa.mobile.healing.ElementFingerprint;
import com.azm.qa.mobile.healing.PageSourceNode;
import com.azm.qa.mobile.healing.PageSourceParser;

/**
 * Pure unit tests for {@link ElementFingerprint#similarityTo} — no device, no Appium session,
 * runs in milliseconds. This is one of the tests that proves the "no billed API key anywhere"
 * claim about self-healing: the scoring that decides whether a heal is safe enough to accept
 * is ordinary, deterministic Java, exercised here exactly like any other unit.
 */
public class FingerprintScoringTest {

    private PageSourceNode root;

    @BeforeClass
    public void loadFixture() {
        root = PageSourceParser.parse(readFixture());
    }

    @Test
    public void exactContentDescMatchScoresNearOne() {
        PageSourceNode skipButton = findByContentDesc(root, "Skip");
        ElementFingerprint fingerprint = ElementFingerprint.capture(skipButton, 1080, 2280);

        double score = fingerprint.similarityTo(skipButton, 1080, 2280);

        Assert.assertTrue(score > 0.95, "Identical node against its own fingerprint should score near 1.0, got " + score);
    }

    @Test
    public void differentElementScoresLow() {
        PageSourceNode skipButton = findByContentDesc(root, "Skip");
        PageSourceNode continueButton = findByText(root, "Continue");
        ElementFingerprint skipFingerprint = ElementFingerprint.capture(skipButton, 1080, 2280);

        double score = skipFingerprint.similarityTo(continueButton, 1080, 2280);

        Assert.assertTrue(score < 0.5, "A visually and semantically different element should score low, got " + score);
    }

    @Test
    public void resourceIdSurvivesEvenWhenContentDescAndTextAreAbsent() {
        PageSourceNode continueButton = findByText(root, "Continue");
        ElementFingerprint fingerprint = ElementFingerprint.capture(continueButton, 1080, 2280);

        // A node identical except its resource-id has drifted -- everything else matches, so
        // the score should still be meaningfully high even though the id itself doesn't match.
        PageSourceNode driftedNode = new PageSourceNode(
                continueButton.className, continueButton.text, continueButton.contentDesc,
                "com.titanstudio.pro:id/continue_button_v2", "[80,1900][1000,2020]",
                true, continueButton.indexInParent, continueButton.parent);

        double score = fingerprint.similarityTo(driftedNode, 1080, 2280);
        Assert.assertTrue(score > 0.7, "Text + position + class agreement should carry a high score despite resource-id drift, got " + score);
    }

    private PageSourceNode findByContentDesc(PageSourceNode root, String contentDesc) {
        return root.flatten().stream()
                .filter(n -> n.contentDesc.equals(contentDesc))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Fixture is missing a node with content-desc=" + contentDesc));
    }

    private PageSourceNode findByText(PageSourceNode root, String text) {
        return root.flatten().stream()
                .filter(n -> n.text.equals(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Fixture is missing a node with text=" + text));
    }

    private String readFixture() {
        try {
            return Files.readString(Path.of("src", "test", "resources", "fixtures", "sample-page-source.xml"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
