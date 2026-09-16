package com.azm.qa.mobile.core.driver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azm.qa.mobile.core.config.ConfigManager;

/**
 * Derives a unique {@code systemPort}/{@code mjpegServerPort}/{@code chromedriverPort} triple
 * per thread — the single place that guarantees {@code parallel="methods"} can never collide
 * two UiAutomator2 sessions on the same port on one host.
 *
 * <p>Before this class existed, ports lived as hand-picked, hardcoded values in
 * {@code device-profiles.json} (one entry per named device). That works for exactly as many
 * profiles as someone remembered to give a unique port — add a third profile without one, or
 * copy-paste an existing entry's port, and two sessions silently fight over the same
 * UiAutomator2 instrumentation port. That failure mode ("instrumentation process cannot be
 * initialized") reads exactly like a flaky device, not a port clash, which is what makes it
 * dangerous. Deriving ports from the thread itself removes the possibility entirely: there is
 * no port to forget to set, because nothing hardcodes one.
 *
 * <p>Assignment is permanent for a thread's lifetime, not reacquired per test method. TestNG's
 * {@code parallel="methods"} runs against a fixed-size thread pool ({@code thread-count} in
 * the suite XML), so the same physical thread runs many test methods in sequence — reassigning
 * ports on every method would only add churn for a value that never needed to change mid-suite.
 */
public final class DevicePool {

    private static final Logger log = LoggerFactory.getLogger(DevicePool.class);

    private static final AtomicInteger NEXT_SLOT = new AtomicInteger(0);
    private static final ConcurrentHashMap<Long, PortSlot> ASSIGNED = new ConcurrentHashMap<>();

    private DevicePool() {
    }

    /**
     * Returns this thread's port slot, assigning one on first call. Safe to call repeatedly
     * (e.g. once per test method) — later calls on the same thread return the same slot.
     */
    public static PortSlot acquireSlotForCurrentThread() {
        long threadId = Thread.currentThread().threadId();
        return ASSIGNED.computeIfAbsent(threadId, id -> {
            int slot = NEXT_SLOT.getAndIncrement();
            PortSlot assigned = new PortSlot(
                    slot,
                    systemPortBase() + slot,
                    mjpegPortBase() + slot,
                    chromedriverPortBase() + slot);
            log.info("Assigned port slot {} to thread {}: {}", slot, id, assigned);
            return assigned;
        });
    }

    /** Total number of distinct threads that have ever requested a slot in this JVM. */
    public static int slotsIssued() {
        return NEXT_SLOT.get();
    }

    private static int systemPortBase() {
        return ConfigManager.getInt("devicepool.systemPort.base", 8200);
    }

    private static int mjpegPortBase() {
        return ConfigManager.getInt("devicepool.mjpegPort.base", 7810);
    }

    private static int chromedriverPortBase() {
        return ConfigManager.getInt("devicepool.chromedriverPort.base", 9515);
    }

    public record PortSlot(int slotIndex, int systemPort, int mjpegServerPort, int chromedriverPort) {
    }
}
