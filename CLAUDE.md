# AZM Android Automation Framework — AI Agent Architecture

Appium + TestNG **Android** automation framework for **com.titanstudio.pro**. This is the
Java/mobile counterpart to the Playwright/TypeScript framework in `Automation Testing web` —
same governing principle, same hybrid self-healing model, same "no billed API key anywhere"
guarantee, ported to Java where mobile needs its own tools (a real driver/session factory,
gestures, permissions, deep links, device fragmentation).

**Android only, on purpose.** No iOS driver exists or is planned in this assignment. Every
core class is typed as `AppiumDriver`, never `AndroidDriver` — see "AndroidDriver vs
AppiumDriver" below — so an iOS implementation of the Android-only pieces is additive later.

**`capability.platform.PlatformActions`** (mandated below) is a *different* thing from a
multi-platform abstraction: it's a single facade in front of `AdbShellUtils` so every OS-level
effect goes through one audited seam, not a second implementation to guess an iOS contract
against. It stays Android-only underneath; nothing about it commits to iOS ever existing.

**No Anthropic API key is required anywhere.** Self-healing is fully deterministic
(fingerprint scoring + a 9-strategy cascade, no model call). Code generation
(`MarkdownSpecParser` → `TestNgCodeGenerator`) is template-based and fully offline.
AI-assisted spec authoring runs through Claude Code's `/gen-tests` and `/gen-screen` slash
commands — you (the agent) *are* the AI layer; there is no billed call to look for.

**Core principle: agentic at authoring time, deterministic at execution time.** The model
writes specs, screens and locator baselines. It never decides pass/fail, and it never runs in
the execution path CI actually gates on.

## Layout

- `core/` — `driver/` (DriverFactory, CapabilitiesBuilder), `config/` (ConfigManager,
  DeviceProfile), `waits/` (WaitUtils, AppReadyWaits), `gestures/`, `permissions/`,
  `lifecycle/`, `deeplink/`, `context/` (WebView switching), `guard/` (InterferenceGuard +
  InterruptionReporter), `util/` (AdbShellUtils, ScreenshotUtils)
- `healing/` — the self-healing engine: `SelfHealingResolver` (chain → cascade → fail loudly),
  `LocatorCandidate`, `ElementFingerprint` (scoring), `StrategyCascade` (9 strategies),
  `PageSourceParser`/`PageSourceNode` (pure XML tree, no driver dependency), `HealStore` +
  `HealReporter` (ledger + Allure attachment), `CurrentTest` (thread-scoped test identity)
- `screens/` — `BaseScreen` (self-healing actions, raw-locator assertions, interference guard,
  auto-bootstrap of locator baselines) and screen objects that extend it
- `locators/` — `LocatorRepository` (JSON-backed fingerprint baselines), `ScreenCrawler` (pure
  page-source crawl, no AI), `LocatorDriftChecker`
- `spec/` — `MarkdownSpecParser`, `SpecModel`, `StepVocabulary`, `TargetSpec`,
  `TestNgCodeGenerator` — the Markdown → TestNG pipeline
- `data/` — `TestDataFactory` (unique data per call), `TestDataStore` (SHA-256 dedup)
- `analysis/` — `RunAnalyzer` (deterministic), `AiNarrator` (no-ops without
  `ANTHROPIC_API_KEY` — the ONLY place a billed call can happen), `TestImpactSelector`
  (git diff → `testng-impacted.xml`)
- `report/` — `AllureMeta`
- `safety/` — `DestructiveKeywords`
- `cli/` — `PipelineRunner`, the `mvn exec:java -Dexec.args="..."` entry point
- `.claude/commands/` — `gen-tests.md`, `gen-screen.md` (AI authoring, no API key)
- `specs/` — hand-written Markdown feature specs (edit these)
- `locator-baselines/` — committed drift-check baselines
- `.github/workflows/mobile-pipeline.yml` — device-free CI (build/lint/unit/drift/analysis)

## Conventions

