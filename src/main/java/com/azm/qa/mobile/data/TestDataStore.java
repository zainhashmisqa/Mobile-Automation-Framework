package com.azm.qa.mobile.data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thread-scoped, deduplicated record of entities a test created (an account registered
 * on-device, a profile completed) — the mobile twin of the web framework's
 * {@code test-data-store.ts}. Records are keyed by a SHA-256 fingerprint of their identifying
 * fields, so a test that (re-)saves "the same" entity twice never stores it twice, and
 * {@link #latest} always returns the most recently created entity of that kind for reuse.
 *
 * <p><b>Cleanup.</b> A test calling {@link #save} is declaring "I created something real that
 * shouldn't just accumulate on the backend forever." {@link #registerCleanupHandler} lets a
 * screen or test base class attach the actual deletion steps for one {@code kind} once;
 * {@code BaseTest} then calls {@link #cleanupAllForCurrentThread()} at teardown, before the
 * driver quits, so a handler can still drive the UI (or call an API) to remove what this test
 * created. No handlers are registered by default — nothing in this framework invents a
 * "delete account" flow for an app it hasn't crawled a real screen for yet (see CLAUDE.md's
 * "never invent a locator" rule); this is the seam a real screen wires into once one exists.
 */
public final class TestDataStore {

    private static final Logger log = LoggerFactory.getLogger(TestDataStore.class);

    private static final ThreadLocal<Map<String, Entry>> BY_FINGERPRINT =
            ThreadLocal.withInitial(LinkedHashMap::new);

    /** Process-wide, not thread-scoped: the deletion steps for a "kind" don't vary by thread. */
    private static final Map<String, Consumer<Entry>> CLEANUP_HANDLERS = new ConcurrentHashMap<>();

    private TestDataStore() {
    }

    public static void save(String kind, String... identifyingFields) {
        String fingerprint = fingerprint(kind, identifyingFields);
        BY_FINGERPRINT.get().put(fingerprint, new Entry(kind, identifyingFields, System.currentTimeMillis()));
    }

    public static Optional<Entry> latest(String kind) {
        return BY_FINGERPRINT.get().values().stream()
                .filter(e -> e.kind.equals(kind))
                .reduce((first, second) -> second); // last inserted of this kind
    }

    /**
     * Registers how to delete one {@code kind} of entity. Call once, typically from a screen's
     * static initializer or a base test class's setup — e.g.
     * {@code TestDataStore.registerCleanupHandler("account", entry -> accountScreen.delete(entry))}.
     * Registering the same kind twice replaces the previous handler.
     */
    public static void registerCleanupHandler(String kind, Consumer<Entry> handler) {
        CLEANUP_HANDLERS.put(kind, handler);
    }

    /**
     * Runs the registered handler for every entity this thread created, best-effort: one
     * handler throwing doesn't stop the rest from running, and a kind with no registered
     * handler is counted, not silently dropped, so an unregistered kind shows up in the summary
     * rather than looking like cleanup succeeded when nothing actually ran.
     */
    public static CleanupSummary cleanupAllForCurrentThread() {
        Map<String, Entry> entries = BY_FINGERPRINT.get();
        int attempted = 0;
        int succeeded = 0;
        int noHandler = 0;

        for (Entry entry : entries.values()) {
            Consumer<Entry> handler = CLEANUP_HANDLERS.get(entry.kind());
            if (handler == null) {
                noHandler++;
                continue;
            }
            attempted++;
            try {
                handler.accept(entry);
                succeeded++;
            } catch (Exception e) {
                log.warn("Cleanup handler for kind '{}' failed: {}", entry.kind(), e.getMessage());
            }
        }

        CleanupSummary summary = new CleanupSummary(attempted, succeeded, noHandler);
        if (noHandler > 0) {
            log.info("{} {} created this test have no registered cleanup handler and were left as-is",
                    noHandler, noHandler == 1 ? "entity" : "entities");
        }
        return summary;
    }

    public static void clearForCurrentThread() {
        BY_FINGERPRINT.remove();
    }

    private static String fingerprint(String kind, String... fields) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(kind.getBytes(StandardCharsets.UTF_8));
            for (String field : fields) {
                digest.update((byte) 0);
                digest.update((field == null ? "" : field).getBytes(StandardCharsets.UTF_8));
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record Entry(String kind, String[] identifyingFields, long createdAtEpochMillis) {
    }

    public record CleanupSummary(int attempted, int succeeded, int skippedNoHandler) {
    }
}
