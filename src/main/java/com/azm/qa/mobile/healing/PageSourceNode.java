package com.azm.qa.mobile.healing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One node from a parsed UiAutomator2 page-source XML tree. Deliberately a plain data holder
 * with no driver dependency — this is what makes {@link StrategyCascade} and
 * {@link ElementFingerprint} testable with a canned XML string and no emulator.
 */
public final class PageSourceNode {

    private static final Pattern BOUNDS = Pattern.compile("\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]");

    public final String className;
    public final String text;
    public final String contentDesc;
    public final String resourceId;
    public final boolean clickable;
    public final int[] bounds; // x1, y1, x2, y2
    public final int indexInParent;
    public final PageSourceNode parent;
    public final List<PageSourceNode> children = new ArrayList<>();
    /** Path from root as sibling indices, e.g. "0/2/1" — a stable-ish structural fingerprint. */
    public final String structuralPath;

    public PageSourceNode(String className, String text, String contentDesc, String resourceId,
                           String boundsAttr, boolean clickable, int indexInParent, PageSourceNode parent) {
        this.className = className == null ? "" : className;
        this.text = text == null ? "" : text;
        this.contentDesc = contentDesc == null ? "" : contentDesc;
        this.resourceId = resourceId == null ? "" : resourceId;
        this.clickable = clickable;
        this.bounds = parseBounds(boundsAttr);
        this.indexInParent = indexInParent;
        this.parent = parent;
        this.structuralPath = parent == null
                ? String.valueOf(indexInParent)
                : parent.structuralPath + "/" + indexInParent;
    }

    private static int[] parseBounds(String attr) {
        if (attr == null) {
            return new int[]{0, 0, 0, 0};
        }
        Matcher m = BOUNDS.matcher(attr);
        if (!m.matches()) {
            return new int[]{0, 0, 0, 0};
        }
        return new int[]{
                Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4))
        };
    }

    public int centerX() {
        return (bounds[0] + bounds[2]) / 2;
    }

    public int centerY() {
        return (bounds[1] + bounds[3]) / 2;
    }

    /** Flattens this node and every descendant into one list, depth-first. */
    public List<PageSourceNode> flatten() {
        List<PageSourceNode> all = new ArrayList<>();
        collect(this, all);
        return all;
    }

    private static void collect(PageSourceNode node, List<PageSourceNode> out) {
        out.add(node);
        for (PageSourceNode child : node.children) {
            collect(child, out);
        }
    }
}
