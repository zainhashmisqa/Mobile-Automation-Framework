package com.azm.qa.mobile.healing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The heuristic half of the hybrid: nine mobile-specific search strategies over a parsed
 * page-source tree, each strategy narrowing to a candidate set, every candidate scored
 * against the stored {@link ElementFingerprint}, and the single best-scoring match across all
 * nine returned — provided it clears {@code selfhealing.minConfidence}.
 *
 * <p>Deliberately pure: takes an XML string and a fingerprint, returns a match. No driver, no
 * network, no model call. That is what lets {@code StrategyCascadeTest} and
 * {@code FingerprintScoringTest} run as ordinary JUnit-speed unit tests with no emulator
 * attached, and what keeps the "no billed API key anywhere" guarantee true at the one place
 * most self-healing vendors quietly break it.
 *
 * <p>Ranked roughly by how much a Flutter build actually preserves: accessibility labels
 * (Semantics) survive almost every kind of change; resource-ids are frequently stripped
 * entirely by the Flutter renderer; raw structural xpath is the least stable and comes last.
 */
public final class StrategyCascade {

    private final double minConfidence;

    public StrategyCascade(double minConfidence) {
        this.minConfidence = minConfidence;
    }

    public Optional<CascadeMatch> resolve(String pageSourceXml, String description, ElementFingerprint baseline,
                                           int screenWidth, int screenHeight) {
        PageSourceNode root = PageSourceParser.parse(pageSourceXml);
        List<PageSourceNode> all = root.flatten();
        Set<String> descriptionTokens = tokenize(description);

        CascadeMatch best = null;

        for (Strategy strategy : Strategy.values()) {
            List<PageSourceNode> candidates = strategy.select(all, baseline, descriptionTokens);
            for (PageSourceNode candidate : candidates) {
                double score = baseline.similarityTo(candidate, screenWidth, screenHeight);
                if (best == null || score > best.score()) {
                    best = new CascadeMatch(strategy.label, candidate, score);
                }
            }
        }

        if (best == null || best.score() < minConfidence) {
            return Optional.empty();
        }
        return Optional.of(best);
    }

    private static Set<String> tokenize(String description) {
        if (description == null || description.isBlank()) {
            return Set.of();
        }
        return Set.of(description.toLowerCase(Locale.ROOT).split("\\W+"));
    }

    public record CascadeMatch(String strategy, PageSourceNode node, double score) {
    }

    /**
     * Each strategy is a narrowing filter over the full node list. Order here is the
     * documented priority — accessibility-id first, raw structural xpath last — and is what a
     * heal report's "strategy" column names.
     */
    private enum Strategy {
        ACCESSIBILITY_ID_EXACT((all, fp, tokens) -> filter(all, n ->
                !fp.contentDesc().isEmpty() && n.contentDesc.equals(fp.contentDesc()))),

        CONTENT_DESC_CONTAINS((all, fp, tokens) -> filter(all, n ->
                !fp.contentDesc().isEmpty() && !n.contentDesc.isEmpty()
                        && containsIgnoreCase(n.contentDesc, fp.contentDesc()))),

        TEXT_EXACT((all, fp, tokens) -> filter(all, n ->
                !fp.text().isEmpty() && n.text.equals(fp.text()))),

        TEXT_CONTAINS((all, fp, tokens) -> filter(all, n ->
                !fp.text().isEmpty() && !n.text.isEmpty() && containsIgnoreCase(n.text, fp.text()))),

        RESOURCE_ID_EXACT((all, fp, tokens) -> filter(all, n ->
                !fp.resourceId().isEmpty() && n.resourceId.equals(fp.resourceId()))),

        CLASS_AND_INDEX((all, fp, tokens) -> filter(all, n ->
                !fp.className().isEmpty() && n.className.equals(fp.className())
                        && n.indexInParent == fp.indexInParent())),

        TEXT_ANCHORED_XPATH((all, fp, tokens) -> filter(all, n ->
                !fp.className().isEmpty() && n.className.equals(fp.className())
                        && !n.text.isEmpty() && !fp.text().isEmpty()
                        && containsIgnoreCase(n.text, firstWord(fp.text())))),

        SIBLING_ANCHORED_XPATH((all, fp, tokens) -> filter(all, n ->
                n.parent != null && !fp.className().isEmpty() && n.className.equals(fp.className())
                        && n.parent.children.size() > 1
                        && Math.abs(n.indexInParent - fp.indexInParent()) <= 1)),

        DESCRIPTION_TOKEN_HEURISTIC((all, fp, tokens) -> tokens.isEmpty() ? List.of() : filter(all, n ->
                tokens.stream().anyMatch(t -> t.length() > 2
                        && (containsIgnoreCase(n.contentDesc, t) || containsIgnoreCase(n.text, t)))));

        private final Selector selector;

        Strategy(Selector selector) {
            this.selector = selector;
        }

        final String label = name().toLowerCase(Locale.ROOT).replace('_', '-');

        List<PageSourceNode> select(List<PageSourceNode> all, ElementFingerprint fp, Set<String> tokens) {
            return selector.select(all, fp, tokens);
        }

        @FunctionalInterface
        private interface Selector {
            List<PageSourceNode> select(List<PageSourceNode> all, ElementFingerprint fp, Set<String> tokens);
        }
    }

    private static List<PageSourceNode> filter(List<PageSourceNode> all, java.util.function.Predicate<PageSourceNode> p) {
        List<PageSourceNode> out = new ArrayList<>();
        for (PageSourceNode n : all) {
            if (p.test(n)) {
                out.add(n);
            }
        }
        return out;
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static String firstWord(String s) {
        int idx = s.indexOf(' ');
        return idx < 0 ? s : s.substring(0, idx);
    }
}
