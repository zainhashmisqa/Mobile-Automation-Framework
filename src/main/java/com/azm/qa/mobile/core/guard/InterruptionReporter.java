package com.azm.qa.mobile.core.guard;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import io.qameta.allure.Allure;

/**
 * Thread-scoped interruption counter, mirroring {@code HealReporter}. Deliberately
 * thread-scoped rather than held on the {@link InterferenceGuard} instance itself: a screen
 * object constructs a fresh guard every time (see {@code BaseScreen}), so an instance-level
 * counter would silently reset on every {@code new SomeScreen(driver)} and could never reach
 * the test listener. Scoping it to the thread is what lets one test method's interruptions,
 * across however many screen objects it touches, be read once at teardown.
 */
public final class InterruptionReporter {

    private static final ThreadLocal<AtomicInteger> COUNT = ThreadLocal.withInitial(AtomicInteger::new);

    private InterruptionReporter() {
    }

    /**
     * The Allure label is set here, live, at the moment of the interruption -- not deferred to
     * the test listener. TestNG doesn't guarantee listener execution order, and Allure's own
     * listener finalizes a test's result before this framework's {@code TestListener} runs, so
     * a label set from {@code onTestSuccess}/{@code onTestFailure} attaches to nothing (see
     * {@code HealReporter}, where this was confirmed directly against a real run).
     */
    public static void report(String matchedActivity, String fullComponent) {
        int countSoFar = COUNT.get().incrementAndGet();

        String body = "matched: " + matchedActivity + "\ncomponent: " + fullComponent
                + "\naction: pressed BACK to dismiss";
        Allure.addAttachment("Interference dismissed: " + matchedActivity, "text/plain",
                new java.io.ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)), ".txt");

        Allure.label("interrupted", "true");
        Allure.label("interruptionCount", String.valueOf(countSoFar));
    }

    public static int countForCurrentTestAndReset() {
        return COUNT.get().getAndSet(0);
    }
}
