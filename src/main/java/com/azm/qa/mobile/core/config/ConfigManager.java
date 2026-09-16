package com.azm.qa.mobile.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single source of truth for configuration, with an explicit precedence chain:
 *
 * <ol>
 *   <li><b>Thread-scoped override</b> — set by the test/driver layer, see {@link #setThreadOverride}</li>
 *   <li><b>System property</b> — {@code -Dapp.package=...}</li>
 *   <li><b>Environment variable</b> — {@code APP_PACKAGE} (dots to underscores, upper-cased)</li>
 *   <li>{@code config-local.properties} — machine-specific values (an {@code app.path}, a real
 *       device's udid); git-ignored, loaded unconditionally if present, not committed</li>
 *   <li>{@code config-{env}.properties} — where {@code env} comes from {@code ENV}/{@code -Denv}</li>
 *   <li>{@code config.properties} — committed defaults, nothing machine-specific or secret</li>
 * </ol>
 *
 * <p>The thread-scoped tier is what makes true in-JVM parallel execution across different
 * devices possible. A process-wide {@code device.profile} forces {@code thread-count=1},
 * because every thread would read whichever profile was set last. Scoping it per thread
 * lets TestNG run one method against an API 34 emulator while another runs against API 36,
 * in the same JVM, without either seeing the other's capabilities.
 */
public final class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);

    private static final Properties BASE = new Properties();
    private static final Properties ENV_SPECIFIC = new Properties();
    private static final Properties LOCAL = new Properties();

    /** Per-thread overrides. Cleared in BaseTest's teardown so threads can be reused safely. */
    private static final ThreadLocal<Map<String, String>> THREAD_OVERRIDES =
            ThreadLocal.withInitial(HashMap::new);

    static {
        load(BASE, "config/config.properties", true);
        String env = System.getProperty("env", System.getenv("ENV"));
        if (env != null && !env.isBlank()) {
            load(ENV_SPECIFIC, "config/config-" + env + ".properties", false);
            log.info("Loaded environment profile: {}", env);
        }
        // Unconditional, optional: this is the file app.path/a real device's udid live in. Not
        // gated behind -Denv, because a machine-local override should apply regardless of which
        // (if any) env profile is active.
        if (load(LOCAL, "config/config-local.properties", false)) {
            log.info("Loaded config-local.properties");
        }
    }

    private ConfigManager() {
    }

    private static boolean load(Properties target, String resource, boolean required) {
        try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                if (required) {
                    throw new IllegalStateException("Missing required config resource: " + resource);
                }
                return false;
            }
            target.load(in);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read config resource: " + resource, e);
        }
    }

    /**
     * Overrides a key for the calling thread only. Set by {@code BaseTest} before the driver
     * is created so the whole stack below it reads this thread's device profile.
     */
    public static void setThreadOverride(String key, String value) {
        THREAD_OVERRIDES.get().put(key, value);
    }

    /** Clears this thread's overrides. Always call in teardown — TestNG reuses threads. */
    public static void clearThreadOverrides() {
        THREAD_OVERRIDES.remove();
    }

    public static String get(String key) {
        String threadValue = THREAD_OVERRIDES.get().get(key);
        if (threadValue != null) {
            return threadValue;
        }
        String sysValue = System.getProperty(key);
        if (sysValue != null && !sysValue.isBlank()) {
            return sysValue;
        }
        String envValue = System.getenv(toEnvKey(key));
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        String local = LOCAL.getProperty(key);
        if (local != null && !local.isBlank()) {
            return local;
        }
        String envSpecific = ENV_SPECIFIC.getProperty(key);
        if (envSpecific != null && !envSpecific.isBlank()) {
            return envSpecific;
        }
        String base = BASE.getProperty(key);
        return (base != null && !base.isBlank()) ? base : null;
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /** Fails loudly at setup rather than letting a null surface as a confusing Appium error. */
    public static String require(String key) {
        String value = get(key);
        if (value == null) {
            throw new IllegalStateException(
                    "Required config '" + key + "' is not set. Provide it via -D" + key
                            + ", the " + toEnvKey(key) + " environment variable, or config.properties.");
        }
        return value;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Config '{}' is not an integer ('{}'); using default {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    /** {@code app.package} -> {@code APP_PACKAGE} */
    private static String toEnvKey(String key) {
        return key.replace('.', '_').replace('-', '_').toUpperCase(java.util.Locale.ROOT);
    }
}
