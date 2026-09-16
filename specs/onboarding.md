# Onboarding

key: MOB-ONBOARD
priority: HIGH

## Description
com.titanstudio.pro is installed and, on first real launch, should reach a real rendered
screen with at least one Semantics-labelled, interactive element -- proof the Flutter engine
attached and the app didn't just hang on its native splash frame.

No screen's real accessibility labels are committed yet (see AppLaunchTest's page-source
dump for how to discover them), so this spec intentionally uses only steps that need no
app-specific locator. Once a real screen has been crawled with
`mvn exec:java -Dexec.args="crawl --screen Onboarding"`, its actual controls (e.g. a "Skip"
or "Continue" affordance) belong in a `screen:`-scoped spec of their own.

## Acceptance Criteria
- The app is installed and launches
- The app reaches the foreground within the configured launch timeout
- A real, non-empty page source is available once launched (not just the native splash frame)

## Scenarios

### [POSITIVE] The app launches and reaches a real, rendered screen
priority: CRITICAL
tags: @smoke
preconditions: app is installed
- launch app => the app is in the foreground
- wait for app ready => the page source has stopped changing between polls
- expect the first real screen visible => no locator crawled yet -- becomes an explicit TODO, not a guess
