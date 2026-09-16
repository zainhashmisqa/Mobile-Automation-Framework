package com.azm.qa.mobile.healing;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import io.qameta.allure.Allure;
import io.qameta.allure.model.StatusDetails;

/**
 * Turns a successful heal into something visible: an Allure attachment AND label on the
 * current test, set live while the test is still running — plus a per-thread counter the test
 * listener reads at teardown for the console log line.
 *
 * <p>This is the enforcement of the framework's one hard rule for self-healing: <b>a heal is
 * never silent.</b> Whoever reviews the run sees exactly which locator broke, what it was
 * replaced with, by which strategy, and at what confidence — the same information a human
 * would need to decide whether to update the source locator.
 *
 * <p><b>The Allure label is set here, at heal time, not deferred to the test listener.</b>
 * TestNG does not guarantee listener execution order, and Allure's own listener finalizes a
 * test's result before this framework's {@code TestListener} runs — a label set from
 * {@code onTestSuccess}/{@code onTestFailure} silently attaches to nothing, while the test's
 * own Allure context is provably still open here, mid-method. Confirmed directly: the
 * attachment (set here) showed up in the result JSON; a label set from the listener, on an
 * earlier version of this class, did not.
 */
public final class HealReporter {

    private static final ThreadLocal<AtomicInteger> HEAL_COUNT = ThreadLocal.withInitial(AtomicInteger::new);

    private HealReporter() {
    }

    public static void report(String description, String originalStrategy, String healedStrategy,
                               double confidence) {
        int countSoFar = HEAL_COUNT.get().incrementAndGet();

        String body = "description: " + description
                + "\noriginal strategy: " + originalStrategy
                + "\nhealed strategy:   " + healedStrategy
                + "\nconfidence:        " + String.format(java.util.Locale.ROOT, "%.2f", confidence);

        Allure.addAttachment("Self-heal: " + description, "text/plain",
                new java.io.ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)), ".txt");

        Allure.label("healed", "true");
        Allure.label("healCount", String.valueOf(countSoFar));

        // Allure's own Categories tab can only match on status + a regex over
        // statusDetails.message -- it has no way to filter on a custom label. Without this,
        // categories.json's "Self-healed" entry has nothing distinctive to match against a
        // passed test (which normally carries no message at all), so it either matches
        // nothing or -- if written broadly -- matches everything. Writing a real marker here
        // is what makes that category tab entry genuinely accurate instead of decorative.
        String healNote = "HEALED: " + description + " via " + healedStrategy
                + " (confidence " + String.format(java.util.Locale.ROOT, "%.2f", confidence) + ")";
        Allure.getLifecycle().updateTestCase(testResult -> {
            StatusDetails details = testResult.getStatusDetails();
            if (details == null) {
                details = new StatusDetails();
                testResult.setStatusDetails(details);
            }
            String existing = details.getMessage();
            details.setMessage(existing == null || existing.isBlank() ? healNote : existing + "\n" + healNote);
        });
    }

    /** Read by the test listener at teardown, then reset for the next test on this thread. */
    public static int healCountForCurrentTestAndReset() {
        return HEAL_COUNT.get().getAndSet(0);
    }
}
