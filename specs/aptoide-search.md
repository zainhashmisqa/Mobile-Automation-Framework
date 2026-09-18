# Aptoide Search

key: APT-SEARCH
screen: SearchScreen
priority: HIGH

## Description
Aptoide's real search screen (cm.aptoide.pt), reached via Home's real Search entry point --
crawled live via a chained navigation (skip onboarding, then tap Search). Typing into the
real search field is a safe, read-only, non-destructive action -- no network call, download,
or purchase is exercised, only that the real input field accepts and reflects real text.

## Acceptance Criteria
- The real search field is reachable from home via the real Search entry point
- Typing into it is accepted and reflected back

## Scenarios

### [POSITIVE] Search field accepts and reflects real input
tags: @smoke
- launch app => Aptoide installs (or is already installed) and reaches the foreground
- wait for app ready => the page source has stabilized
- tap `id:cm.aptoide.pt:id/skip_button` (SKIP) => the onboarding wizard is dismissed
- tap `id:cm.aptoide.pt:id/action_search` (Search) => the real search screen opens
- type `aptoide` into `id:cm.aptoide.pt:id/search_src_text` => the real search field accepts input
- expect `id:cm.aptoide.pt:id/search_src_text` contains `aptoide` => the typed text is reflected back
