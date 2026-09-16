package com.azm.qa.mobile.selfscore;

import java.util.List;

/**
 * One criterion's result: a 0-10 score, the evidence that produced it (file paths, counts,
 * grep hits — never a bare adjective), and a one-line verdict a human can act on.
 */
public record CriterionScore(double score, List<String> evidence, String verdict) {

    public CriterionScore {
        if (score < 0 || score > 10) {
            throw new IllegalArgumentException("score must be in [0, 10], got " + score);
        }
        evidence = List.copyOf(evidence);
    }

    public static CriterionScore of(double score, String verdict, String... evidence) {
        return new CriterionScore(score, List.of(evidence), verdict);
    }
}
