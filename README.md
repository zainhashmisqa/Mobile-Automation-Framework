# AZM Agentic Android Automation Framework

Appium + TestNG **Android** automation framework, built for the AZM QA → QE transformation
programme's AI-Augmented Automation milestone. Originally scaffolded against
**com.titanstudio.pro** (Flutter); the live assessment target is **Aptoide**
(`cm.aptoide.pt` v9.22.5.3, native Android, confirmed via `aapt` — no Flutter) — see
[CLAUDE.md](./CLAUDE.md)'s "Mandatory rules — Aptoide assessment target". This is the
Java/mobile counterpart to the Playwright/TypeScript framework in `Automation Testing web` —
same governing principle, same hybrid self-healing model, same **no billed API key required
anywhere** guarantee.

**Live, hosted Allure report:** https://azmdevelopment.github.io/M2-Zain-Hashmi-Mobile/ — real
results from real CI runs against the live Aptoide app, auto-published on every dispatch, not
a static snapshot.

**Scope: Android only. iOS is explicitly out of scope, not an oversight.** The team's own
test-scenario spec (`Mobile_APK_Test_Scenarios`, §10 "Known Constraints for This APK") states
it directly: *"No iOS parity — Android-only artifact — the platform-parity scenarios can't be
run here. Test iOS separately with the matching .ipa."* This repo has no `.ipa` and no iOS
driver, and never claims otherwise. The core is still typed as `AppiumDriver`, not
`AndroidDriver` (see [CLAUDE.md](./CLAUDE.md)'s "AndroidDriver vs AppiumDriver"), so adding iOS
later is a new implementation of the Android-only pieces (`AdbShellUtils`, `PermissionUtils`,
`DeepLinkUtils`, `InterferenceGuard`, `WebViewContextUtils`), not a rewrite of everything above
them — but that implementation doesn't exist, and no `PlatformActions`-style interface has been
speculatively built ahead of it. One real implementation now beats an abstraction validated
against zero.

Describe a feature in Markdown; a deterministic pipeline turns it into runnable TestNG
classes, backed by a fingerprint-based self-healing locator engine, Allure reporting, and a
CI pipeline that boots a real Android emulator and drives the real app.

## Status

The framework runs for real against the live Aptoide app — locally and in CI, not just
compiled. **3 real screens** have been crawled from the live app (onboarding, home, search),
each backing a real Markdown-authored spec that generates and passes a real TestNG test.
`SelfHealingProofTest` has produced a genuine, unforced heal event, correctly grouped in
Allure's own Categories tab. CI boots a real emulator on a GitHub-hosted runner, installs the
real Aptoide APK, and auto-publishes the merged (unit + device) report to `gh-pages`.

The team's `Mobile_APK_Test_Scenarios` spec (34 scenario IDs, S-01 through S-81) is now fully
mapped against this framework: **21 real, passing scenario tests**, **12 honest, attributed
skips** (a condition this APK/CI setup genuinely can't exercise — never a fake pass), and 1
already covered by existing infrastructure (S-81, parallel execution). See
[Scenario coverage](#scenario-coverage-capability) below and the committed
`capability/COVERAGE.md` for the full per-scenario breakdown. See
[Proof it actually works](#proof-it-actually-works) for specifics and
[What's honestly not done yet](#whats-honestly-not-done-yet) for what's still open.

## Proof it actually works

- **Live Allure report:** https://azmdevelopment.github.io/M2-Zain-Hashmi-Mobile/ — real
  `environment.properties` (`App.package=cm.aptoide.pt`, real device udid/API level, real APK
  path under `/home/runner/work/...` on the CI runner), not placeholder data.
- **A real, unforced self-heal**, visible in the report's **Categories** tab under
  *"Self-healed (locator drift, deterministic)"*: `SelfHealingProofTest` breaks a real element's
  primary locator on purpose and the 9-strategy cascade recovers it —
  `HEALED: SKIP via cascade:text-exact (confidence 1.00)`. Deterministic, not AI — no model
  call happens at runtime (see [Where AI actually is](#where-ai-actually-is-in-this-framework)).
- **3 real crawled screens**, each with a committed locator baseline and zero invented
  locators:
  - `OnboardingScreen` — the real first-run wizard, `SKIP` button (`cm.aptoide.pt:id/skip_button`)
  - `HomeScreen` — the real catalog screen, reached by tapping the real `SKIP` button
  - `SearchScreen` — the real search field, reached by a **chained** crawl (skip onboarding,
    then tap Home's real `Search` entry point) — see `PipelineRunner`'s `--afterSteps` flag
- **3 real Markdown-authored scenarios**, each generated with zero `TODO(codegen)` and passing
  against the live app: `specs/aptoide-onboarding.md`, `specs/aptoide-home.md`,
  `specs/aptoide-search.md` (the last one types into a real field and asserts the value is
  reflected back via `getText()`).
- **CI boots a real emulator and drives the real app**: `device-suite` runs on a GitHub-hosted
  `ubuntu-latest` runner via `reactivecircus/android-emulator-runner`, installs the real
  Aptoide APK (committed via Git LFS — Aptoide's own download button is JS-driven with no
  stable scrapeable URL, so a versioned local copy is the practical choice), and the run
  summary links straight to the hosted Allure report on every push.

## Scenario coverage (`capability/`)

The `Mobile_APK_Test_Scenarios` spec supplied for this assessment (34 scenarios, `S-01`–`S-81`,
priority High/Medium/Low) is the contract behind `capability/COVERAGE.md` and
`CLAUDE.md`'s `capability.*` class table. It is not reproduced in this repo (only the mapping
is), and nothing here was built against a guess of what it might contain — the classes and
tests below were written after the real document was provided.

- **8 of 12 `capability/*` classes exist**: `platform.PlatformActions` (the single audited adb
  seam every other capability class routes through), `orientation.OrientationUtils`,
  `performance.PerformanceUtils`, `network.NetworkConditionUtils`,
  `accessibility.AccessibilityUtils`, `localization.LocalizationUtils`,
  `visual.VisualRegressionUtils`, `appmgmt.AppInstallUtils`. The remaining 4
  (`notifications.NotificationUtils`, `interrupt.InterruptUtils`,
  `recording.ScreenRecorderUtils`, `api.AptoideApiClient`) don't exist yet **on purpose**: every
  scenario that would exercise them is a logged skip (no server-side control over Aptoide's
  push infra, no known API contract to build a client against), so there is no real test to
  back a class with yet — building one anyway would be untested code standing in for coverage
  that doesn't actually exist.
- **21 scenarios have real, passing tests**: S-01/02/03 (launch/lifecycle/cold-start timing),
  S-10/11/12/13/14 (deep links — see the real bug found below), S-30/31/32/33 (permission
  grant/deny, verified via dumpsys ground truth), S-40/41/42 (network conditions), S-50/51
  (orientation, background/foreground state), S-70/71/72 (accessibility, font scale, locale),
  S-80 (visual baseline diff).
- **12 scenarios are logged, attributed skips** — `SkipException` with a specific reason, never
  a fake pass, per CLAUDE.md's mandatory honesty rule: S-04 (no previous-build APK to upgrade
  from), S-15/S-20/S-21/S-22/S-23 (need locators from screens not yet crawled — app detail,
  search results, other nav tabs, an empty state), S-43 (no network-throttling primitive on a
  stock emulator), S-52 (incoming-call injection — named explicitly in CLAUDE.md's honesty rule
  as an emulator-dependent skip), S-53 (deliberately filling emulator storage risks the shared
  CI runner), S-54 (no tablet device profile configured), S-60/S-61 (no control over Aptoide's
  production push backend).
- **S-81** (parallel execution across a device pool) needs no new test — `testng.xml`'s
  `parallel="methods" thread-count="2"` across the `pixel7_api36`/`pixel7_api34` profiles is
  already that mechanism; it just hasn't been proven on two real, concurrently-booted
  emulators (only one AVD exists on the dev machine that built this).
- **S-70 is quarantined** (`testng-quarantine.xml`, non-blocking), not deleted or weakened:
  confirmed directly on a real `device-suite` CI run that its outcome varies between launches
  of the identical commit (Run 1 passed, Run 2 failed, same job) — Aptoide's home screen tiles
  are server-fetched, so which elements lack a `content-desc` isn't fully deterministic. The
  underlying finding (some home-screen elements really do lack an accessible name) is real and
  still visible in the Allure report; it just doesn't gate the pipeline on a third-party app
  defect this repo doesn't control.

Real bugs this coverage pass found and fixed, not just written around:
- `AdbShellUtils.currentFocusedActivity()` called `dumpsys window windows`, which prints nothing
  useful on a real API 34 device — `dumpsys window` (no `windows`) does. This silently broke
  both `InterferenceGuard`'s ad detection and `DeepLinkUtils.landedInApp()` on this API level;
  confirmed directly with a manual adb session before fixing it.
- `DeepLinkTest` originally guessed `https://web.aptoide.com/` — the real, `AutoVerify=true`
  host (confirmed via `dumpsys package cm.aptoide.pt`) is `app.aptoide.com`.
- A background/foreground state test raced `BaseScreen.isDisplayed()`'s immediate,
  non-waiting `findElement()` right after Appium's automatic re-foreground — a failure
  screenshot at the exact assertion showed the screen fully rendered a few hundred ms later.
  Fixed by asserting on the waited `expectVisible()` result instead.

## Setup

```powershell
git clone <this repo>
cd "Mobile Automation"
cp .env.example .env      # fill in APP_PATH if you want a fresh install rather than attaching
                           # to an already-installed build
```

### Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 21 LTS | `JAVA_HOME` set |
| Maven | 3.9+ | or use the committed `./mvnw` / `mvnw.cmd` |
| Node.js | 24.x LTS | to run the Appium server |
| Appium server | 3.x | `npm install -g appium && appium driver install uiautomator2` |
| Android SDK | — | `platform-tools`, `emulator`, `build-tools`; `ANDROID_HOME` set |
| An AVD or real device | Android 14+ (API 34+) | see below |

Appium 3 dropped `/wd/hub` — point at the bare server URL (already the default in
`config.properties`): `http://127.0.0.1:4723`. `InterferenceGuard`'s adb-shell path needs the
server started with `--allow-insecure=uiautomator2:adb_shell` (confirmed required directly —
without it, `mobile: shell` calls are refused).

### Creating an emulator

```powershell
sdkmanager "system-images;android-34;google_apis;x86_64"
avdmanager create avd -n pixel7_api34 -k "system-images;android-34;google_apis;x86_64" -d pixel_7
emulator -avd pixel7_api34
```

Device fragmentation evidence needs **two** version-distinct profiles — see
`src/test/resources/config/device-profiles.json` (`pixel7_api36` / `pixel7_api34`). Only one
AVD (`pixel7_api34`) exists on the current dev machine; `DevicePool` derives
`systemPort`/`mjpegServerPort`/`chromedriverPort` per thread automatically, so a second profile
can never collide with an existing one once a second AVD is booted.

## Running it

```powershell
.\mvnw.cmd test                                    # main suite (testng.xml) -- needs 2 emulators
.\mvnw.cmd test -DsuiteFile=testng-smoke.xml        # fast tier -- single-device-safe, use this
.\mvnw.cmd test -DsuiteFile=testng-quarantine.xml   # known-unstable, non-blocking

# override any config key: a different APK, a different device profile
.\mvnw.cmd test "-Dapp.path=C:\path\to\base.apk" -Denv=aptoide

mvn allure:serve                                    # generate + open the Allure report locally
```

### The agentic pipeline (no device needed for most of it)

```powershell
mvn exec:java -Dexec.args="md --file specs/aptoide-onboarding.md"       # generate TestNG from a spec
mvn exec:java -Dexec.args="md --file specs/aptoide-onboarding.md --dry-run"
mvn exec:java -Dexec.args="parse --file specs/aptoide-onboarding.md"    # inspect the parsed spec
mvn exec:java -Dexec.args="crawl --screen HomeScreen --afterScreen OnboardingScreen --afterAction tapSkip"
                                                                         # one real navigation hop, then crawl
mvn exec:java -Dexec.args="crawl --screen SearchScreen --afterSteps OnboardingScreen.tapSkip;HomeScreen.tapSearch"
                                                                         # a real chain of hops, then crawl
mvn exec:java -Dexec.args="impacted --base HEAD~1"                     # git diff -> testng-impacted.xml
mvn exec:java -Dexec.args="analyze"                                    # deterministic run analysis (+ optional AI narration)
mvn exec:java -Dexec.args="selfscore"                                  # score this repo against the mobile-mapped 10-criteria rubric
mvn exec:java -Dexec.args="selfscore --repo <path> --compare"          # benchmark against another repo, side by side
mvn exec:java -Dexec.args="heal-report"                                # render the heal ledger into a standalone local page (deterministic, not AI)
```

`crawl`'s `--afterScreen`/`--afterAction` (one hop) and `--afterSteps` (a chain) exist because
every crawl session starts with `noReset=false` — a fresh session always restarts at
`MainActivity`, so reaching a screen more than one real tap deep means invoking real,
already-committed navigation methods first. Neither flag can invent a navigation path: the
class and method must already exist and compile.

**Need AI?** Use Claude Code's `/gen-tests` (author/expand a spec) or `/gen-screen` (crawl a
live screen into a reviewable Screen Object) — both run inside your Claude Code session with
no API key. `ANTHROPIC_API_KEY` is read in exactly one place, `analysis/AiNarrator.java`, and
every other component works identically whether or not it's set.

## Project structure

```
src/main/java/com/azm/qa/mobile/
  core/             driver/session (ThreadLocal AppiumDriver), config, waits, gestures,
                    permissions, lifecycle, deep links, WebView context, the ad/dialog guard
  capability/       8 of 12 scenario-mapped classes (platform, orientation, performance,
                    network, accessibility, localization, visual, appmgmt) -- see
                    capability/COVERAGE.md for exactly what's covered and what's a logged skip
  healing/          the self-healing engine -- fingerprinting, the 9-strategy cascade, the
                    heal ledger. Fully deterministic, no model call, ever.
  screens/          BaseScreen + 3 real crawled screens: OnboardingScreen, HomeScreen, SearchScreen
  locators/         LocatorRepository, ScreenCrawler, LocatorDriftChecker
  spec/             MarkdownSpecParser -> SpecModel -> TestNgCodeGenerator
  data/             TestDataFactory (unique data), TestDataStore (SHA-256 dedup)
  analysis/         RunAnalyzer (deterministic), AiNarrator (opt-in), TestImpactSelector
  report/           AllureMeta, AllureEnvironment, SelfHealingReportGenerator (heal-report CLI)
  selfscore/        FrameworkScorer + 10 criteria -- scores any repo against a mobile-mapped
                    rubric, no compile/run of the target repo needed
  safety/           DestructiveKeywords
  cli/              PipelineRunner -- the `mvn exec:java` entry point

src/test/java/com/azm/qa/mobile/
  base/             BaseTest, RetryAnalyzer (set directly on @Test -- see note below)
  listeners/        TestListener (screenshots, heal/interruption marking)
  framework/        41 unit tests for the framework itself -- NO device needed
  capability/       21 real scenario tests + 12 logged-skip tests (BlockedScenariosTest etc.),
                    mapped to the team's test-scenario spec -- see Scenario coverage above
  proof/            SelfHealingProofTest -- a standing proof healing actually works
  smoke/            AppLaunchTest
  generated/        codegen output -- overwritten every run, never hand-edited
                    (AptoideOnboardingTest, AptoideHomeTest, AptoideSearchTest)

specs/              Markdown feature specs -- edit these (onboarding.md is the original,
                    pre-Aptoide placeholder, kept as an honest historical record)
apk/                the real Aptoide APK, Git-LFS-tracked, used by CI's device-suite job
.claude/commands/   gen-tests.md, gen-screen.md -- AI authoring, no API key
```

See [CLAUDE.md](./CLAUDE.md) for the full convention set: the self-healing model in detail,
why assertions never heal, the interference guard, config precedence, and the step vocabulary
`/gen-tests` writes against.

**Retry note:** `RetryAnalyzer` is set directly on every `@Test` annotation (hand-written and
codegen'd), not via a `IAnnotationTransformer` listener. Confirmed directly with a controlled
scratch test that the listener-based approach — the original design — never actually gets
honored by TestNG's invoker through Maven Surefire's TestNG provider, even though the
transformer itself runs. Retry had silently never worked, for any test, until this was fixed.

## Where AI actually is in this framework

| Component | AI? | Notes |
|---|---|---|
| Self-healing (`healing/*`) | **No** | Fingerprint scoring + a 9-strategy cascade. Deterministic, offline, sub-millisecond. |
| Codegen (`spec/*`) | **No** | Markdown → Java by string templating. Same spec always produces the same file. |
| Authoring specs/screens | **Yes, but no API key** | Claude Code's `/gen-tests` / `/gen-screen` — you (the agent) are the AI layer. |
| Run analysis (`analysis/RunAnalyzer`) | **No** | Categorizes failures, flags anomalies, correlates with heals. |
| Run narration (`analysis/AiNarrator`) | **Optional** | The ONLY class that can call a billed API, and only if `ANTHROPIC_API_KEY` is set. Everything else is unaffected by its absence. |

The rule that keeps this trustworthy: **the model never decides pass/fail.** It writes specs,
screens, and locator baselines, all reviewed like any other commit; execution is ordinary,
fast, repeatable Java.

## CI/CD (`.github/workflows/mobile-pipeline.yml`)

| Job | Trigger | What it does |
|---|---|---|
| `build-and-unit-test` | every push/PR | compiles + runs the 41 unit tests, publishes PR check results, uploads its own `allure-results/` as an artifact |
| `codegen-drift-check` | every push/PR | regenerates every spec, fails if generated code drifted from what's committed |
| `run-analysis` | every push/PR | deterministic heal/failure analysis (`analyze`) |
| `device-suite` | manual `workflow_dispatch` | boots a **real** emulator on the hosted runner (`reactivecircus/android-emulator-runner`), installs the real Aptoide APK, runs `testng-smoke.xml` then `testng-capability.xml` (the latter excludes `group=smoke`/`quarantine` so it never re-runs what the smoke tier or the quarantine lane already cover), gates on heal rate, downloads and merges in `build-and-unit-test`'s results, generates the Allure report via the CLI directly, and publishes to `gh-pages`. Confirmed on a real run: 70 test cases, 98.24% pass rate, the one genuine failure being the S-70 finding now quarantined above. |

Real bugs found and fixed while wiring this up, kept here rather than silently smoothed over:
- `simple-elf/allure-report-action`'s Docker image was broken (`openjdk:8-jre-alpine` removed
  from Docker Hub's official namespace) — replaced with the Allure CLI directly.
- `reactivecircus/android-emulator-runner` runs each line of `script:` as its own separate
  shell — a multi-line loop broke; every line is now a self-contained one-liner.
- The default `GITHUB_TOKEN` is read-only on this repo — added explicit
  `permissions: contents: write` for the `gh-pages` push.
- **Known, recurring flakiness**: GitHub-hosted emulators occasionally hit a real
  resource-pressure ANR ("System UI"/"Pixel Launcher isn't responding") — confirmed this
  happens locally too, not just on shared CI hardware. Mitigated with a bumped emulator memory
  allocation and a real, now-working retry, but not eliminated — documented here rather than
  claimed solved.

## Heal-rate gate and Slack notification (what they actually do)

Both exist and are wired into `device-suite`'s "Heal-rate policy gate" step — confirmed
directly against a real CI run, not assumed:

- **`HealPolicy`** (`healing/HealPolicy.java`, 5 unit tests) reads `self-healing-data/heal-events.jsonl`
  via `RunAnalyzer` and compares the run's heal rate against two configurable thresholds:
  `healpolicy.warnHealRate` (default 5%) and `healpolicy.maxHealRate` (default 15%). Above the
  fail threshold, `analyze` exits 1 and the CI step fails the job (unless run with
  `--warn-only`). Verified on a real run: a genuine test failure (a hosted-emulator ANR, 0
  heals) was correctly categorized as `locator-or-timing`, not miscounted as a heal, and
  reported `Heal rate 0.0% is within normal bounds` — the gate doesn't fire on an unrelated
  failure just because *something* failed.
- **Slack notification** (`Notify Slack on failure` step) is real and conditioned on
  `secrets.SLACK_WEBHOOK_URL` — confirmed via `gh secret list` that no such secret is
  currently configured on this repo, so the step correctly no-ops, same honesty pattern as
  `AiNarrator`. Add that secret to activate it; nothing else needs to change.

Still genuinely open:
- **Only one parallel-execution profile pair is defined** (`pixel7_api36`/`pixel7_api34` in
  `device-profiles.json`), and only one AVD exists on the current dev machine. `DevicePool`'s
  port isolation is proven by its own unit tests, not yet by two real, concurrently-running
  emulators — this is also why S-54 (tablet layout) and a genuinely-parallel S-81 stay unproven.
- **`TestDataStore`'s cleanup hooks have no real handlers registered yet.** The mechanism is
  built and unit-tested (`TestDataStoreCleanupTest`) — but Aptoide is a read-only black-box
  target here (no server-side data seeding, per CLAUDE.md's Honesty rule), so there is no real
  create/delete flow yet to register a handler against.
- **4 of 12 `capability/*` classes don't exist yet**, on purpose: `NotificationUtils`,
  `InterruptUtils`, `ScreenRecorderUtils`, `AptoideApiClient`. Every scenario that would back
  them is a logged skip (no server-side push control, no documented Aptoide API contract to
  build a client against) — see [Scenario coverage](#scenario-coverage-capability) above.
- **Only 3 of 34 scenarios have real screen coverage beyond onboarding/home/search** — S-15,
  S-20, S-21, S-22, S-23 are all logged skips specifically because their screens (app detail,
  search results, other nav tabs, empty state) haven't been crawled yet. All reachable with the
  same `--afterSteps` mechanism already proven for `SearchScreen`.
- **No scheduled (`schedule:`) CI trigger.** `device-suite` is manual-dispatch-only, so the one
  job that exercises a real device only runs when someone remembers to trigger it.
- **No lint/static-analysis gate and no code-coverage tooling** — `pom.xml` has no
  Checkstyle/Spotless/PMD/JaCoCo, unlike a sibling repo for the same assessment.
- **`device-suite` reliability** — the hosted-emulator ANR class of flakiness recurs
  occasionally; retry is real and does fire, but isn't a 100% guarantee on shared CI hardware.
- **Reporting completeness is still thin** outside the `AllureMeta`-annotated device scenarios
  — most of the 41 device-free unit tests don't call `AllureMeta.annotate(...)`, so their
  Allure entries carry no scenario ID/severity/description beyond the bare pass/fail.

## Framework Architecture skill mapping

| Skill (target) | Evidence |
|---|---|
| Design patterns (4) | `screens/*` + `BaseScreen`; no locators in test bodies |
| Layered architecture (3) | `core` → `screens` → `tests`; tests never touch the driver |
| Driver & session mgmt (4) | `DriverFactory` (ThreadLocal, `AppiumDriver`-typed) |
| Config & secrets (3) | `ConfigManager` precedence chain; nothing committed |
| Modularity & reusability (4) | `core/*` shared by every screen and test |
| Appium proficiency (3) | UiAutomator2 + WebView context switching |
| Mobile locator strategy (3) | strategy is app-dependent, not fixed: accessibility-id first for the original Flutter target (com.titanstudio.pro); resource-id/class-name first for the native Aptoide target (cm.aptoide.pt, confirmed via `aapt` — no Flutter, real Android views), xpath last either way |
| Gestures/permissions/deep links (3) | `GestureUtils` · `PermissionUtils` · `DeepLinkUtils` |
| App state & lifecycle (3) | `AppLifecycleUtils` |
| Device fragmentation (3) | `device-profiles.json`, thread-scoped `device.profile` |
| Parallel execution (3) | `parallel="methods"` + thread-scoped profile + `DevicePool` (per-thread `systemPort`/`mjpegServerPort`/`chromedriverPort` — no port is ever hand-picked, so a new profile can't collide with an existing one) |
| Flakiness mgmt (4) | `RetryAnalyzer` (set directly on `@Test` — see note above), `@quarantine` lane, `RunAnalyzer` flake detection, `HealPolicy` heal-rate gate |
| Waits & sync (4) | `WaitUtils`; implicit wait ZERO; no `Thread.sleep` anywhere |
| Test independence (4) | `BaseTest` — fresh driver + app state per test method, `TestDataStore` cleanup before teardown |
| Data & state mgmt (3) | `TestDataFactory` (SHA-256 fingerprinting), `TestDataStore` (dedup + registered cleanup handlers run at teardown) |
| AI test-case generation (4) | `specs/*.md` → `MarkdownSpecParser` → `TestNgCodeGenerator` |
| Self-healing locators (3) | `SelfHealingResolver` + heal ledger + `SelfHealingProofTest` + `HealPolicy` (a heal is reported as "passed-with-healing", and a run whose heal rate exceeds `healpolicy.maxHealRate` fails the `analyze` step rather than reading as a clean green) |
| Visual/anomaly detection (3) | `RunAnalyzer` anomaly flags |
| Test impact analysis (3) | `TestImpactSelector` → `testng-impacted.xml` |
| Agentic automation (3) | `.claude/commands/*`, `PipelineRunner` (`md`/`parse`/`crawl`/`impacted`/`analyze`/`selfscore`/`heal-report`), `ScreenCrawler` |
| Pipeline integration (4) | `mobile-pipeline.yml` — device-free jobs on every push + a real hosted-emulator dispatch lane; run summary links the hosted Allure report |
| Trigger strategy (3) | smoke / regression / quarantine / impacted suites |
| Quality gates (3) | `codegen-drift-check` + unit tests gate the build |
| Reporting & observability (3) | Allure + `AllureMeta`, heal ledger, run analysis, real self-healed tests grouped in Allure's native Categories tab, unit + device results merged into one published report |
| Self-evaluation / benchmarking | `selfscore` — scores this repo (or any other) against a mobile-mapped 10-criteria rubric, no compile/run needed; run `mvn exec:java -Dexec.args="selfscore"` |
