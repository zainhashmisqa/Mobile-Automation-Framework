package com.azm.qa.mobile.spec;

import java.util.ArrayList;
import java.util.List;

/**
 * The parsed shape of one {@code specs/*.md} feature file — the Java twin of the web
 * framework's parsed {@code TestCase[]}. {@link MarkdownSpecParser} produces this;
 * {@link TestNgCodeGenerator} consumes it. Nothing in between calls a model: the same
 * Markdown always parses to the same tree.
 */
public final class SpecModel {

    public String featureName;
    public String key;
    public String screen;
    public String priority = "MEDIUM";
    public String description = "";
    public final List<String> acceptanceCriteria = new ArrayList<>();
    public final List<Scenario> scenarios = new ArrayList<>();

    public static final class Scenario {
        public String type;          // POSITIVE | NEGATIVE | EDGE
        public String title;
        public String priority;
        public final List<String> tags = new ArrayList<>();
        public final List<String> preconditions = new ArrayList<>();
        public final List<Step> steps = new ArrayList<>();
    }

    /** One "action => expected" (or "action -> expected") line from a scenario. */
    public static final class Step {
        public final String action;
        public final String expected;

        public Step(String action, String expected) {
            this.action = action;
            this.expected = expected;
        }
    }
}
