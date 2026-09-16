package com.azm.qa.mobile.healing;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parses the UiAutomator2 page-source XML (what {@code driver.getPageSource()} returns) into
 * a {@link PageSourceNode} tree. Pure — no Appium/driver dependency — so healing logic that
 * consumes the tree can be unit-tested against a canned XML fixture with no emulator running.
 */
public final class PageSourceParser {

    private PageSourceParser() {
    }

    public static PageSourceNode parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            return toNode(root, 0, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse page source XML", e);
        }
    }

    private static PageSourceNode toNode(Element element, int indexInParent, PageSourceNode parent) {
        PageSourceNode node = new PageSourceNode(
                element.getAttribute("class"),
                element.getAttribute("text"),
                element.getAttribute("content-desc"),
                element.getAttribute("resource-id"),
                element.getAttribute("bounds"),
                "true".equals(element.getAttribute("clickable")),
                indexInParent,
                parent);

        NodeList childNodes = element.getChildNodes();
        int childIndex = 0;
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                node.children.add(toNode((Element) child, childIndex, node));
                childIndex++;
            }
        }
        return node;
    }
}
