# AZM Agentic Android Automation Framework

Appium + TestNG **Android** automation framework for **com.titanstudio.pro** (Flutter). This
is the Java/mobile counterpart to the Playwright/TypeScript framework in
`Automation Testing web` — same governing principle, same hybrid self-healing model, same
**no billed API key required anywhere** guarantee, built for the AZM QA → QE transformation
programme's AI-Augmented Automation milestone.

**Scope: Android only.** No iOS driver exists or is planned in this assignment. The core is
still typed as `AppiumDriver`, not `AndroidDriver` (see [CLAUDE.md](./CLAUDE.md)'s "AndroidDriver
vs AppiumDriver"), so adding iOS later is a new implementation of the Android-only pieces
(`AdbShellUtils`, `PermissionUtils`, `DeepLinkUtils`, `InterferenceGuard`,
`WebViewContextUtils`), not a rewrite of everything above them — but that implementation
doesn't exist, and no `PlatformActions`-style interface has been speculatively built ahead of
it. One real implementation now beats an abstraction validated against zero.

Describe a feature in Markdown; a deterministic pipeline turns it into runnable TestNG
classes, backed by a fingerprint-based self-healing locator engine, Allure reporting, and a
device-free CI pipeline that still proves real value with no emulator attached.

## Status

The framework skeleton compiles cleanly end to end — core driver/session management, the
self-healing engine, the Markdown → TestNG codegen pipeline, run analysis, and the device-free
unit tests that prove all of it, with no emulator required. See
[Prerequisites](#prerequisites) for what's still needed to run the *device* suite
(`AppLaunchTest`, `SelfHealingProofTest`) against a real emulator, and
[What's honestly not done yet](#whats-honestly-not-done-yet) for the rest.

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
`config.properties`): `http://127.0.0.1:4723`.

### Creating an emulator

```powershell
sdkmanager "system-images;android-34;google_apis;x86_64"
avdmanager create avd -n pixel7_api34 -k "system-images;android-34;google_apis;x86_64" -d pixel_7
emulator -avd pixel7_api34
```

Device fragmentation evidence needs **two** version-distinct profiles — see
`src/test/resources/config/device-profiles.json` (`pixel7_api36` / `pixel7_api34`). Create a
second AVD on a different API level and give it its own `udid` entry there — you don't need to
pick a port; `DevicePool` derives `systemPort`/`mjpegServerPort`/`chromedriverPort` per thread
automatically, so a new profile can never collide with an existing one.

## Running it

```powershell
.\mvnw.cmd test                                    # main suite (testng.xml)
.\mvnw.cmd test -DsuiteFile=testng-smoke.xml        # fast tier
.\mvnw.cmd test -DsuiteFile=testng-quarantine.xml   # known-unstable, non-blocking

# override any config key: a different APK, a different device profile
.\mvnw.cmd test "-Dapp.path=C:\path\to\base.apk" "-Ddevice.profile=pixel7_api34"

mvn allure:serve                                    # generate + open the Allure report
```

### The agentic pipeline (no device needed for most of it)

```powershell
mvn exec:java -Dexec.args="md --file specs/onboarding.md"          # generate TestNG from a spec
mvn exec:java -Dexec.args="md --file specs/onboarding.md --dry-run"
mvn exec:java -Dexec.args="parse --file specs/onboarding.md"        # inspect the parsed spec
mvn exec:java -Dexec.args="crawl --screen OnboardingScreen"         # needs a live device -- crawls the current screen
mvn exec:java -Dexec.args="impacted --base HEAD~1"                  # git diff -> testng-impacted.xml
mvn exec:java -Dexec.args="analyze"                                 # deterministic run analysis (+ optional AI narration)
```

**Need AI?** Use Claude Code's `/gen-tests` (author/expand a spec) or `/gen-screen` (crawl a
live screen into a reviewable Screen Object) — both run inside your Claude Code session with
no API key. `ANTHROPIC_API_KEY` is read in exactly one place, `analysis/AiNarrator.java`, and
every other component works identically whether or not it's set.

## Project structure

```
src/main/java/com/azm/qa/mobile/
  core/             driver/session (ThreadLocal AppiumDriver), config, waits, gestures,
                    permissions, lifecycle, deep links, WebView context, the ad/dialog guard
  healing/          the self-healing engine -- fingerprinting, the 9-strategy cascade, the
                    heal ledger. Fully deterministic, no model call, ever.
  screens/          BaseScreen (self-healing actions, raw-locator assertions)
  locators/         LocatorRepository, ScreenCrawler, LocatorDriftChecker
  spec/             MarkdownSpecParser -> SpecModel -> TestNgCodeGenerator
  data/             TestDataFactory (unique data), TestDataStore (SHA-256 dedup)
  analysis/         RunAnalyzer (deterministic), AiNarrator (opt-in), TestImpactSelector
  report/           AllureMeta
  safety/           DestructiveKeywords
  cli/              PipelineRunner -- the `mvn exec:java` entry point

src/test/java/com/azm/qa/mobile/
  base/             BaseTest, RetryAnalyzer, RetryTransformer
  listeners/        TestListener (screenshots, heal/interruption marking)
  framework/        unit tests for the framework itself -- NO device needed
  proof/            SelfHealingProofTest -- a standing proof healing actually works
  smoke/            AppLaunchTest
  generated/        codegen output -- overwritten every run, never hand-edited

specs/              Markdown feature specs -- edit these
locator-baselines/  committed drift-check baselines
.claude/commands/   gen-tests.md, gen-screen.md -- AI authoring, no API key
```

See [CLAUDE.md](./CLAUDE.md) for the full convention set: the self-healing model in detail,
why assertions never heal, the interference guard, config precedence, and the step vocabulary
`/gen-tests` writes against.

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

## What's honestly not done yet

- **The device suite hasn't run against the real app in this environment.** The framework
  compiles cleanly and its device-free unit tests pass; `AppLaunchTest` and
  `SelfHealingProofTest` need a live emulator/device with `com.titanstudio.pro` installed,
  which this development machine did not have fully set up (Android SDK system image + AVD)
  until late in this build. Run them yourself once your emulator is up — see
  [Prerequisites](#prerequisites) — and treat the first run as also validating the app's real
  Semantics-label coverage (see `AppLaunchTest#dumpPageSourceForLocatorDiscovery`).
- **No screen objects exist for this app's real UI yet.** Nothing in `screens/` claims to know
  a real accessibility id, resource-id, or text for com.titanstudio.pro — every locator in this
  repo is either a framework-internal detail or a placeholder proof, on purpose (see
  CLAUDE.md's "never invent a locator" rule). The first real screen comes from running the
  `crawl` command or `/gen-screen` against the live app.
- **Only one parallel-execution profile pair is defined** (`pixel7_api36`/`pixel7_api34` in
  `device-profiles.json`). `DevicePool` guarantees the two won't collide on ports if run
  concurrently, but that guarantee is proven by the port-derivation logic itself (and its unit
  tests), not yet by two real, concurrently-running emulators — that still needs two AVDs
  booted at once.
- **`TestDataStore`'s cleanup hooks have no real handlers registered yet.** The mechanism is
  built and unit-tested (`TestDataStoreCleanupTest`) — a screen can call
  `TestDataStore.registerCleanupHandler("kind", ...)` once it exists — but no screen has been
  crawled for this app yet, so there is nothing to register a real deletion flow against.
- **The device-suite CI lane is untested** (`workflow_dispatch` + `self-hosted` in
  `mobile-pipeline.yml`) — it's written to the same shape as the reference repo's working CI,
  but no self-hosted runner has actually executed it.

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
| Flakiness mgmt (4) | `RetryAnalyzer`, `@quarantine` lane, `RunAnalyzer` flake detection, `HealPolicy` heal-rate gate |
| Waits & sync (4) | `WaitUtils`; implicit wait ZERO; no `Thread.sleep` anywhere |
| Test independence (4) | `BaseTest` — fresh driver + app state per test method, `TestDataStore` cleanup before teardown |
| Data & state mgmt (3) | `TestDataFactory` (SHA-256 fingerprinting), `TestDataStore` (dedup + registered cleanup handlers run at teardown) |
| AI test-case generation (4) | `specs/*.md` → `MarkdownSpecParser` → `TestNgCodeGenerator` |
| Self-healing locators (3) | `SelfHealingResolver` + heal ledger + `SelfHealingProofTest` + `HealPolicy` (a heal is reported as "passed-with-healing", and a run whose heal rate exceeds `healpolicy.maxHealRate` fails the `analyze` step rather than reading as a clean green) |
| Visual/anomaly detection (3) | `RunAnalyzer` anomaly flags |
| Test impact analysis (3) | `TestImpactSelector` → `testng-impacted.xml` |
| Agentic automation (3) | `.claude/commands/*`, `PipelineRunner`, `ScreenCrawler` |
| Pipeline integration (4) | `mobile-pipeline.yml` — device-free jobs + dispatch lane |
| Trigger strategy (3) | smoke / regression / quarantine / impacted suites |
| Quality gates (3) | `codegen-drift-check` + unit tests gate the build |
| Reporting & observability (3) | Allure + `AllureMeta`, heal ledger, run analysis |
