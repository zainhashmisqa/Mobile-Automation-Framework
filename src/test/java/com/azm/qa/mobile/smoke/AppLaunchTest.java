package com.azm.qa.mobile.smoke;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.azm.qa.mobile.base.BaseTest;
import com.azm.qa.mobile.base.RetryAnalyzer;
import com.azm.qa.mobile.core.config.ConfigManager;
import com.azm.qa.mobile.core.lifecycle.AppLifecycleUtils;
import com.azm.qa.mobile.report.AllureMeta;

/**
 * The first, most honest test in this framework: it asserts only what can be verified without
 * guessing a single locator. com.titanstudio.pro's real Semantics labels are not yet known on
 * this machine (no emulator has been able to run the app during development), so no screen
 * object claims to know a real element here -- that would repeat exactly the mistake flagged
 * in the earlier framework review ("never invent a locator you haven't verified").
 *
 * <p>{@link #dumpPageSourceForLocatorDiscovery()} is the tool for finding out: it captures page
 * source at fixed intervals after launch, because an app's real UI only appears after
 * its engine attaches -- capturing source immediately after launch here would show nothing but
 * the native splash frame and look like "this app has no accessibility tree", which would be
 * wrong. Run it, read the dumps under {@code target/page-source/}, and use what you find to
 * write the first real screen object.
 */
public class AppLaunchTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(AppLaunchTest.class);
    private static final Path DUMP_DIR = Path.of("target", "page-source");

    @Test(groups = "smoke", description = "The app installs, launches, and reaches the foreground",
            retryAnalyzer = RetryAnalyzer.class)
    public void appLaunchesAndReachesForeground() {
        AllureMeta.annotate(AllureMeta.Meta.of("SMOKE-001 / S-01", "App lifecycle", "Launch", "CRITICAL",
                "com.titanstudio.pro installs (or is already installed), launches, and is the foreground app. "
                        + "Also stands in for S-01 (cold start on a clean device) from the team's test-scenario "
                        + "spec -- see capability/COVERAGE.md."));

        String appPackage = ConfigManager.require("app.package");
        AppLifecycleUtils lifecycle = new AppLifecycleUtils(driver, appPackage);

        Assert.assertTrue(lifecycle.isInstalled(), appPackage + " should be installed on the device/emulator");

        // No named locator to wait on yet -- see the class javadoc -- so readiness here is
        // structural: the driver has a session and the app is the process in the foreground,
        // which is everything that can be honestly claimed before a real screen is mapped.
        waits.waitUntilTrue(d -> lifecycle.state().contains("RUNNING_IN_FOREGROUND"),
                Duration.ofSeconds(ConfigManager.getInt("wait.appReadyTimeoutSeconds", 60)),
                "app state to reach RUNNING_IN_FOREGROUND");

        String pageSource = driver.getPageSource();
        Assert.assertFalse(pageSource.isBlank(), "page source should not be empty once the app is foregrounded");
    }

    @Test(groups = "regression",
            description = "Timed page-source snapshots for locator discovery -- captures the app's real UI once it has settled",
            retryAnalyzer = RetryAnalyzer.class)
    public void dumpPageSourceForLocatorDiscovery() {
        AllureMeta.annotate(AllureMeta.Meta.of("SMOKE-002", "Locator discovery", "Splash transition", "MEDIUM",
                "Captures page source at t=0/5/10/15s so a screen's real locators can be read off once "
                        + "the UI has actually settled, instead of whatever is on screen mid-launch."));

        int[] offsetsSeconds = {0, 5, 10, 15};
        for (int offset : offsetsSeconds) {
            if (offset > 0) {
                waits.pauseFor(Duration.ofSeconds(5));
            }
            String source = driver.getPageSource();
            Path file = dumpTo(offset, source);
            log.info("t={}s page source ({} chars) written to {}", offset, source.length(), file);
        }
    }

    private Path dumpTo(int offsetSeconds, String pageSource) {
        try {
            Files.createDirectories(DUMP_DIR);
            Path file = DUMP_DIR.resolve("t" + offsetSeconds + "s.xml");
            Files.writeString(file, pageSource);
            return file;
        } catch (Exception e) {
            throw new java.io.UncheckedIOException(
                    "Could not write page source dump for t=" + offsetSeconds + "s",
                    e instanceof java.io.IOException io ? io : new java.io.IOException(e));
        }
    }
}
