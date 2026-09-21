# Capability layer coverage

Tracks which `capability/*` classes exist and which scenario IDs / baseline items they cover.
Add a row here whenever a capability class is added, per the repo's mandatory rules (§2/§3).
Scenario IDs and baseline item numbers trace back to the team's test-scenario spec; this file
does not reproduce that document, only the mapping. Everything below runs across two suites,
not one: `testng-smoke.xml` (smoke-tagged, CRITICAL/HIGH) and `testng-capability.xml`
(everything else in the package) -- `testng-capability.xml` deliberately excludes group=smoke
so a full `device-suite` run doesn't pay a session teardown/setup twice for the same method.

| Concern | Class | Status | Scenario(s) covered |
|---|---|---|---|
| OS-level effects (adb) | `capability.platform.PlatformActions` | Done | Facade underlying several rows below; no scenario of its own |
| Rotate / responsive | `capability.orientation.OrientationUtils` | Done | S-50 (real, Search screen only) |
| Cold/warm start, memory | `capability.performance.PerformanceUtils` | Done | S-03 (real); S-01 (relabeled onto existing `AppLaunchTest`) |
| Network / offline / throttle | `capability.network.NetworkConditionUtils` | Done | S-40, S-41, S-42 (real); S-43 logged skip (no throttling primitive) |
| Install / upgrade / low-storage | `capability.appmgmt.AppInstallUtils` | Done (unused by a passing test) | S-04, S-53 both logged skips -- see reasons in `BlockedScenariosTest` |
| TalkBack / font scale / targets | `capability.accessibility.AccessibilityUtils` | Done | S-70, S-71 (real) |
| Locale / RTL | `capability.localization.LocalizationUtils` | Done | S-72 (real attempt, honest skip if the device refuses) |
| Screenshot diff | `capability.visual.VisualRegressionUtils` | Done | S-80 (real, single density) |
| Notifications / background | `capability.notifications.NotificationUtils` | Not started | S-60, S-61 -- both logged skips (no server-side control over a real push); building the class now would have no real test to back it |
| Interrupt injection | `capability.interrupt.InterruptUtils` | Not started | S-51 handled via `AppLifecycleUtils` directly (no dedicated interrupt class needed); S-52 is a logged skip per CLAUDE.md #4 |
| Video on failure | `capability.recording.ScreenRecorderUtils` | Not started | No scenario ID of its own (baseline item 21); not wired into the failure listener yet |
| External API oracle/seed | `capability.api.AptoideApiClient` | Not started | S-20, S-22 -- no known Aptoide API contract is documented in this repo; CLAUDE.md's own no-fabrication rule blocks guessing one |

## Scenario-by-scenario status (34 total)

Real, passing-if-the-device-cooperates coverage (21): S-01 (`AppLaunchTest`, relabeled),
S-02, S-03, S-10, S-11, S-12, S-13, S-14, S-30, S-31, S-32, S-33, S-40, S-41, S-42, S-50, S-51,
S-70, S-71, S-72 (attempt-then-honest-skip), S-80.

Logged skip, not a fake pass (12), all in `BlockedScenariosTest` except S-43 (in
`NetworkConditionTest`) and S-72 (attempts first, in `AccessibilityLocalizationTest`): S-04,
S-15, S-20, S-21, S-22, S-23, S-43, S-52, S-53, S-54, S-60, S-61.

Structurally covered by existing config, no new test written (1): S-81 (parallel device-pool
execution) -- `testng.xml`'s `parallel="methods"` + `thread-count="2"` across the
`pixel7_api36`/`pixel7_api34` profiles in `device-profiles.json` already is the mechanism this
scenario asks for, and `DevicePool`'s port-isolation logic already has unit-test coverage. What
it has never had is two REAL emulators running concurrently in the same job -- `device-suite`
boots exactly one. That gap is pre-existing and unrelated to this pass.

## Known gaps in what's covered so far

- S-50/S-51/S-70/S-71/S-72/S-80 only exercise the Home/Search screens. Several scenarios (S-15,
  S-20, S-21, S-22, S-23) explicitly need locators this repo hasn't crawled yet -- an app-detail
  screen, a search-results list, other bottom-nav tabs, an empty-state view. None of those are
  guessed; each is a logged skip in `BlockedScenariosTest` naming exactly what's missing.
- S-30/S-31/S-32/S-33 and S-40 prove the grant/deny and network-toggle *mechanism* at the OS
  level; none of them are driven through the actual in-app trigger point (a download, a camera
  action, a login flow) the spec describes, because that trigger point needs the same missing
  screens as above.
- S-54 needs a tablet device profile that doesn't exist in `device-profiles.json`.
- S-60/S-61 need control over Aptoide's production push infrastructure, which this team doesn't
  own -- matches the "no server-side data seeding" constraint in the team's own spec.
