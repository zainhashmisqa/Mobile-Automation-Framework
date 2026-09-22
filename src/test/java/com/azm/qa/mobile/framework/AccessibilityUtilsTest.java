package com.azm.qa.mobile.framework;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.azm.qa.mobile.capability.accessibility.AccessibilityUtils;
import com.azm.qa.mobile.healing.PageSourceNode;
import com.azm.qa.mobile.healing.PageSourceParser;

/**
 * Pure unit tests for {@link AccessibilityUtils#findClickableNodesMissingAccessibleName} against
 * canned page-source fixtures -- no device, no driver. Mirrors the same split
 * {@code StrategyCascade}/{@code PageSourceParser} already use: the scanning logic is a pure
 * function over a parsed tree, so it's trusted at CI speed rather than only exercised live.
 */
public class AccessibilityUtilsTest {

    @Test
    public void flagsAClickableNodeWithNoContentDescAndNoText() {
        String xml = "<hierarchy>"
                + "<android.widget.FrameLayout clickable=\"false\">"
                + "  <android.widget.Button clickable=\"true\" content-desc=\"\" text=\"\" bounds=\"[0,0][100,100]\"/>"
                + "  <android.widget.Button clickable=\"true\" content-desc=\"Search\" text=\"\" bounds=\"[0,100][100,200]\"/>"
                + "  <android.widget.TextView clickable=\"true\" content-desc=\"\" text=\"Sign in\" bounds=\"[0,200][100,300]\"/>"
                + "</android.widget.FrameLayout>"
                + "</hierarchy>";

        PageSourceNode root = PageSourceParser.parse(xml);
        List<PageSourceNode> offenders = AccessibilityUtils.findClickableNodesMissingAccessibleName(root);

        Assert.assertEquals(offenders.size(), 1, "only the button with no content-desc AND no text should be flagged");
        Assert.assertEquals(offenders.get(0).bounds[1], 0, "the flagged node should be the first button (y1=0)");
    }

    @Test
    public void ignoresNonClickableNodesRegardlessOfMissingAccessibleName() {
        String xml = "<hierarchy>"
                + "<android.widget.FrameLayout clickable=\"false\">"
                + "  <android.widget.ImageView clickable=\"false\" content-desc=\"\" text=\"\" bounds=\"[0,0][50,50]\"/>"
                + "</android.widget.FrameLayout>"
                + "</hierarchy>";

        PageSourceNode root = PageSourceParser.parse(xml);
        List<PageSourceNode> offenders = AccessibilityUtils.findClickableNodesMissingAccessibleName(root);

        Assert.assertTrue(offenders.isEmpty(), "a non-clickable node should never be flagged, labeled or not");
    }

    @Test
    public void findsOffendersAtAnyDepth() {
        String xml = "<hierarchy>"
                + "<android.widget.FrameLayout clickable=\"false\">"
                + "  <android.widget.LinearLayout clickable=\"false\">"
                + "    <android.widget.LinearLayout clickable=\"false\">"
                + "      <android.widget.Button clickable=\"true\" content-desc=\"\" text=\"\" bounds=\"[0,0][10,10]\"/>"
                + "    </android.widget.LinearLayout>"
                + "  </android.widget.LinearLayout>"
                + "</android.widget.FrameLayout>"
                + "</hierarchy>";

        PageSourceNode root = PageSourceParser.parse(xml);
        List<PageSourceNode> offenders = AccessibilityUtils.findClickableNodesMissingAccessibleName(root);

        Assert.assertEquals(offenders.size(), 1, "an offender three levels deep should still be found");
    }
}
