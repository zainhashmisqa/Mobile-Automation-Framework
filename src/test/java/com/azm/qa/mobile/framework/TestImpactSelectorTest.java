package com.azm.qa.mobile.framework;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.azm.qa.mobile.analysis.TestImpactSelector;

/**
 * Contract test for {@link TestImpactSelector} that needs no prepared git history: an
 * unresolvable ref (this repo may be shallow, mid-rebase, or simply not have the requested
 * ref) must degrade to "no changes selected" rather than throwing and taking the whole
 * {@code impacted} pipeline command down with it. The full-suite-fallback and file-to-class
 * mapping behaviour is exercised in practice by running
 * {@code mvn exec:java -Dexec.args="impacted"} against a real diff — see the README.
 */
public class TestImpactSelectorTest {

    @Test
    public void unresolvableGitRefDegradesToEmptyResultInsteadOfThrowing() {
        TestImpactSelector selector = new TestImpactSelector();

        TestImpactSelector.Result result = selector.select("this-ref-does-not-exist-anywhere-" + System.nanoTime());

        Assert.assertNotNull(result);
        Assert.assertTrue(result.impactedClasses().isEmpty());
        Assert.assertFalse(result.fullSuiteFallback());
    }
}
