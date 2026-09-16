package com.azm.qa.mobile.locators;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.azm.qa.mobile.healing.ElementFingerprint;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * One screen's fingerprint baselines — {@code description -> ElementFingerprint} — backed by
 * {@code src/test/resources/locators/<screen>.json}.
 *
 * <p>These are what the self-healing cascade scores candidates against. A baseline is
 * committed once a screen's happy path has run successfully at least once: either seeded by
 * hand from {@code ScreenCrawler}'s output, or bootstrapped automatically the first time
 * {@code BaseScreen} resolves an element that has no baseline recorded yet (see
 * {@code BaseScreen#bootstrapIfAbsent}). Either way the file is meant to be committed and
 * reviewed like any other test asset — it is not a runtime-only cache.
 */
public final class LocatorRepository {

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, ElementFingerprint> baselines;
    private volatile boolean dirty = false;

    public LocatorRepository(String screenKey) {
        this.file = Path.of("src", "test", "resources", "locators", screenKey + ".json");
        this.baselines = loadFromClasspathOrDisk(screenKey);
    }

    public Optional<ElementFingerprint> get(String description) {
        return Optional.ofNullable(baselines.get(description));
    }

    public boolean has(String description) {
        return baselines.containsKey(description);
    }

    public java.util.Set<String> keys() {
        return java.util.Set.copyOf(baselines.keySet());
    }

    /** Records a new baseline in memory and persists it immediately — bootstrapping is rare, so this cost is fine. */
    public synchronized void put(String description, ElementFingerprint fingerprint) {
        baselines.put(description, fingerprint);
        dirty = true;
        flush();
    }

    private synchronized void flush() {
        if (!dirty) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), baselines);
            dirty = false;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write locator baseline: " + file, e);
        }
    }

    private Map<String, ElementFingerprint> loadFromClasspathOrDisk(String screenKey) {
        // Prefer the on-disk file (what a bootstrap just wrote, or what a developer just edited)
        // over the classpath copy, which may be stale relative to target/classes.
        if (Files.isRegularFile(file)) {
            try {
                return new LinkedHashMap<>(mapper.readValue(file.toFile(),
                        mapper.getTypeFactory().constructMapType(LinkedHashMap.class,
                                String.class, ElementFingerprint.class)));
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read locator baseline: " + file, e);
            }
        }
        String resource = "locators/" + screenKey + ".json";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                return new LinkedHashMap<>();
            }
            return new LinkedHashMap<>(mapper.readValue(in,
                    mapper.getTypeFactory().constructMapType(LinkedHashMap.class,
                            String.class, ElementFingerprint.class)));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read locator baseline resource: " + resource, e);
        }
    }
}
