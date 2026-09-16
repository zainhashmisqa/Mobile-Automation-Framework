package com.azm.qa.mobile.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * One entry from {@code config/device-profiles.json} — a named device the suite can target.
 *
 * <p>Device fragmentation is a real skill target, not a checkbox: the same suite has to run
 * against different Android versions and screen sizes and still pass. Naming each device here
 * (rather than hardcoding capabilities) is what lets {@code -Ddevice.profile=pixel7_api34}
 * switch the whole run, and what lets a parallel run drive several profiles at once.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DeviceProfile {

    private static final String RESOURCE = "config/device-profiles.json";
    private static volatile Map<String, DeviceProfile> cache;

    private String name;
    private String platformName = "Android";
    private String automationName = "UiAutomator2";
    private String deviceName;
    private String platformVersion;
    private String udid;
    private Boolean emulator;
    private String notes;

    public static DeviceProfile active() {
        return byName(ConfigManager.get("device.profile", "default"));
    }

    public static DeviceProfile byName(String profileName) {
        Map<String, DeviceProfile> profiles = all();
        DeviceProfile profile = profiles.get(profileName);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Unknown device.profile '" + profileName + "'. Known profiles: " + profiles.keySet());
        }
        return profile;
    }

    public static List<String> names() {
        return List.copyOf(all().keySet());
    }

    private static Map<String, DeviceProfile> all() {
        Map<String, DeviceProfile> local = cache;
        if (local == null) {
            synchronized (DeviceProfile.class) {
                local = cache;
                if (local == null) {
                    local = load();
                    cache = local;
                }
            }
        }
        return local;
    }

    private static Map<String, DeviceProfile> load() {
        try (InputStream in = DeviceProfile.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + RESOURCE);
            }
            ObjectMapper mapper = new ObjectMapper();
            Map<String, DeviceProfile> parsed = mapper.readValue(in,
                    mapper.getTypeFactory().constructMapType(java.util.LinkedHashMap.class,
                            String.class, DeviceProfile.class));
            parsed.forEach((key, value) -> value.name = key);
            return parsed;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + RESOURCE, e);
        }
    }

    public String getName() {
        return name;
    }

    public String getPlatformName() {
        return platformName;
    }

    public String getAutomationName() {
        return automationName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public String getUdid() {
        return udid;
    }

    public Boolean getEmulator() {
        return emulator;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public String toString() {
        return "DeviceProfile{" + name + ", device=" + deviceName
                + ", platformVersion=" + platformVersion + ", udid=" + udid + "}";
    }
}
