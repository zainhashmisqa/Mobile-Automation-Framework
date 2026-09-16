package com.azm.qa.mobile.base;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import com.azm.qa.mobile.core.config.ConfigManager;

/**
 * Retries a failed test up to {@code test.retryCount} times. Deliberately per-method (keyed by
 * the TestNG result's test name), not global, so one flaky method retrying doesn't consume a
 * budget meant for a different one running in a parallel thread.
 */
public final class RetryAnalyzer implements IRetryAnalyzer {

    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();

    @Override
    public boolean retry(ITestResult result) {
        int maxRetries = ConfigManager.getInt("test.retryCount", 1);
        String key = result.getTestClass().getName() + "#" + result.getMethod().getMethodName();
        AtomicInteger count = attempts.computeIfAbsent(key, k -> new AtomicInteger(0));

        if (count.get() < maxRetries) {
            count.incrementAndGet();
            return true;
        }
        return false;
    }
}
