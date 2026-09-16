package com.azm.qa.mobile.selfscore;

import java.nio.file.Path;

/**
 * One axis of the mobile-mapped 10-criteria rubric (see the plan this was built from: a mobile
 * adaptation of the web-framework assessment deck's 10 axes, kept at 10 slots so scores stay
 * comparable). Every implementation is a pure, read-only file-system scan — no criterion
 * compiles or runs the target repo, so {@code selfscore} can evaluate a repo it did not build.
 */
public interface Criterion {

    /** Short name shown in the scorecard, e.g. "Self-healing authenticity". */
    String name();

    /** The web-deck axis this maps from, e.g. "AI/Healing" — shown for traceability. */
    String mappedFrom();

    CriterionScore evaluate(Path repoRoot);
}
