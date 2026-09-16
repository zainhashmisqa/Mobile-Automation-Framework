# Aptoide Onboarding

key: APT-ONBOARD
screen: OnboardingScreen
priority: CRITICAL

## Description
The real, first-run onboarding wizard for cm.aptoide.pt (v9.22.5.3), crawled live from the
actual app (see src/test/resources/locators/OnboardingScreen.json) -- no server-side login,
no seeded data, nothing beyond what a fresh install genuinely shows.

## Acceptance Criteria
- The app installs, launches, and the real onboarding wizard renders
- Tapping the real SKIP button dismisses the wizard

## Scenarios

### [POSITIVE] Skip button dismisses the real onboarding wizard
tags: @smoke
- launch app => Aptoide installs (or is already installed) and reaches the foreground
- wait for app ready => the page source has stabilized
- expect `id:cm.aptoide.pt:id/title` visible => the real onboarding title renders
- tap `id:cm.aptoide.pt:id/skip_button` (SKIP) => the real SKIP button responds
- expect `id:cm.aptoide.pt:id/title` hidden => the onboarding wizard is dismissed
