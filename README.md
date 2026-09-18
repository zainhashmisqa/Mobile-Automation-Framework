# AZM Agentic Android Automation Framework

Appium + TestNG **Android** automation framework, built for the AZM QA → QE transformation
programme's AI-Augmented Automation milestone. Originally scaffolded against
**com.titanstudio.pro** (Flutter); the live assessment target is **Aptoide** (`cm.aptoide.pt`
v9.22.5.3, native Android, confirmed via `aapt` — no Flutter) — see CLAUDE.md's "Mandatory
rules — Aptoide assessment target". This is the Java/mobile counterpart to the
Playwright/TypeScript framework in `Automation Testing web` — same governing principle, same
hybrid self-healing model, same **no billed API key required anywhere** guarantee.

**Live, hosted Allure report:** https://azmdevelopment.github.io/M2-Zain-Hashmi-Mobile/ — real
results from real runs against the live Aptoide app, including a genuine self-healing recovery
(see [Proof it actually works](#proof-it-actually-works)).

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

The framework has run for real against the live Aptoide app on a local emulator, not just
compiled: `AppLaunchTest`, a real crawled-and-generated `AptoideOnboardingTest`, and
`SelfHealingProofTest` (which produced a genuine, unforced heal event) all pass. 37 real test
cases total (34 device-free unit tests + 3 real device tests) are published in the
[hosted Allure report](https://azmdevelopment.github.io/M2-Zain-Hashmi-Mobile/), with the
healed test correctly grouped in Allure's own Categories tab. See
[Proof it actually works](#proof-it-actually-works) for the specifics, and
[What's honestly not done yet](#whats-honestly-not-done-yet) for what's still open.

## Proof it actually works

- **Live Allure report:** https://azmdevelopment.github.io/M2-Zain-Hashmi-Mobile/ — 37 test
  cases, 100% pass, real `environment.properties` (`App.package=cm.aptoide.pt`,
  `Device.udid=emulator-5554`, real API level), not placeholder data.
- **A real, unforced self-heal**, visible in the report's **Categories** tab under
  *"Self-healed (locator drift, deterministic)"*: `SelfHealingProofTest` breaks a real element's
  primary locator on purpose and the 9-strategy cascade recovers it —
  `HEALED: SKIP via cascade:text-exact (confidence 1.00)`. Deterministic, not AI — no model call
  happens at runtime (see [Where AI actually is](#where-ai-actually-is-in-this-framework)).
- **A real crawled screen**: `screens/OnboardingScreen.java` and
  `src/test/resources/locators/OnboardingScreen.json` come from actually crawling Aptoide's live
  onboarding wizard — the `SKIP` button, resource-id `cm.aptoide.pt:id/skip_button`, nothing
  guessed.
- **A real Markdown-authored scenario**: `specs/aptoide-onboarding.md` →
  `AptoideOnboardingTest` (generated, zero `TODO(codegen)`) → passes against the live app.
- **CI is green** on every push (`build-and-unit-test`, `codegen-drift-check`, `run-analysis`),
  and the run summary links straight to the hosted Allure report.

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
mvn exec:java -Dexec.args="selfscore"                               # score this repo against the mobile-mapped 10-criteria rubric
mvn exec:java -Dexec.args="selfscore --repo <path> --compare"       # benchmark against another repo, side by side
mvn exec:java -Dexec.args="heal-report"                             # render the heal ledger into a standalone local page (deterministic, not AI)
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
  report/           AllureMeta, AllureEnvironment, SelfHealingReportGenerator (heal-report CLI)
  selfscore/        FrameworkScorer + 10 criteria -- scores any repo against a mobile-mapped
                    rubric, no compile/run of the target repo needed
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

Closed since the framework was first scaffolded (kept here so the history is honest, not
deleted and forgotten):
- ~~The device suite hasn't run against the real app~~ — it now has, repeatedly, against real
  Aptoide on a local emulator. See [Proof it actually works](#proof-it-actually-works).
- ~~No screen objects exist for this app's real UI~~ — `OnboardingScreen` is real, crawled from
  the live app, committed with its baseline.

Still genuinely open:
- **Only one parallel-execution profile pair is defined** (`pixel7_api36`/`pixel7_api34` in
  `device-profiles.json`), and only one AVD (`pixel7_api34`) exists on the current dev machine.
  `DevicePool` guarantees the two won't collide on ports if run concurrently, but that guarantee
  is proven by the port-derivation logic itself (and its unit tests), not yet by two real,
  concurrently-running emulators — that still needs a second AVD booted at once. Deliberately
  scoped out of the current pass (the assessment's real-device-run gap mattered more than this
  one).
- **`TestDataStore`'s cleanup hooks have no real handlers registered yet.** The mechanism is
  built and unit-tested (`TestDataStoreCleanupTest`) — a screen can call
  `TestDataStore.registerCleanupHandler("kind", ...)` — but Aptoide is a read-only black-box
  target here (no server-side data seeding, per CLAUDE.md's Honesty rule), so there is no real
  create/delete flow yet to register a handler against.
- **The device-suite CI lane is untested** (`workflow_dispatch` + `self-hosted` in
  `mobile-pipeline.yml`) — its Allure-generation and GitHub Pages-publish steps have never
  executed once; no self-hosted runner has ever been registered against this repo. The
  currently-hosted Allure report (see above) was published manually, once, from a real local
  run — it is **not** regenerated automatically by CI yet.
- **Reporting completeness is still thin** (`selfscore`'s own honest read: ~2.7/10 on this
  axis) — most tests don't call `AllureMeta.annotate(...)`, so their Allure entries carry no
  scenario ID/severity/description beyond the bare pass/fail.

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
| Pipeline integration (4) | `mobile-pipeline.yml` — device-free jobs + dispatch lane; run summary links the hosted Allure report on every push |
| Trigger strategy (3) | smoke / regression / quarantine / impacted suites |
| Quality gates (3) | `codegen-drift-check` + unit tests gate the build |
| Reporting & observability (3) | Allure + `AllureMeta`, heal ledger, run analysis, real self-healed tests grouped in Allure's native Categories tab |
| Self-evaluation / benchmarking | `selfscore` — scores this repo (or any other) against a mobile-mapped 10-criteria rubric, no compile/run needed; run `mvn exec:java -Dexec.args="selfscore"` |
