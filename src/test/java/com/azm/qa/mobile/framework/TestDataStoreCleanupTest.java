package com.azm.qa.mobile.framework;

import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.azm.qa.mobile.data.TestDataStore;

/**
 * Pure unit tests for {@link TestDataStore}'s cleanup wiring — no device, no driver. Proves
 * three contracts: a registered handler actually runs, an unregistered kind is counted rather
 * than silently dropped, and one handler throwing doesn't stop the rest from running.
 */
public class TestDataStoreCleanupTest {

    @AfterMethod(alwaysRun = true)
    public void resetStoreState() {
        TestDataStore.clearForCurrentThread();
        // Handlers are process-wide by design (see TestDataStore's javadoc), so reset the one
        // shared kind name these tests reuse ("test-account") back to a known no-op between
        // tests rather than leaking one test's handler into the next. Tests that need an
        // "unregistered kind" use a nanoTime-suffixed name instead, so they never collide with
        // a handler some other test registered.
        TestDataStore.registerCleanupHandler("test-account", entry -> { });
    }

    @Test
    public void registeredHandlerRunsForEachSavedEntity() {
        AtomicInteger invocations = new AtomicInteger(0);
        TestDataStore.registerCleanupHandler("test-account", entry -> invocations.incrementAndGet());

        TestDataStore.save("test-account", "user1@example.com");
        TestDataStore.save("test-account", "user2@example.com");

        TestDataStore.CleanupSummary summary = TestDataStore.cleanupAllForCurrentThread();

        Assert.assertEquals(invocations.get(), 2);
        Assert.assertEquals(summary.attempted(), 2);
        Assert.assertEquals(summary.succeeded(), 2);
        Assert.assertEquals(summary.skippedNoHandler(), 0);
    }

    @Test
    public void unregisteredKindIsCountedNotSilentlyDropped() {
        TestDataStore.save("kind-with-no-handler-" + System.nanoTime(), "some-identifier");

        TestDataStore.CleanupSummary summary = TestDataStore.cleanupAllForCurrentThread();

        Assert.assertEquals(summary.attempted(), 0);
        Assert.assertEquals(summary.skippedNoHandler(), 1);
    }

    @Test
    public void oneFailingHandlerDoesNotStopOthersFromRunning() {
        String failingKind = "test-failing-kind-" + System.nanoTime();
        AtomicInteger otherInvocations = new AtomicInteger(0);

        TestDataStore.registerCleanupHandler(failingKind, entry -> {
            throw new RuntimeException("simulated cleanup failure");
        });
        TestDataStore.registerCleanupHandler("test-account", entry -> otherInvocations.incrementAndGet());

        TestDataStore.save(failingKind, "identifier-1");
        TestDataStore.save("test-account", "identifier-2");

        TestDataStore.CleanupSummary summary = TestDataStore.cleanupAllForCurrentThread();

        Assert.assertEquals(otherInvocations.get(), 1, "the second handler should still have run");
        Assert.assertEquals(summary.attempted(), 2);
        Assert.assertEquals(summary.succeeded(), 1, "only the non-throwing handler counts as succeeded");
    }

    @Test
    public void latestReturnsTheMostRecentlySavedEntityOfAKind() {
        String kind = "test-latest-kind-" + System.nanoTime();

        TestDataStore.save(kind, "first@example.com");
        TestDataStore.save(kind, "second@example.com");

        var latest = TestDataStore.latest(kind);

        Assert.assertTrue(latest.isPresent());
        Assert.assertEquals(latest.get().identifyingFields()[0], "second@example.com");
    }
}
