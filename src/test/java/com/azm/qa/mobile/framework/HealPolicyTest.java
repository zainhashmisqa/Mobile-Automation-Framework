package com.azm.qa.mobile.framework;

import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.azm.qa.mobile.analysis.RunAnalyzer;
import com.azm.qa.mobile.healing.HealPolicy;

/**
 * Pure unit tests for {@link HealPolicy} — no device, no Allure results directory. Builds
 * {@link RunAnalyzer.Report} directly rather than through {@code RunAnalyzer.analyze(...)} so
 * the policy's own threshold logic is exercised in isolation from result-file parsing.
 */
public class HealPolicyTest {

    @Test
    public void healRateWithinBothThresholdsPasses() {
        HealPolicy policy = new HealPolicy(0.05, 0.15);
        RunAnalyzer.Report report = report(100, 2); // 2% healed

        HealPolicy.Verdict verdict = policy.evaluate(report);

        Assert.assertEquals(verdict.level(), HealPolicy.Level.PASS);
        Assert.assertFalse(verdict.shouldFailBuild());
    }

    @Test
    public void healRateAboveWarnButBelowMaxWarnsWithoutFailing() {
        HealPolicy policy = new HealPolicy(0.05, 0.15);
        RunAnalyzer.Report report = report(100, 10); // 10% healed

        HealPolicy.Verdict verdict = policy.evaluate(report);

        Assert.assertEquals(verdict.level(), HealPolicy.Level.WARN);
        Assert.assertFalse(verdict.shouldFailBuild());
    }

    @Test
    public void healRateAboveMaxFailsTheBuild() {
        HealPolicy policy = new HealPolicy(0.05, 0.15);
        RunAnalyzer.Report report = report(100, 20); // 20% healed

        HealPolicy.Verdict verdict = policy.evaluate(report);

        Assert.assertEquals(verdict.level(), HealPolicy.Level.FAIL);
        Assert.assertTrue(verdict.shouldFailBuild());
        Assert.assertTrue(verdict.message().contains("20.0%"));
    }

    @Test
    public void zeroTestsNeverFailsTheGate() {
        HealPolicy policy = new HealPolicy(0.05, 0.15);
        RunAnalyzer.Report report = report(0, 0);

        HealPolicy.Verdict verdict = policy.evaluate(report);

        Assert.assertEquals(verdict.level(), HealPolicy.Level.PASS);
    }

    @Test
    public void exactlyAtTheMaxThresholdDoesNotFail() {
        // evaluate() uses a strict ">" comparison -- landing exactly on the configured
        // threshold should not trip the gate, only exceeding it should.
        HealPolicy policy = new HealPolicy(0.05, 0.15);
        RunAnalyzer.Report report = report(100, 15); // exactly 15%

        HealPolicy.Verdict verdict = policy.evaluate(report);

        Assert.assertNotEquals(verdict.level(), HealPolicy.Level.FAIL);
    }

    private RunAnalyzer.Report report(long total, long healed) {
        return new RunAnalyzer.Report(total, 0, healed, 0, false, Map.of(), List.of(), 0);
    }
}
