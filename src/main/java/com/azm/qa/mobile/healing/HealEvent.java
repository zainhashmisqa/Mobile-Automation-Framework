package com.azm.qa.mobile.healing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One line of the heal ledger ({@code self-healing-data/heal-events.jsonl}) — the audit trail
 * that makes a heal a reviewable signal instead of a silently-passing test. Every successful
 * heal writes exactly one of these; the run is also marked {@code HEALED} (see
 * {@link HealReporter}) so it never reads as a plain green pass.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HealEvent(
        long timestamp,
        String testClass,
        String testMethod,
        String description,
        String originalStrategy,
        String healedStrategy,
        double confidence,
        String screen) {
}
