package com.azm.qa.mobile.capability.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.connection.ConnectionState;

/**
 * S-40/S-41/S-42: airplane-mode toggle and Wi-Fi/cellular switching via Appium's own
 * {@code setConnection}/{@code getConnection} -- real OS network state, not a driver-side
 * simulation. S-43 (throttled network) is deliberately NOT covered here: Appium/UIAutomator2
 * has no supported "slow this connection down" primitive on a stock emulator, and CLAUDE.md's
 * mandatory honesty rule (#4) names network throttling explicitly as a condition to skip with a
 * reason rather than fake.
 */
public final class NetworkConditionUtils {

    private static final Logger log = LoggerFactory.getLogger(NetworkConditionUtils.class);

    private final AndroidDriver driver;

    public NetworkConditionUtils(AndroidDriver driver) {
        this.driver = driver;
    }

    public void setAirplaneMode(boolean enabled) {
        long mask = enabled ? ConnectionState.AIRPLANE_MODE_MASK : 0;
        driver.setConnection(new ConnectionState(mask));
        log.info("Airplane mode set to {}", enabled);
    }

    public void setWifiAndData(boolean wifi, boolean data) {
        long mask = (wifi ? ConnectionState.WIFI_MASK : 0) | (data ? ConnectionState.DATA_MASK : 0);
        driver.setConnection(new ConnectionState(mask));
        log.info("Wi-Fi={} Data={}", wifi, data);
    }

    public ConnectionState current() {
        return driver.getConnection();
    }
}
