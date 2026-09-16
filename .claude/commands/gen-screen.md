---
description: Crawl a live screen and turn what's found into a reviewable Screen Object and locator baseline
---

# Crawl a screen and generate a Screen Object

You are acting as the screen-mapping agent. The user's request follows.

## User request

$ARGUMENTS

## What to do

1. **Confirm a device/emulator is attached and Appium is running.** If not, tell the user what
   to start (`appium` server, `emulator -avd <name>`) rather than guessing it's already there.

2. **Get the app onto the screen you're mapping.** If the user hasn't already navigated there,
   either ask them to, or drive it yourself with existing, already-verified screens/tests if
   one gets you there.

3. **Crawl it**:
   ```
   mvn exec:java -Dexec.args="crawl --screen <ScreenName>"
   ```
   This writes/updates `locators/<ScreenName>.json` with every interactive, uniquely
   describable element the crawler found — no AI, no guessing, straight off the live page
   source.

4. **Read the result** and turn it into a real `screens/<ScreenName>Screen.java` extending
   `BaseScreen` (see any existing screen for the shape): one method per meaningful action
   (e.g. `tapSkip()`, `isWelcomeMessageVisible()`), each built on `tap(...)`/`type(...)`/
   `expectVisible(...)` with the exact locator the crawl found — reference the fingerprint's
   `contentDesc`/`resourceId` field directly via `LocatorCandidate.accessibilityId(...)` etc.,
   never a value you're inferring or remembering from elsewhere.

5. **Flag anything the crawl found that has no stable description at all** (empty
   content-desc, empty text, empty resource-id) — that's a real gap in the app's Semantics
   labelling, worth raising with the dev team rather than working around with a brittle
   coordinate-based or image-matching locator.

6. **Report** what you found: how many elements, which ones became real screen-object methods,
   and which (if any) had no usable label.

## Hard rules

- Every locator in the generated Screen Object must trace back to something the crawl actually
  found in `locators/<ScreenName>.json` — never a value typed from assumption.
- Don't overwrite an existing Screen Object's hand-written business logic; add new methods for
  newly-discovered elements and flag anything that looks like it changed.