### Layering is not optional
`core → screens → tests`. A test never touches a raw `By` or the driver directly; a screen
never constructs its own driver; `DriverFactory`/`CapabilitiesBuilder` are the only place a
session is built.

### AndroidDriver vs AppiumDriver
Every signature in `core`/`screens` is typed as `AppiumDriver`, never `AndroidDriver`. The
handful of places that genuinely need Android-only behaviour cast locally
(`(AndroidDriver) driver`). This is what keeps an iOS driver a future subclass instead of a
rewrite — don't concrete-type it back.

### Self-healing: actions heal, assertions don't
```java
// Preferred — an author-declared, ranked fallback chain. Healing can only land on a
// selector a human wrote down; anything past index 0 is reported as a heal.
screen.tap(List.of(
    LocatorCandidate.accessibilityId("Skip"),
    LocatorCandidate.resourceId("com.titanstudio.pro:id/skip_button")
), "skip button");

// Fallback — a single locator; the heuristic cascade searches the live page source by
// fingerprint if it breaks. Needs a stored baseline in locators/<screen>.json to heal onto.
screen.tap(AppiumBy.accessibilityId("Skip"), "skip button");
```
**Assertions never go through the healer.** `expectVisible`/`expectHidden` take the raw `By`
with an ordinary explicit wait. Healing answers "this locator is broken"; an assertion waiting
for something to render can't tell that apart from "hasn't appeared yet", and healing onto a
looser locator could make an assertion pass against the wrong element.

**A heal is never silent.** Every successful heal: (1) writes an Allure attachment with
old strategy → new strategy → confidence, (2) appends a line to
`self-healing-data/heal-events.jsonl`, (3) marks the TestNG/Allure result `healed=true` so it
never reads as a plain green pass. Review healed locators in the report; a heal is a signal,
not a fix.

### Locator baselines are seeded, then committed
The first time a screen resolves a description with no stored fingerprint, `BaseScreen`
bootstraps one into `locators/<screen>.json` automatically. Treat that file like any other
test asset: review it, commit it, and prefer running `/gen-screen` or
`mvn exec:java -Dexec.args="crawl --screen X"` deliberately over letting bootstrap be the only
source of truth for an important screen.

### Never invent a locator
`TargetSpec`/`StepVocabulary` refuse to guess. A step's target resolves from a backtick
locator (`` `accessibility:Skip` ``, `` `id:resource_name` ``, `` `text:...` ``,
`` `xpath:...` ``) or it becomes an explicit `// TODO(codegen)` — never a fabricated
accessibility id built from prose. Fill the TODO by crawling the real screen, not by guessing.

