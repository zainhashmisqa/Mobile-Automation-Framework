package com.azm.qa.mobile.locators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.azm.qa.mobile.healing.ElementFingerprint;

/**
 * Compares a fresh {@link ScreenCrawler} pass against the committed baseline in
 * {@code locators/<screen>.json} and reports drift — non-blocking, informational, the mobile
 * twin of the web framework's {@code locator-drift-check} CI job. It never fails a build by
 * itself: drift is a prompt to look, not an automatic gate.
 */
public final class LocatorDriftChecker {

    public DriftReport check(String screenKey, Map<String, ElementFingerprint> freshlyCrawled) {
        LocatorRepository baseline = new LocatorRepository(screenKey);

        List<String> newlyFound = new ArrayList<>();   // current crawl has it, baseline doesn't
        List<String> changed = new ArrayList<>();      // present in both, but the fingerprint moved
        List<String> missing = new ArrayList<>();      // baseline has it, current crawl didn't see it

        for (Map.Entry<String, ElementFingerprint> entry : freshlyCrawled.entrySet()) {
            var existing = baseline.get(entry.getKey());
            if (existing.isEmpty()) {
                newlyFound.add(entry.getKey());
            } else if (!fingerprintsRoughlyEqual(existing.get(), entry.getValue())) {
                changed.add(entry.getKey());
            }
        }
        for (String baselineKey : baseline.keys()) {
            if (!freshlyCrawled.containsKey(baselineKey)) {
                missing.add(baselineKey);
            }
        }

        return new DriftReport(screenKey, newlyFound, changed, missing);
    }

    private boolean fingerprintsRoughlyEqual(ElementFingerprint a, ElementFingerprint b) {
        return a.contentDesc().equals(b.contentDesc())
                && a.text().equals(b.text())
                && a.resourceId().equals(b.resourceId())
                && a.className().equals(b.className());
    }

    public record DriftReport(String screenKey, List<String> newlyFound, List<String> changed, List<String> missing) {
        public boolean hasDrift() {
            return !newlyFound.isEmpty() || !changed.isEmpty() || !missing.isEmpty();
        }

        public String summary() {
            return screenKey + ": " + newlyFound.size() + " new, " + changed.size() + " changed, "
                    + missing.size() + " missing";
        }
    }
}
