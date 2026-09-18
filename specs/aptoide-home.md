# Aptoide Home

key: APT-HOME
screen: HomeScreen
priority: HIGH

## Description
Aptoide's real home/catalog screen (cm.aptoide.pt), crawled live after skipping onboarding
(see src/test/resources/locators/HomeScreen.json) -- real bottom navigation
(Editorial/Search/Stores/Apps), category filter chips, and app tiles. No downloads, installs,
or login involved -- just reaching the screen and using its real, safe navigation.

## Acceptance Criteria
- After skipping onboarding, the real home screen's bottom navigation renders
- The real Search entry point responds to a tap

## Scenarios

### [POSITIVE] Search entry point is reachable from home
tags: @smoke
- launch app => Aptoide installs (or is already installed) and reaches the foreground
- wait for app ready => the page source has stabilized
- tap `id:cm.aptoide.pt:id/skip_button` (SKIP) => the onboarding wizard is dismissed
- expect `id:cm.aptoide.pt:id/action_search` visible => the real home bottom navigation renders
- tap `id:cm.aptoide.pt:id/action_search` (Search) => the real Search entry point responds