### The interference guard
com.titanstudio.pro ships Google Mobile Ads and Play Core; either can steal the foreground
mid-test. `BaseScreen` checks and dismisses before every action via `InterferenceGuard`, and
every dismissal is recorded through `InterruptionReporter` (thread-scoped — a screen object is
recreated often, so the counter can't live on the guard instance) and surfaces in Allure as
`interrupted=true`. An interstitial firing on every run is a product finding, not noise to
swallow.

### No `Thread.sleep`, anywhere, including for a timed diagnostic
Every wait is explicit and named. Where a genuinely time-based pause is needed (e.g. sampling
page source at fixed intervals to catch Flutter's splash → first-frame transition), use
`WaitUtils#pauseFor` — built on `FluentWait`, not a raw sleep — so this claim stays literally
true of the whole codebase, not just the parts that happen to have a real condition to wait on.

### Config & secrets
`ConfigManager`'s precedence: thread override → system property → env var →
`config-{env}.properties` → `config.properties`. Nothing machine-specific or secret is
committed; `app.path` and `config-local.properties` are both git-ignored.

### Parallelism needs thread-scoped device profile
`BaseTest` calls `ConfigManager.setThreadOverride("device.profile", ...)` from the `device`
TestNG parameter before creating a driver. This is what makes `parallel="methods"` with more
than one device profile safe — a process-wide profile would race across threads.

### Tags / groups
`smoke` (CRITICAL/HIGH positives — fast tier), `regression` (always applied), `negative`
(NEGATIVE/EDGE scenarios), `quarantine` (known-unstable, its own non-blocking suite file —
remove the tag once fixed, don't leave it to rot).

### Reporting
Every generated (and hand-written) test opens with `AllureMeta.annotate(...)` supplying
`testCaseId`/`feature`/`story`/`severity`/`description`.

## Commands

```
mvnw test                                              # main suite (testng.xml)
mvnw test -DsuiteFile=testng-smoke.xml
mvnw test -DsuiteFile=testng-quarantine.xml
mvnw test -DsuiteFile=testng-impacted.xml              # after running `impacted` below

mvn exec:java -Dexec.args="md --file specs/onboarding.md"
mvn exec:java -Dexec.args="md --file specs/onboarding.md --dry-run"
mvn exec:java -Dexec.args="parse --file specs/onboarding.md"
mvn exec:java -Dexec.args="crawl --screen OnboardingScreen"   # needs a live device/emulator
mvn exec:java -Dexec.args="impacted --base HEAD~1"
mvn exec:java -Dexec.args="analyze"                            # + AI narration iff ANTHROPIC_API_KEY is set

mvn allure:serve                                       # generate + open the Allure report
```

## Markdown-driven test authoring

Same shape as the web framework's spec format — see `specs/onboarding.md` for a real,
committed example, and `spec/StepVocabulary.java` for the exact phrasings that compile.
`MarkdownSpecParser` → `TestNgCodeGenerator` is fully deterministic: same spec in, same Java
out, always — that's what `codegen-drift-check` in CI depends on.

**Need AI?** Use `/gen-tests` to author or expand a spec, or `/gen-screen` to crawl a live
screen and turn the result into a reviewable Screen Object. Both run inside this Claude Code
session — no API key, and nothing else in the framework can call a billed one.

Generated files land in `src/test/java/.../generated/` and are overwritten on every run.
Hand-written additions belong in the source spec or a separate, non-generated class.

## Extending this base

New screens go in `screens/`, extending `BaseScreen`. New specs go in `specs/`. Update
`spec/StepVocabulary.java` and `.claude/commands/gen-tests.md` together whenever a convention
here changes, or generated code will drift from what `/gen-tests` teaches Claude to write.

---

# Mandatory rules — Aptoide assessment target

The rules below are **non-negotiable** for this repo, composing with (not replacing) every
convention above. Merged in from a governance doc supplied for the Aptoide assessment; several
referenced pieces (`capability/*`, `Mobile_APK_Test_Scenarios.docx`'s scenario IDs, the Aptoide
API contract) do not exist in this repo yet — see README's "What's honestly not done yet" for
what's still missing and why nothing was fabricated to fill the gap in the meantime.

## 0. App under test

- Assessment target: **Aptoide — `cm.aptoide.pt` v9.22.5.3** (native Android, black-box via
  Appium/UIAutomator2).
- **Never edit** the committed `config.properties` default (`com.titanstudio.pro`). Point runs
  at Aptoide via `config-aptoide.properties` or `-Dapp.package=cm.aptoide.pt` —
  `ConfigManager`'s existing precedence chain already supports this with no framework change.

## 1. Framework conventions the agent must honour

- **Never** write `Thread.sleep`. Every wait goes through `core.waits.WaitUtils` (or
  `AppReadyWaits`).
- **Never** call `adb` directly. All shell/OS effects go through
  `capability.platform.PlatformActions`, which delegates to `core.util.AdbShellUtils`.
- **Never** build capabilities anywhere but `core.driver.CapabilitiesBuilder`.
- **Driver** is always obtained from `core.driver.DriverFactory.getDriver()` — thread-local,
  one session per thread. Never cache it across threads.
- **Self-healing applies to actions only** (`tap`/`type`). Assertions
  (`expectVisible`/`expectHidden`) stay raw — never heal an assertion.
- **Runtime is deterministic.** Never add a model/LLM call in the execution path. AI stays at
  authoring time (`.claude/commands/*`) and post-run analysis (`analysis.AiNarrator`, which
  no-ops without a key).
- **Never invent a locator.** Locators come from `LocatorRepository` / spec backticks only. If
  one is missing, emit `TODO(codegen)` — never guess.
- **Every meaningful state change attaches to Allure.** No silent steps.
- Respect `safety.DestructiveKeywords` — flag logout/delete/uninstall flows.

## 2. The capability layer — use it, don't reinvent it

When new work touches one of these concerns, use the listed class rather than writing a new
helper for something already covered. **None of the classes below exist yet** — this table is
the contract for whoever (human or agent) builds each one; check before assuming it's there.

| Concern | Class | Baseline item / scenarios |
|---|---|---|
| Network / offline / throttle | `capability.network.NetworkConditionUtils` | 11 · S-40–43 |
| Cold/warm start, memory | `capability.performance.PerformanceUtils` | 22 · S-01, S-03 |
| Rotate / responsive | `capability.orientation.OrientationUtils` | 25, 9 · S-50, S-54 |
| Notifications / background | `capability.notifications.NotificationUtils` | 13 · S-60, S-61 |
| Install / upgrade / low-storage | `capability.appmgmt.AppInstallUtils` | 13, 19 · S-04, S-53 |
| TalkBack / font scale / targets | `capability.accessibility.AccessibilityUtils` | 23 · S-70, S-71 |
| Locale / RTL | `capability.localization.LocalizationUtils` | 24 · S-72 |
| Screenshot diff | `capability.visual.VisualRegressionUtils` | 26 · S-80 |
| Video on failure | `capability.recording.ScreenRecorderUtils` | 21 |
| Interrupt injection | `capability.interrupt.InterruptUtils` | 9 · S-51, S-52 |
| OS-level effects (adb) | `capability.platform.PlatformActions` | 5 |
| External API oracle/seed | `capability.api.AptoideApiClient` | 27 · S-20, S-22 |

## 3. Mandatory coverage & traceability

- Every generated test must map to a scenario ID (`S-01 … S-81`) from
  `Mobile_APK_Test_Scenarios.docx` and, through it, to a baseline checklist item. Put the ID in
  the test name or an Allure label so coverage stays traceable. **This document has not been
  provided to this repo yet — no scenario IDs have been assigned or invented.**
- Build order follows scenario priority: High first, then Medium, then Low.
- Keep `capability/COVERAGE.md` accurate — add a row whenever a capability is added.

## 4. Honesty rule — never fake a pass

Some conditions can't be exercised against this APK. Make the test **log a clear, attributed
skip** — never a false pass — for:

- **iOS parity** — no `.ipa`; `PlatformActions`' notional iOS counterpart is out of scope (see
  the Android-only decision above).
- **Server-side data seeding** — Aptoide's production backend; any API client here is
  read-only. Use idempotent/read-only flows or a mock backend.
- **Locale change on non-root real devices**, **network throttling**, and **incoming-call
  injection** — emulator/farm-dependent; skip with a reason if the device refuses.
- **Secure-coding validation** on a third-party binary — static/behavioural observation only.

A green test that didn't run its condition is a defect, not a pass.

## 5. Side-effect gate

Read-only investigation, analysis, and generating/editing local files are fine without asking.
**Before any of the following, ask for explicit approval and wait for a clear "yes"**: `git
push`, `git commit`, opening/editing PRs or Jira issues, posting comments, moving sprint items,
sending messages, or a real-device dispatch that costs farm time. Propose the diff/plan first.

## 6. Definition of done for a new/edited test

- [ ] Maps to a scenario ID; ID visible in test name or Allure label.
- [ ] Uses the capability-layer class for its concern (no reinvented helper).
- [ ] No `Thread.sleep`; waits via `WaitUtils`.
- [ ] No direct `adb`; OS effects via `PlatformActions`.
- [ ] Assertions are raw (not self-healing).
- [ ] Unsupported condition → logged skip, not a fake pass.
- [ ] Allure step/attachment on each state change.
- [ ] Flaky/device-state-heavy cases parked in `testng-quarantine.xml` until proven on the farm.
- [ ] `capability/COVERAGE.md` updated if a capability was added.
