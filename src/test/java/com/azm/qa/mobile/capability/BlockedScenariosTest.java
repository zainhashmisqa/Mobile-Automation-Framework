package com.azm.qa.mobile.capability;

import org.testng.SkipException;
import org.testng.annotations.Test;

import com.azm.qa.mobile.base.BaseTest;
import com.azm.qa.mobile.report.AllureMeta;

/**
 * Scenarios from the team's test-scenario spec that this repo cannot honestly exercise yet --
 * each one is a logged skip with a specific, attributed reason, never a fake pass, per
 * CLAUDE.md's mandatory honesty rule (#4). Grouped here rather than skipped silently so the gap
 * stays visible in the Allure report and in capability/COVERAGE.md, which lists the same
 * reasons against each capability class.
 *
 * <p>Two shapes of reason show up below: a genuinely external constraint (no server-side data
 * seeding, no previous-build artifact, an unsafe-on-shared-CI simulation) that isn't fixable by
 * writing more framework code, and a "needs a live-device crawl" gap (a results list, an
 * app-detail screen, extra nav tabs) that {@code /gen-screen} could close once someone runs it
 * against a real device -- writing a locator for either without that crawl would break the
 * "never invent a locator" rule.
 */
public class BlockedScenariosTest extends BaseTest {

    @Test(groups = "regression", description = "S-04: app upgrade -- no previous build artifact committed")
    public void appUpgrade() {
        annotateAndSkip("S-04", "Launch, Install & Smoke", "App upgrade", "MEDIUM",
                "Only the current build (v9.22.5.3) is committed to this repo -- there is no previous "
                        + "build APK to install-then-upgrade from.");
    }

    @Test(groups = "regression", description = "S-15: bottom-tab traversal across all main sections -- nav-tab locators not yet crawled")
    public void bottomTabTraversal() {
        annotateAndSkip("S-15", "Navigation & Deep Links", "Bottom-tab traversal", "HIGH",
                "HomeScreen only anchors on the Search entry point today. The other bottom-nav "
                        + "destinations (Editorial/Stores/Apps) have no committed locator baseline -- "
                        + "needs `crawl --screen HomeScreen` run against a live device, not a guess.");
    }

    @Test(groups = "regression", description = "S-20: search results list -- results-list locator not yet crawled")
    public void searchResultsListPopulatesAndScrolls() {
        annotateAndSkip("S-20", "Search, Browse & Content Loading", "Search results list", "HIGH",
                "No committed locator baseline for the results RecyclerView or its items -- SearchScreen's "
                        + "baseline only covers the input field and bottom nav.");
    }

    @Test(groups = "regression", description = "S-21: scrolling a long results list -- same missing baseline as S-20")
    public void scrollingLongResultsList() {
        annotateAndSkip("S-21", "Search, Browse & Content Loading", "Scroll long results list", "MEDIUM",
                "Depends on the same results-list baseline missing for S-20.");
    }

    @Test(groups = "regression", description = "S-22: app detail screen -- no AppDetailScreen object exists yet")
    public void appDetailScreenRenders() {
        annotateAndSkip("S-22", "Search, Browse & Content Loading", "App detail screen", "HIGH",
                "No screen object or locator baseline exists for the app-detail screen yet -- needs "
                        + "`crawl --screen AppDetailScreen` against a live device first.");
    }

    @Test(groups = "regression", description = "S-23: empty/no-results search state -- empty-state locator not yet crawled")
    public void emptySearchState() {
        annotateAndSkip("S-23", "Search, Browse & Content Loading", "Empty search state", "MEDIUM",
                "No committed locator baseline for the empty-results state message/view.");
    }

    @Test(groups = "regression", description = "S-52: incoming call / system dialog interrupt -- emulator/farm-dependent")
    public void incomingCallInterrupt() {
        annotateAndSkip("S-52", "Lifecycle, Interrupts & Orientation", "Incoming call / system dialog interrupt", "MEDIUM",
                "Named explicitly in CLAUDE.md's mandatory honesty rule (#4) as a condition to skip "
                        + "with a reason if the device refuses it -- no incoming-call-injection helper is "
                        + "wired into this framework yet.");
    }

    @Test(groups = "regression", description = "S-53: low-storage during install -- unsafe to simulate on a shared CI runner")
    public void lowStorageDuringInstall() {
        annotateAndSkip("S-53", "Lifecycle, Interrupts & Orientation", "Low-storage install", "LOW",
                "Deliberately filling emulator storage risks the CI runner itself, not just this test -- "
                        + "not attempted without a disposable, isolated device.");
    }

    @Test(groups = "regression", description = "S-54: tablet vs phone layout -- no tablet device profile configured")
    public void tabletVsPhoneLayout() {
        annotateAndSkip("S-54", "Lifecycle, Interrupts & Orientation", "Tablet vs phone layout", "MEDIUM",
                "device-profiles.json only defines two phone profiles (pixel7_api36/pixel7_api34) -- "
                        + "no tablet profile/AVD exists to run this against.");
    }

    @Test(groups = "regression", description = "S-60: push notification tap-through -- no server-side control to trigger a real push")
    public void pushNotificationTapThrough() {
        annotateAndSkip("S-60", "Push Notifications & Background Services", "Push notification tap-through", "MEDIUM",
                "Aptoide's production backend is not owned by this team -- there is no way to trigger "
                        + "a real FCM push on demand, and faking one via a local notification wouldn't "
                        + "prove FirebaseMessagingService's real deep-link handling.");
    }

    @Test(groups = "regression", description = "S-61: download-complete notification tap -- depends on the same missing download flow as S-22/S-30")
    public void downloadCompleteNotification() {
        annotateAndSkip("S-61", "Push Notifications & Background Services", "Download-complete notification", "MEDIUM",
                "No download-trigger screen object exists yet (see S-22), so there is no real "
                        + "download to complete and notify on.");
    }

    private void annotateAndSkip(String scenarioId, String feature, String story, String severity, String reason) {
        AllureMeta.annotate(AllureMeta.Meta.of(scenarioId, feature, story, severity,
                "Logged skip, not a fake pass -- see CLAUDE.md's mandatory honesty rule (#4). " + reason));
        throw new SkipException(reason);
    }
}
