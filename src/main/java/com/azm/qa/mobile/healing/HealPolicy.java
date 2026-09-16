package com.azm.qa.mobile.healing;

import com.azm.qa.mobile.analysis.RunAnalyzer;
import com.azm.qa.mobile.core.config.ConfigManager;

/**
 * The policy layer on top of the heal ledger — the answer to "doesn't self-healing hide
 * regressions?" A heal recovering an occasional stale locator is normal maintenance; a run
 * where a large fraction of tests needed healing is a signal that something changed for real
 * (a redesign, a broken build) and the run should say so loudly rather than reporting a wall
 * of green.
 *
 * <p>Deliberately separate from {@link RunAnalyzer}: the analyzer's job is to describe what
 * happened, this class's job is to judge it against a threshold and hand back a pass/fail
 * verdict a CI step can act on. Two tiers, both configurable:
 *
 * <ul>
 *   <li>{@code healpolicy.maxHealRate} (default 0.15) — above this, {@link #evaluate} fails.</li>
 *   <li>{@code healpolicy.warnHealRate} (default 0.05) — above this but below the fail
 *       threshold, the run still passes but the verdict is flagged for attention.</li>
 * </ul>
 */
public final class HealPolicy {

    private final double warnHealRate;
    private final double maxHealRate;

    public HealPolicy(double warnHealRate, double maxHealRate) {
        this.warnHealRate = warnHealRate;
        this.maxHealRate = maxHealRate;
    }

    public static HealPolicy fromConfig() {
        return new HealPolicy(
                Double.parseDouble(ConfigManager.get("healpolicy.warnHealRate", "0.05")),
                Double.parseDouble(ConfigManager.get("healpolicy.maxHealRate", "0.15")));
    }

    public Verdict evaluate(RunAnalyzer.Report report) {
        if (report.totalTests() == 0) {
            return new Verdict(Level.PASS, 0.0, "No tests in this run -- nothing to gate.");
        }

        double healRate = (double) report.healedTests() / report.totalTests();

        if (healRate > maxHealRate) {
            return new Verdict(Level.FAIL, healRate, String.format(java.util.Locale.ROOT,
                    "Heal rate %.1f%% exceeds the %.1f%% gate (%d of %d tests healed). "
                            + "This many locators recovering in one run reads as a real UI change "
                            + "that needs review, not incidental drift -- review the heal ledger "
                            + "before trusting this run's green.",
                    healRate * 100, maxHealRate * 100, report.healedTests(), report.totalTests()));
        }
        if (healRate > warnHealRate) {
            return new Verdict(Level.WARN, healRate, String.format(java.util.Locale.ROOT,
                    "Heal rate %.1f%% is above the %.1f%% attention threshold (%d of %d tests "
                            + "healed) but under the %.1f%% gate -- worth a look, not yet a failure.",
                    healRate * 100, warnHealRate * 100, report.healedTests(), report.totalTests(),
                    maxHealRate * 100));
        }
        return new Verdict(Level.PASS, healRate, String.format(java.util.Locale.ROOT,
                "Heal rate %.1f%% is within normal bounds.", healRate * 100));
    }

    public enum Level { PASS, WARN, FAIL }

    public record Verdict(Level level, double healRate, String message) {
        public boolean shouldFailBuild() {
            return level == Level.FAIL;
        }
    }
}
