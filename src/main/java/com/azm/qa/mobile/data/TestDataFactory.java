package com.azm.qa.mobile.data;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates data that is unique per call, per worker, per run — the mobile twin of the web
 * framework's {@code buildAccount()}. Parallel test methods (or parallel device profiles)
 * calling this at the same instant still never collide, which matters for anything the app
 * treats as "already registered" or "already exists".
 */
public final class TestDataFactory {

    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    private TestDataFactory() {
    }

    /** A phone-like string unlikely to collide with anything else running concurrently. */
    public static String buildPhoneNumber(String countryPrefix) {
        long seq = SEQUENCE.incrementAndGet();
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return countryPrefix + System.currentTimeMillis() % 1_000_000 + seq + rand;
    }

    public static String buildDisplayName(String base) {
        return base + "-" + SEQUENCE.incrementAndGet() + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    public static String buildEmail(String localPartBase, String domain) {
        return localPartBase + "+" + System.currentTimeMillis() + SEQUENCE.incrementAndGet() + "@" + domain;
    }
}
