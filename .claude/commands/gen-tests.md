---
description: Author or expand a Markdown feature spec in specs/, then generate and run the TestNG tests from it
---

# Generate mobile tests from a feature description

You are acting as the test-authoring agent for this framework. The user's request follows.
**No Anthropic API key is configured in this project by design** — you ARE the AI layer, so
reason about the app yourself rather than looking for a service that calls a model.

## User request

$ARGUMENTS

## What to do

1. **Read `CLAUDE.md` first** for the project conventions, then read an existing spec
   (`specs/onboarding.md`) to match its style, and
   `src/main/java/com/azm/qa/mobile/spec/StepVocabulary.java` to see exactly which step
   phrasings compile to real code versus a `// TODO(codegen)`.

2. **Work out what the user wants.**
   - Named a flow with no spec yet → author a new `specs/<feature>.md`.
   - Pointed at an existing spec → extend it with the coverage they asked for.
   - Asked to "expand" a spec → add the negative/edge scenarios the author didn't write:
     permission denial, a killed/backgrounded app mid-flow, a deep link to a missing resource,
     an ad interstitial mid-interaction, rotation. Only add what's actually plausible for
     THIS app, not a generic checklist.

3. **Write the spec in the documented format**: metadata block (`key`, `screen`, `priority`),
   `## Description`, `## Acceptance Criteria`, then `## Scenarios` with
   `### [POSITIVE|NEGATIVE|EDGE] <title>` headings.

4. **Phrase steps so they compile.** Use the vocabulary `StepVocabulary` understands:
   - `launch app` / `relaunch app` / `background app 5s`
   - `` tap `accessibility:Label` `` (or `id:`, `text:`, `xpath:`, `uiautomator:`)
   - `` type <value> into `accessibility:Label` ``
   - `swipe up|down|left|right` · `` scroll to `accessibility:Label` ``
   - `grant permission <name>` / `revoke permission <name>`
   - `open deep link <uri>`
   - `rotate portrait|landscape`
   - `` expect `accessibility:Label` visible|hidden ``
   - `` expect `accessibility:Label` contains <text> ``
   - `switch to webview` / `switch to native`
   - `wait for app ready`
   - `` `rawJavaStatement();` `` as an escape hatch for anything else

   **Never invent a locator you have not verified.** Either:
   - crawl the real screen first — `mvn exec:java -Dexec.args="crawl --screen <Name>"` (needs
     a live emulator/device with the app open on that screen), which writes
     `locators/<Name>.json`, then use the accessibility ids/text it actually found; or
   - leave the target as plain prose with no backticks, so it compiles to an honest
     `// TODO(codegen)` instead of a guess.

5. **Generate and verify:**
   ```
   mvn exec:java -Dexec.args="md --file specs/<feature>.md"
   ```
   Report how many steps came out as `TODO(codegen)` and fix the phrasing of any that should
   have compiled. Then, only if the user asked you to actually run them against a device:
   ```
   mvnw test -Dtest=<GeneratedClassName>
   ```

6. **Report honestly.** Say which scenarios you added and why, which steps still need a human
   (a real crawl, a decision about test data), and whether the generated tests actually passed.
   If a test fails because the app is genuinely broken, say so — never weaken the assertion to
   make it green.

## Hard rules

- Never put real credentials in a spec. This app's flows covered so far need no authenticated
  session; if a future spec needs one, it reads from `ConfigManager`/an env var, never a
  hardcoded value in the spec or generated test.
- Never add a step that performs a destructive action (logout, uninstall, clear app data,
  revoke all permissions — see `safety/DestructiveKeywords.java`) unless the user explicitly
  asked for that scenario.
- Generated files under `src/test/java/.../generated/` are overwritten on every run.
  Hand-written additions belong in the source spec or a separate, non-generated class.
